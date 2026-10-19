package com.projeto.livrariadigitaleclb.util;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.print.PrintManager;

import androidx.core.content.FileProvider;

import com.projeto.livrariadigitaleclb.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/** Imprimir, enviar ou abrir um PDF gerado pelo app. */
public final class Documentos {

    public static final String AUTORIDADE = "com.projeto.livrariadigitaleclb.fileprovider";

    private Documentos() {}

    /** Pasta temporária dos PDFs (exposta ao FileProvider). */
    public static File pastaPdfs(Context c) {
        File dir = new File(c.getCacheDir(), "pdfs");
        //noinspection ResultOfMethodCallIgnored
        dir.mkdirs();
        return dir;
    }

    /** Pergunta à pessoa se quer imprimir ou enviar o PDF. */
    public static void oferecer(Activity a, File pdf, String titulo) {
        Dialogos.opcoes(a, titulo, "O documento está pronto. O que você quer fazer?",
                new Dialogos.Opcao(a.getString(R.string.imprimir), R.drawable.ic_imprimir, () -> imprimir(a, pdf, titulo)),
                new Dialogos.Opcao("Enviar (WhatsApp, e-mail…)", R.drawable.ic_compartilhar, () -> compartilhar(a, pdf, titulo)),
                new Dialogos.Opcao("Ver na tela", R.drawable.ic_busca, () -> abrir(a, pdf)));
    }

    public static void imprimir(Activity a, File pdf, String nomeTrabalho) {
        PrintManager pm = (PrintManager) a.getSystemService(Context.PRINT_SERVICE);
        if (pm == null) {
            Avisos.erro(a, "Impressão indisponível", "Este aparelho não tem o serviço de impressão.");
            return;
        }
        PrintAttributes atributos = new PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                .build();
        pm.print(nomeTrabalho, new AdaptadorPdf(pdf, nomeTrabalho), atributos);
    }

    public static void compartilhar(Activity a, File arquivo, String assunto) {
        Uri uri = FileProvider.getUriForFile(a, AUTORIDADE, arquivo);
        Intent envio = new Intent(Intent.ACTION_SEND);
        envio.setType(arquivo.getName().endsWith(".pdf") ? "application/pdf" : "application/octet-stream");
        envio.putExtra(Intent.EXTRA_STREAM, uri);
        envio.putExtra(Intent.EXTRA_SUBJECT, assunto);
        envio.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            a.startActivity(Intent.createChooser(envio, "Enviar por…"));
        } catch (ActivityNotFoundException e) {
            Avisos.erro(a, "Não foi possível enviar", "Nenhum aplicativo para enviar arquivos foi encontrado.");
        }
    }

    public static void abrir(Activity a, File pdf) {
        Uri uri = FileProvider.getUriForFile(a, AUTORIDADE, pdf);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "application/pdf");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            a.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Avisos.erro(a, "Não foi possível abrir",
                    "Não há um leitor de PDF instalado. Use \"Imprimir\" ou \"Enviar\".");
        }
    }

    /** Entrega um PDF já pronto ao serviço de impressão do Android. */
    private static final class AdaptadorPdf extends PrintDocumentAdapter {
        private final File pdf;
        private final String nome;

        AdaptadorPdf(File pdf, String nome) {
            this.pdf = pdf;
            this.nome = nome;
        }

        @Override
        public void onLayout(PrintAttributes antigos, PrintAttributes novos, CancellationSignal cancelamento,
                             LayoutResultCallback callback, Bundle extras) {
            if (cancelamento.isCanceled()) {
                callback.onLayoutCancelled();
                return;
            }
            PrintDocumentInfo info = new PrintDocumentInfo.Builder(nome + ".pdf")
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
                    .build();
            callback.onLayoutFinished(info, !novos.equals(antigos));
        }

        @Override
        public void onWrite(PageRange[] paginas, ParcelFileDescriptor destino,
                            CancellationSignal cancelamento, WriteResultCallback callback) {
            try (InputStream in = new FileInputStream(pdf);
                 OutputStream out = new FileOutputStream(destino.getFileDescriptor())) {
                byte[] buf = new byte[16 * 1024];
                int n;
                while ((n = in.read(buf)) > 0) {
                    if (cancelamento.isCanceled()) {
                        callback.onWriteCancelled();
                        return;
                    }
                    out.write(buf, 0, n);
                }
                callback.onWriteFinished(new PageRange[]{PageRange.ALL_PAGES});
            } catch (Exception e) {
                callback.onWriteFailed(e.getMessage());
            }
        }
    }
}
