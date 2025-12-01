package com.projeto.livrariadigitaleclb.ui.relatorios;

import android.content.Intent;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.projeto.livrariadigitaleclb.data.local.AppDatabase;
import com.projeto.livrariadigitaleclb.data.local.dao.VendaDao;
import com.projeto.livrariadigitaleclb.data.local.entity.Venda;
import com.projeto.livrariadigitaleclb.databinding.ActivityRelatoriosBinding;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RelatoriosActivity extends AppCompatActivity {

    private ActivityRelatoriosBinding binding;
    private VendaDao vendaDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRelatoriosBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        vendaDao = AppDatabase.getInstance(this).vendaDao();

        binding.btnHome.setOnClickListener(v -> finish());
        binding.btnImprimir.setOnClickListener(v -> gerarRelatorio());
    }

    private void gerarRelatorio() {
        int checkedId = binding.radioGroupRelatorios.getCheckedRadioButtonId();
        String tipo;

        if (checkedId == binding.radioDiario.getId()) {
            tipo = "DIARIO";
        } else if (checkedId == binding.radioSemanal.getId()) {
            tipo = "SEMANAL";
        } else if (checkedId == binding.radioMensal.getId()) {
            tipo = "MENSAL";
        } else {
            Toast.makeText(this, "Selecione um tipo de relatório", Toast.LENGTH_SHORT).show();
            return;
        }

        List<Venda> todas = vendaDao.listarVendas();
        List<Venda> filtradas = filtrarPorPeriodo(todas, tipo);

        if (filtradas.isEmpty()) {
            Toast.makeText(this, "Nenhuma venda encontrada para este período", Toast.LENGTH_SHORT).show();
            return;
        }

        gerarPDF(tipo, filtradas);
    }

    private List<Venda> filtrarPorPeriodo(List<Venda> vendas, String tipo) {
        long agora = System.currentTimeMillis();
        long limite;

        Calendar c = Calendar.getInstance();

        switch (tipo) {
            case "DIARIO":
                limite = getInicioDoDia(agora);
                break;
            case "SEMANAL":
                limite = getInicioDoDia(c.getTimeInMillis());
                break;
            case "MENSAL":
            default:
                c.add(Calendar.DAY_OF_YEAR, -30);
                limite = getInicioDoDia(c.getTimeInMillis());
                break;
        }

        List<Venda> resultado = new ArrayList<>();
        for (Venda v : vendas) {
            if (v.dataVenda >= limite) {
                resultado.add(v);
            }
        }
        return resultado;
    }

    private void gerarPDF(String tipo, List<Venda> vendas) {
        try {
            PdfDocument pdf = new PdfDocument();
            PdfDocument.PageInfo pageInfo =
                    new PdfDocument.PageInfo.Builder(595, 842, 1).create();
            PdfDocument.Page page = pdf.startPage(pageInfo);

            Paint paint = new Paint();
            int x = 40;
            int y = 40;

            // ==== Cabeçalho ====
            Paint headerPaint = new Paint();
            headerPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            headerPaint.setTextSize(24);
            headerPaint.setTextAlign(Paint.Align.CENTER);

            int pageWidth = pageInfo.getPageWidth();
            int headerX = pageWidth / 2;
            int headerY = 50;

            String tituloCabecalho;
            switch (tipo) {
                case "DIARIO":
                    tituloCabecalho = "Relatório Diário de Vendas";
                    break;
                case "SEMANAL":
                    tituloCabecalho = "Relatório Semanal de Vendas";
                    break;
                case "MENSAL":
                default:
                    tituloCabecalho = "Relatório Mensal de Vendas";
                    break;
            }

            page.getCanvas().drawText(tituloCabecalho, headerX, headerY, headerPaint);
            page.getCanvas().drawLine(40, 75, pageWidth - 40, 75, paint);

            y = 110;
            paint.setTextSize(14);

            SimpleDateFormat sdfDataHora =
                    new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

            int numeroPagina = 1;

            for (Venda v : vendas) {
                String linha =
                        "Título: " + v.titulo +
                                " | Preço: R$ " + String.format(Locale.getDefault(), "%.2f", v.preco) +
                                " | Data: " + sdfDataHora.format(new Date(v.dataVenda));

                page.getCanvas().drawText(linha, x, y, paint);
                y += 30;

                if (y > 780) {
                    pdf.finishPage(page);
                    numeroPagina++;

                    pageInfo = new PdfDocument.PageInfo.Builder(595, 842, numeroPagina).create();
                    page = pdf.startPage(pageInfo);
                    y = 110;

                    page.getCanvas().drawText(tituloCabecalho, headerX, headerY, headerPaint);
                    page.getCanvas().drawLine(40, 75, pageWidth - 40, 75, paint);
                }
            }

            pdf.finishPage(page);

            File pasta = new File(getExternalFilesDir(null), "pdfs");
            if (!pasta.exists()) pasta.mkdirs();

            String data = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());
            String nomeArquivo = tituloCabecalho + " - " + data + ".pdf";

            File arquivo = new File(pasta, nomeArquivo);
            FileOutputStream fos = new FileOutputStream(arquivo);

            pdf.writeTo(fos);
            pdf.close();
            fos.close();

            Toast.makeText(this,
                    "PDF gerado em:\n" + arquivo.getAbsolutePath(),
                    Toast.LENGTH_LONG).show();

            abrirPDF(arquivo);

        } catch (Exception e) {
            Toast.makeText(this,
                    "Erro ao gerar PDF: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    private long getInicioDoDia(long millis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(millis);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }


    private void abrirPDF(File arquivo) {
        try {
            Uri uri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".provider",
                    arquivo
            );

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/pdf");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);

        } catch (Exception e) {
            Toast.makeText(this,
                    "Nenhum leitor de PDF instalado.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }
}
