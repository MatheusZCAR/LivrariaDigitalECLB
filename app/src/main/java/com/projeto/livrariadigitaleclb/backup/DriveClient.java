package com.projeto.livrariadigitaleclb.backup;

import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Cliente mínimo da API REST do Google Drive (v3), usando apenas o escopo "drive.file":
 * o app só enxerga a pasta e os arquivos que ele mesmo criou.
 */
public class DriveClient {

    public static final String NOME_PASTA = "Livraria ECLB - Cópias de segurança";
    private static final String API = "https://www.googleapis.com/drive/v3";
    private static final String UPLOAD = "https://www.googleapis.com/upload/drive/v3/files";
    private static final String TIPO_PASTA = "application/vnd.google-apps.folder";
    private static final int TEMPO_LIMITE_MS = 60_000;

    /** Erro da API com mensagem já traduzida para a pessoa. */
    public static class ErroDrive extends IOException {
        public final int codigoHttp;
        public final boolean precisaReconectar;

        ErroDrive(int codigoHttp, String mensagem, boolean precisaReconectar) {
            super(mensagem);
            this.codigoHttp = codigoHttp;
            this.precisaReconectar = precisaReconectar;
        }
    }

    public static final class ArquivoDrive {
        public String id;
        public String nome;
        public long criadoEm;
        public long tamanho;
    }

    private final String token;

    public DriveClient(String token) {
        this.token = token;
    }

    /** E-mail da conta Google conectada. */
    public String emailDaConta() throws IOException {
        JSONObject r = json("GET", API + "/about?fields=user(emailAddress,displayName)", null);
        JSONObject user = r.optJSONObject("user");
        return user == null ? null : user.optString("emailAddress", null);
    }

    /** Procura a pasta de cópias criada pelo app; se não existir, cria. */
    public String obterOuCriarPasta() throws IOException {
        String q = "mimeType='" + TIPO_PASTA + "' and name='" + NOME_PASTA.replace("'", "\\'") + "' and trashed=false";
        String url = API + "/files?spaces=drive&fields=files(id,name)&q=" + Uri.encode(q);
        JSONArray arquivos = json("GET", url, null).optJSONArray("files");
        if (arquivos != null && arquivos.length() > 0) {
            return arquivos.optJSONObject(0).optString("id");
        }
        JSONObject meta = new JSONObject();
        try {
            meta.put("name", NOME_PASTA);
            meta.put("mimeType", TIPO_PASTA);
        } catch (JSONException e) {
            throw new IOException(e);
        }
        return json("POST", API + "/files?fields=id", meta.toString()).optString("id");
    }

    /** Envia um arquivo (upload "resumable", funciona bem com arquivos grandes e internet lenta). */
    public String enviar(File arquivo, String nome, String pastaId) throws IOException {
        JSONObject meta = new JSONObject();
        try {
            meta.put("name", nome);
            meta.put("mimeType", "application/zip");
            JSONArray pais = new JSONArray();
            pais.put(pastaId);
            meta.put("parents", pais);
        } catch (JSONException e) {
            throw new IOException(e);
        }

        // 1) Inicia a sessão de envio
        HttpURLConnection inicio = abrir("POST", UPLOAD + "?uploadType=resumable&fields=id");
        inicio.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        inicio.setRequestProperty("X-Upload-Content-Type", "application/zip");
        inicio.setRequestProperty("X-Upload-Content-Length", String.valueOf(arquivo.length()));
        escrever(inicio, meta.toString().getBytes(StandardCharsets.UTF_8));
        verificar(inicio);
        String sessao = inicio.getHeaderField("Location");
        inicio.disconnect();
        if (sessao == null) throw new IOException("O Google Drive não aceitou o envio.");

        // 2) Envia o conteúdo
        HttpURLConnection envio = (HttpURLConnection) new URL(sessao).openConnection();
        envio.setRequestMethod("PUT");
        envio.setConnectTimeout(TEMPO_LIMITE_MS);
        envio.setReadTimeout(TEMPO_LIMITE_MS * 3);
        envio.setDoOutput(true);
        envio.setRequestProperty("Content-Type", "application/zip");
        envio.setFixedLengthStreamingMode(arquivo.length());
        try (InputStream in = new BufferedInputStream(new FileInputStream(arquivo));
             OutputStream out = envio.getOutputStream()) {
            BackupArquivo.copiarFluxo(in, out);
        }
        verificar(envio);
        String id;
        try {
            id = new JSONObject(lerTudo(envio.getInputStream())).optString("id");
        } catch (JSONException e) {
            id = null;
        } finally {
            envio.disconnect();
        }
        return id;
    }

    /** Lista as cópias, da mais nova para a mais antiga. */
    public List<ArquivoDrive> listar(String pastaId) throws IOException {
        String q = "'" + pastaId + "' in parents and trashed=false";
        String url = API + "/files?spaces=drive&orderBy=createdTime%20desc&pageSize=100"
                + "&fields=files(id,name,createdTime,size)&q=" + Uri.encode(q);
        JSONArray arquivos = json("GET", url, null).optJSONArray("files");
        List<ArquivoDrive> lista = new ArrayList<>();
        if (arquivos == null) return lista;
        for (int i = 0; i < arquivos.length(); i++) {
            JSONObject o = arquivos.optJSONObject(i);
            if (o == null) continue;
            ArquivoDrive a = new ArquivoDrive();
            a.id = o.optString("id");
            a.nome = o.optString("name");
            a.criadoEm = lerData(o.optString("createdTime"));
            a.tamanho = o.optLong("size", 0);
            lista.add(a);
        }
        return lista;
    }

    public void baixar(String id, File destino) throws IOException {
        HttpURLConnection c = abrir("GET", API + "/files/" + Uri.encode(id) + "?alt=media");
        verificar(c);
        try (InputStream in = new BufferedInputStream(c.getInputStream());
             OutputStream out = new FileOutputStream(destino)) {
            BackupArquivo.copiarFluxo(in, out);
        } finally {
            c.disconnect();
        }
    }

    public void apagar(String id) throws IOException {
        HttpURLConnection c = abrir("DELETE", API + "/files/" + Uri.encode(id));
        verificar(c);
        c.disconnect();
    }

    /** Mantém só as {@code manter} cópias mais novas. */
    public void apagarAntigas(String pastaId, int manter) throws IOException {
        List<ArquivoDrive> lista = listar(pastaId);
        for (int i = manter; i < lista.size(); i++) {
            apagar(lista.get(i).id);
        }
    }

    // ------------------------------------------------------------------ HTTP

    private HttpURLConnection abrir(String metodo, String url) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setRequestMethod(metodo);
        c.setConnectTimeout(TEMPO_LIMITE_MS);
        c.setReadTimeout(TEMPO_LIMITE_MS);
        c.setRequestProperty("Authorization", "Bearer " + token);
        c.setRequestProperty("Accept", "application/json");
        return c;
    }

    private JSONObject json(String metodo, String url, String corpo) throws IOException {
        HttpURLConnection c = abrir(metodo, url);
        try {
            if (corpo != null) {
                c.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                escrever(c, corpo.getBytes(StandardCharsets.UTF_8));
            }
            verificar(c);
            String resposta = lerTudo(c.getInputStream());
            return resposta.isEmpty() ? new JSONObject() : new JSONObject(resposta);
        } catch (JSONException e) {
            throw new IOException("Resposta inesperada do Google Drive.", e);
        } finally {
            c.disconnect();
        }
    }

    private static void escrever(HttpURLConnection c, byte[] dados) throws IOException {
        c.setDoOutput(true);
        c.setFixedLengthStreamingMode(dados.length);
        try (OutputStream out = c.getOutputStream()) {
            out.write(dados);
        }
    }

    private static void verificar(HttpURLConnection c) throws IOException {
        int codigo = c.getResponseCode();
        if (codigo >= 200 && codigo < 300) return;

        String corpo = "";
        InputStream erro = c.getErrorStream();
        if (erro != null) {
            try {
                corpo = lerTudo(erro);
            } catch (IOException ignorada) {
                // sem detalhes
            }
        }
        throw traduzirErro(codigo, corpo);
    }

    static ErroDrive traduzirErro(int codigo, String corpo) {
        String motivo = "";
        try {
            JSONObject e = new JSONObject(corpo).optJSONObject("error");
            if (e != null) {
                JSONArray erros = e.optJSONArray("errors");
                if (erros != null && erros.length() > 0) {
                    motivo = erros.optJSONObject(0).optString("reason", "");
                }
                if (motivo.isEmpty()) motivo = e.optString("status", "");
            }
        } catch (JSONException ignorada) {
            // corpo não é JSON
        }

        if (codigo == 401) {
            return new ErroDrive(codigo, "A conexão com o Google Drive expirou. Toque em \"Conectar ao Google Drive\" de novo.", true);
        }
        if (motivo.contains("storageQuotaExceeded")) {
            return new ErroDrive(codigo, "O Google Drive desta conta está sem espaço.", false);
        }
        if (motivo.contains("accessNotConfigured") || motivo.contains("SERVICE_DISABLED")) {
            return new ErroDrive(codigo, "A API do Google Drive não está ativada no projeto do Google Cloud deste app (veja o README).", false);
        }
        if (codigo == 403 && (motivo.contains("insufficient") || motivo.contains("PERMISSION_DENIED"))) {
            return new ErroDrive(codigo, "O app não tem permissão para usar o Google Drive. Conecte de novo e marque a permissão pedida.", true);
        }
        if (codigo == 404) {
            return new ErroDrive(codigo, "O arquivo não foi encontrado no Google Drive.", false);
        }
        if (codigo == 429 || codigo >= 500) {
            return new ErroDrive(codigo, "O Google Drive está ocupado agora. Tente de novo em alguns minutos.", false);
        }
        return new ErroDrive(codigo, "O Google Drive recusou a operação (código " + codigo
                + (motivo.isEmpty() ? "" : ", " + motivo) + ").", false);
    }

    private static String lerTudo(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (InputStream i = in) {
            BackupArquivo.copiarFluxo(i, out);
        }
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }

    static long lerData(String rfc3339) {
        if (rfc3339 == null || rfc3339.isEmpty()) return 0;
        String[] formatos = {"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", "yyyy-MM-dd'T'HH:mm:ss'Z'"};
        for (String f : formatos) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(f, Locale.ROOT);
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                java.util.Date d = sdf.parse(rfc3339);
                if (d != null) return d.getTime();
            } catch (ParseException ignorada) {
                // tenta o próximo formato
            }
        }
        return 0;
    }
}
