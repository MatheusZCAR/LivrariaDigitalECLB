package com.projeto.livrariadigitaleclb.ui.relatorios;

import android.content.Intent;
import android.graphics.Paint;
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

        binding.btnImprimir.setOnClickListener(v -> onImprimirClicked());
    }

    private void onImprimirClicked() {
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

        List<Venda> todasVendas = vendaDao.listarVendas();
        List<Venda> filtradas = filtrarPorPeriodo(todasVendas, tipo);

        if (filtradas.isEmpty()) {
            Toast.makeText(this, "Nenhuma venda encontrada para o período selecionado", Toast.LENGTH_SHORT).show();
            return;
        }

        gerarPDF(tipo, filtradas);
    }

    private List<Venda> filtrarPorPeriodo(List<Venda> vendas, String tipo) {
        long agora = System.currentTimeMillis();

        long limiteMillis;
        switch (tipo) {
            case "DIARIO":
                // últimas 24 horas
                limiteMillis = agora - 24L * 60L * 60L * 1000L;
                break;
            case "SEMANAL":
                // últimos 7 dias
                limiteMillis = agora - 7L * 24L * 60L * 60L * 1000L;
                break;
            case "MENSAL":
            default:
                // últimos 30 dias
                limiteMillis = agora - 30L * 24L * 60L * 60L * 1000L;
                break;
        }

        List<Venda> resultado = new ArrayList<>();
        for (Venda v : vendas) {
            if (v.dataVenda >= limiteMillis) {
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
            paint.setTextSize(14);

            int x = 40;
            int y = 60;

            String titulo;
            switch (tipo) {
                case "DIARIO":
                    titulo = "Relatório Diário de Vendas";
                    break;
                case "SEMANAL":
                    titulo = "Relatório Semanal de Vendas";
                    break;
                case "MENSAL":
                default:
                    titulo = "Relatório Mensal de Vendas";
                    break;
            }

            paint.setFakeBoldText(true);
            page.getCanvas().drawText(titulo, x, y, paint);
            paint.setFakeBoldText(false);
            y += 40;

            SimpleDateFormat sdfDataHora =
                    new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

            for (Venda v : vendas) {
                String linha = "Título: " + v.titulo +
                        " | Preço: R$ " + v.preco +
                        " | Data: " + sdfDataHora.format(new Date(v.dataVenda));

                page.getCanvas().drawText(linha, x, y, paint);
                y += 30;

                if (y > 800) {
                    // simples quebra quando estoura a página
                    pdf.finishPage(page);
                    pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 2).create();
                    page = pdf.startPage(pageInfo);
                    y = 60;
                }
            }

            pdf.finishPage(page);

            File pasta = new File(getExternalFilesDir(null), "pdfs");
            if (!pasta.exists()) pasta.mkdirs();

            String nomeArquivo;
            switch (tipo) {
                case "DIARIO":
                    nomeArquivo = "relatorio_diario_vendas.pdf";
                    break;
                case "SEMANAL":
                    nomeArquivo = "relatorio_semanal_vendas.pdf";
                    break;
                case "MENSAL":
                default:
                    nomeArquivo = "relatorio_mensal_vendas.pdf";
                    break;
            }

            File arquivo = new File(pasta, nomeArquivo);
            FileOutputStream fos = new FileOutputStream(arquivo);

            pdf.writeTo(fos);
            pdf.close();
            fos.close();

            Toast.makeText(
                    this,
                    "PDF gerado em:\n" + arquivo.getAbsolutePath(),
                    Toast.LENGTH_LONG
            ).show();

            abrirPDF(arquivo);

        } catch (Exception e) {
            Toast.makeText(
                    this,
                    "Erro ao gerar PDF: " + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        }
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
            Toast.makeText(
                    this,
                    "Nenhum leitor de PDF instalado.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }
}
