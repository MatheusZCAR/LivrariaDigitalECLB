package com.projeto.livrariadigitaleclb.util;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Monta PDFs em folha A4 com quebra automática de linha e de página.
 * (Antes, listas longas eram cortadas na primeira página e linhas compridas saíam da folha.)
 */
public class PdfEscritor {

    private static final int LARGURA = 595;   // A4 em pontos
    private static final int ALTURA = 842;
    private static final int MARGEM = 48;
    private static final int RODAPE = 36;

    private final PdfDocument documento = new PdfDocument();
    private final String rodapeTexto;
    private PdfDocument.Page pagina;
    private Canvas canvas;
    private int numeroPagina = 0;
    private float y;

    private final Paint normal = criarPaint(12, false, Color.BLACK);
    private final Paint negrito = criarPaint(12, true, Color.BLACK);
    private final Paint cinza = criarPaint(10, false, Color.DKGRAY);
    private final Paint linhaPaint = new Paint();

    public PdfEscritor(String rodapeTexto) {
        this.rodapeTexto = rodapeTexto;
        linhaPaint.setColor(Color.GRAY);
        linhaPaint.setStrokeWidth(1f);
        novaPagina();
    }

    private static Paint criarPaint(float tamanho, boolean bold, int cor) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setTextSize(tamanho);
        p.setColor(cor);
        p.setTypeface(Typeface.create(Typeface.SANS_SERIF, bold ? Typeface.BOLD : Typeface.NORMAL));
        return p;
    }

    private void novaPagina() {
        if (pagina != null) finalizarPagina();
        numeroPagina++;
        PdfDocument.PageInfo info = new PdfDocument.PageInfo.Builder(LARGURA, ALTURA, numeroPagina).create();
        pagina = documento.startPage(info);
        canvas = pagina.getCanvas();
        y = MARGEM;
    }

    private void finalizarPagina() {
        String texto = rodapeTexto + "   —   Página " + numeroPagina;
        canvas.drawText(texto, MARGEM, ALTURA - RODAPE / 2f, cinza);
        documento.finishPage(pagina);
        pagina = null;
    }

    private void garantirEspaco(float altura) {
        if (y + altura > ALTURA - MARGEM - RODAPE) novaPagina();
    }

    public void titulo(String texto) {
        paragrafo(texto, 20, true);
        espaco(4);
    }

    public void subtitulo(String texto) {
        espaco(8);
        paragrafo(texto, 14, true);
        espaco(2);
    }

    public void texto(String texto) {
        paragrafo(texto, 12, false);
    }

    public void textoNegrito(String texto) {
        paragrafo(texto, 12, true);
    }

    public void paragrafo(String texto, float tamanho, boolean bold) {
        Paint p = criarPaint(tamanho, bold, Color.BLACK);
        float alturaLinha = tamanho * 1.4f;
        for (String linha : quebrar(texto, p, LARGURA - 2 * MARGEM)) {
            garantirEspaco(alturaLinha);
            y += tamanho;
            canvas.drawText(linha, MARGEM, y, p);
            y += alturaLinha - tamanho;
        }
    }

    /** Texto à esquerda e valor alinhado à direita, na mesma linha (ex.: livro ... R$ 20,00). */
    public void linhaComValor(String esquerda, String direita, boolean bold) {
        Paint p = bold ? negrito : normal;
        float larguraValor = p.measureText(direita) + 12;
        List<String> linhas = quebrar(esquerda, p, LARGURA - 2 * MARGEM - larguraValor);
        float alturaLinha = 12 * 1.45f;
        for (int i = 0; i < linhas.size(); i++) {
            garantirEspaco(alturaLinha);
            y += 12;
            canvas.drawText(linhas.get(i), MARGEM, y, p);
            if (i == 0) {
                canvas.drawText(direita, LARGURA - MARGEM - p.measureText(direita), y, p);
            }
            y += alturaLinha - 12;
        }
    }

    public void separador() {
        garantirEspaco(12);
        y += 6;
        canvas.drawLine(MARGEM, y, LARGURA - MARGEM, y, linhaPaint);
        y += 6;
    }

    public void espaco(float pontos) {
        y += pontos;
    }

    public File salvar(File destino) throws IOException {
        finalizarPagina();
        File pasta = destino.getParentFile();
        if (pasta != null && !pasta.exists() && !pasta.mkdirs()) {
            throw new IOException("Não foi possível criar a pasta " + pasta);
        }
        try (FileOutputStream out = new FileOutputStream(destino)) {
            documento.writeTo(out);
        } finally {
            documento.close();
        }
        return destino;
    }

    static List<String> quebrar(String texto, Paint p, float larguraMax) {
        List<String> linhas = new ArrayList<>();
        if (texto == null) texto = "";
        for (String paragrafo : texto.split("\n", -1)) {
            String[] palavras = paragrafo.split(" ");
            StringBuilder atual = new StringBuilder();
            for (String palavra : palavras) {
                String tentativa = atual.length() == 0 ? palavra : atual + " " + palavra;
                if (p.measureText(tentativa) <= larguraMax) {
                    atual.setLength(0);
                    atual.append(tentativa);
                } else {
                    if (atual.length() > 0) linhas.add(atual.toString());
                    // palavra maior que a linha: corta em pedaços
                    String resto = palavra;
                    while (p.measureText(resto) > larguraMax && resto.length() > 1) {
                        int n = p.breakText(resto, true, larguraMax, null);
                        n = Math.max(1, n);
                        linhas.add(resto.substring(0, n));
                        resto = resto.substring(n);
                    }
                    atual.setLength(0);
                    atual.append(resto);
                }
            }
            linhas.add(atual.toString());
        }
        return linhas;
    }
}
