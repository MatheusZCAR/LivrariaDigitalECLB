package com.projeto.livrariadigitaleclb.backup;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import androidx.sqlite.db.SimpleSQLiteQuery;

import com.projeto.livrariadigitaleclb.BuildConfig;
import com.projeto.livrariadigitaleclb.data.local.AppDatabase;
import com.projeto.livrariadigitaleclb.data.local.entity.LivroEntity;
import com.projeto.livrariadigitaleclb.util.Capas;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Gera e restaura a cópia de segurança: um arquivo .zip com o banco de dados,
 * as fotos das capas e um "manifesto" com informações da cópia.
 */
public final class BackupArquivo {

    private static final String TAG = "BackupArquivo";
    private static final String ID_APP = "livraria-eclb";
    private static final int FORMATO = 1;

    private static final String MANIFESTO = "manifesto.json";
    private static final String PASTA_BANCO = "banco/";
    private static final String PASTA_CAPAS = "capas/";

    /** Impede que um backup e uma restauração aconteçam ao mesmo tempo. */
    public static final Object TRAVA = new Object();

    private BackupArquivo() {}

    /** Informações lidas do manifesto de uma cópia. */
    public static final class Resumo {
        public long criadoEm;
        public int versaoBanco;
        public int livros;
        public int vendas;
        public String versaoApp;
    }

    public static String nomeSugerido(long quando) {
        return "livraria-eclb-" + new SimpleDateFormat("yyyy-MM-dd_HH'h'mm", Locale.ROOT).format(new Date(quando)) + ".zip";
    }

    /** Cria a cópia em um arquivo temporário (pasta cache/backups). */
    public static File criar(Context contexto) throws IOException {
        Context c = contexto.getApplicationContext();
        synchronized (TRAVA) {
            File pastaTemp = new File(c.getCacheDir(), "backups");
            //noinspection ResultOfMethodCallIgnored
            pastaTemp.mkdirs();
            apagarAntigos(pastaTemp, 60 * 60 * 1000L);

            long agora = System.currentTimeMillis();
            File destino = new File(pastaTemp, nomeSugerido(agora));
            File copiaBanco = new File(pastaTemp, "banco.tmp");
            File copiaWal = new File(pastaTemp, "banco-wal.tmp");

            AppDatabase db = AppDatabase.getInstance(c);
            int livros = db.livroDao().contar();
            int vendas = db.vendaDao().contar();

            // Passa o conteúdo do diário (WAL) para o arquivo principal, quando possível.
            try (Cursor cur = db.query(new SimpleSQLiteQuery("PRAGMA wal_checkpoint(FULL)"))) {
                cur.moveToFirst();
            } catch (Exception e) {
                Log.w(TAG, "Checkpoint não concluído", e);
            }

            // Copia os arquivos do banco dentro de uma transação de escrita:
            // assim ninguém altera o banco enquanto ele é copiado.
            File arquivoBanco = c.getDatabasePath(AppDatabase.NOME_ARQUIVO);
            File arquivoWal = new File(arquivoBanco.getPath() + "-wal");
            final IOException[] falha = new IOException[1];
            db.runInTransaction(() -> {
                try {
                    copiar(arquivoBanco, copiaBanco);
                    if (arquivoWal.exists() && arquivoWal.length() > 0) {
                        copiar(arquivoWal, copiaWal);
                    }
                } catch (IOException e) {
                    falha[0] = e;
                }
            });
            if (falha[0] != null) throw falha[0];

            JSONObject manifesto = new JSONObject();
            try {
                manifesto.put("app", ID_APP);
                manifesto.put("formato", FORMATO);
                manifesto.put("versaoBanco", AppDatabase.VERSAO);
                manifesto.put("versaoApp", BuildConfig.VERSION_NAME);
                manifesto.put("criadoEm", agora);
                manifesto.put("livros", livros);
                manifesto.put("vendas", vendas);
            } catch (JSONException e) {
                throw new IOException(e);
            }

            try (ZipOutputStream zip = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(destino)))) {
                adicionarTexto(zip, MANIFESTO, manifesto.toString(2));
                adicionarArquivo(zip, PASTA_BANCO + AppDatabase.NOME_ARQUIVO, copiaBanco);
                if (copiaWal.exists()) {
                    adicionarArquivo(zip, PASTA_BANCO + AppDatabase.NOME_ARQUIVO + "-wal", copiaWal);
                }
                File[] capas = Capas.pasta(c).listFiles();
                if (capas != null) {
                    for (File capa : capas) {
                        if (capa.isFile()) adicionarArquivo(zip, PASTA_CAPAS + capa.getName(), capa);
                    }
                }
            } catch (JSONException e) {
                throw new IOException(e);
            } finally {
                //noinspection ResultOfMethodCallIgnored
                copiaBanco.delete();
                //noinspection ResultOfMethodCallIgnored
                copiaWal.delete();
            }
            return destino;
        }
    }

    /** Lê o manifesto e confere se o arquivo é uma cópia válida. */
    public static Resumo ler(File zipArquivo) throws IOException, BackupInvalidoException {
        boolean temBanco = false;
        JSONObject manifesto = null;
        try (ZipInputStream zip = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipArquivo)))) {
            ZipEntry e;
            while ((e = zip.getNextEntry()) != null) {
                if (MANIFESTO.equals(e.getName())) {
                    try {
                        manifesto = new JSONObject(lerTexto(zip));
                    } catch (JSONException ex) {
                        throw new BackupInvalidoException("O arquivo de cópia está danificado.");
                    }
                } else if ((PASTA_BANCO + AppDatabase.NOME_ARQUIVO).equals(e.getName())) {
                    temBanco = true;
                }
            }
        } catch (java.util.zip.ZipException e) {
            throw new BackupInvalidoException("Este arquivo não é uma cópia de segurança da livraria.");
        }
        if (manifesto == null || !temBanco || !ID_APP.equals(manifesto.optString("app"))) {
            throw new BackupInvalidoException("Este arquivo não é uma cópia de segurança da livraria.");
        }
        Resumo r = new Resumo();
        r.criadoEm = manifesto.optLong("criadoEm");
        r.versaoBanco = manifesto.optInt("versaoBanco");
        r.livros = manifesto.optInt("livros");
        r.vendas = manifesto.optInt("vendas");
        r.versaoApp = manifesto.optString("versaoApp");
        if (r.versaoBanco > AppDatabase.VERSAO) {
            throw new BackupInvalidoException("Esta cópia foi feita por uma versão mais nova do app ("
                    + r.versaoApp + "). Atualize o app neste tablet antes de restaurar.");
        }
        return r;
    }

    /**
     * Substitui TODOS os dados atuais pelos da cópia. Antes disso, guarda uma cópia
     * do estado atual em files/seguranca/antes_de_restaurar.zip.
     */
    public static Resumo restaurar(Context contexto, File zipArquivo) throws IOException, BackupInvalidoException {
        Context c = contexto.getApplicationContext();
        Resumo resumo = ler(zipArquivo);

        synchronized (TRAVA) {
            File pastaExtraida = new File(c.getCacheDir(), "restaurar");
            limparPasta(pastaExtraida);
            //noinspection ResultOfMethodCallIgnored
            pastaExtraida.mkdirs();
            extrair(zipArquivo, pastaExtraida);

            File bancoNovo = new File(pastaExtraida, PASTA_BANCO + AppDatabase.NOME_ARQUIVO);
            if (!bancoNovo.exists()) throw new BackupInvalidoException("A cópia não contém o banco de dados.");
        }

        // Cópia de segurança do que existe hoje, por precaução (fora da trava: criar() também a usa).
        try {
            File atual = criar(c);
            File pastaSeguranca = new File(c.getFilesDir(), "seguranca");
            //noinspection ResultOfMethodCallIgnored
            pastaSeguranca.mkdirs();
            copiar(atual, new File(pastaSeguranca, "antes_de_restaurar.zip"));
            //noinspection ResultOfMethodCallIgnored
            atual.delete();
        } catch (Exception e) {
            Log.w(TAG, "Não foi possível guardar a cópia do estado atual", e);
        }

        synchronized (TRAVA) {
            File pastaExtraida = new File(c.getCacheDir(), "restaurar");
            File bancoNovo = new File(pastaExtraida, PASTA_BANCO + AppDatabase.NOME_ARQUIVO);
            File walNovo = new File(pastaExtraida, PASTA_BANCO + AppDatabase.NOME_ARQUIVO + "-wal");

            AppDatabase.fechar();
            File banco = c.getDatabasePath(AppDatabase.NOME_ARQUIVO);
            for (String sufixo : new String[]{"", "-wal", "-shm", "-journal"}) {
                File f = new File(banco.getPath() + sufixo);
                if (f.exists() && !f.delete()) throw new IOException("Não foi possível substituir " + f.getName());
            }
            copiar(bancoNovo, banco);
            if (walNovo.exists()) copiar(walNovo, new File(banco.getPath() + "-wal"));

            // Capas
            File pastaCapas = Capas.pasta(c);
            limparPasta(pastaCapas);
            File capasNovas = new File(pastaExtraida, PASTA_CAPAS);
            File[] lista = capasNovas.listFiles();
            if (lista != null) {
                for (File f : lista) copiar(f, new File(pastaCapas, f.getName()));
            }

            // O caminho das fotos pode mudar de um aparelho para outro: corrige.
            AppDatabase db = AppDatabase.getInstance(c);
            List<LivroEntity> livros = db.livroDao().listarTodos();
            for (LivroEntity l : livros) {
                if (l.imagemPath == null || l.imagemPath.isEmpty()) continue;
                File nova = new File(pastaCapas, new File(l.imagemPath).getName());
                db.livroDao().atualizarImagem(l.id, nova.exists() ? nova.getAbsolutePath() : null);
            }

            limparPasta(pastaExtraida);
        }
        return resumo;
    }

    // ----------------------------------------------------------------- utilitários

    private static void extrair(File zipArquivo, File destino) throws IOException, BackupInvalidoException {
        String raiz = destino.getCanonicalPath() + File.separator;
        try (ZipInputStream zip = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipArquivo)))) {
            ZipEntry e;
            while ((e = zip.getNextEntry()) != null) {
                File saida = new File(destino, e.getName());
                // Proteção contra arquivos maliciosos que tentam escrever fora da pasta.
                if (!saida.getCanonicalPath().startsWith(raiz)) {
                    throw new BackupInvalidoException("Arquivo de cópia inválido.");
                }
                if (e.isDirectory()) {
                    //noinspection ResultOfMethodCallIgnored
                    saida.mkdirs();
                    continue;
                }
                File pai = saida.getParentFile();
                if (pai != null) {
                    //noinspection ResultOfMethodCallIgnored
                    pai.mkdirs();
                }
                try (OutputStream out = new BufferedOutputStream(new FileOutputStream(saida))) {
                    copiarFluxo(zip, out);
                }
            }
        }
    }

    private static void adicionarArquivo(ZipOutputStream zip, String nome, File arquivo) throws IOException {
        zip.putNextEntry(new ZipEntry(nome));
        try (InputStream in = new BufferedInputStream(new FileInputStream(arquivo))) {
            copiarFluxo(in, zip);
        }
        zip.closeEntry();
    }

    private static void adicionarTexto(ZipOutputStream zip, String nome, String texto) throws IOException {
        zip.putNextEntry(new ZipEntry(nome));
        zip.write(texto.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private static String lerTexto(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        copiarFluxo(in, out);
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }

    public static void copiar(File origem, File destino) throws IOException {
        File pai = destino.getParentFile();
        if (pai != null) {
            //noinspection ResultOfMethodCallIgnored
            pai.mkdirs();
        }
        try (InputStream in = new FileInputStream(origem); OutputStream out = new FileOutputStream(destino)) {
            copiarFluxo(in, out);
        }
    }

    public static void copiarFluxo(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[32 * 1024];
        int n;
        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
    }

    /** Apaga arquivos com mais de {@code idadeMs} (os mais novos podem estar sendo enviados). */
    private static void apagarAntigos(File pasta, long idadeMs) {
        File[] arquivos = pasta.listFiles();
        if (arquivos == null) return;
        long limite = System.currentTimeMillis() - idadeMs;
        for (File f : arquivos) {
            if (f.isFile() && f.lastModified() < limite) {
                //noinspection ResultOfMethodCallIgnored
                f.delete();
            }
        }
    }

    private static void limparPasta(File pasta) {
        File[] arquivos = pasta.listFiles();
        if (arquivos == null) return;
        for (File f : arquivos) {
            if (f.isDirectory()) limparPasta(f);
            //noinspection ResultOfMethodCallIgnored
            f.delete();
        }
    }
}
