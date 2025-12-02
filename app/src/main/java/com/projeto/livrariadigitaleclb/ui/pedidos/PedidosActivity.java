package com.projeto.livrariadigitaleclb.ui.pedidos;

import android.content.Intent;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import android.app.AlertDialog;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.projeto.livrariadigitaleclb.R;
import com.projeto.livrariadigitaleclb.data.local.AppDatabase;
import com.projeto.livrariadigitaleclb.data.local.dao.PedidoDao;
import com.projeto.livrariadigitaleclb.data.local.entity.PedidoEntity;
import com.projeto.livrariadigitaleclb.databinding.ActivityPedidosBinding;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class PedidosActivity extends AppCompatActivity {

    private ActivityPedidosBinding binding;

    private PedidoDao pedidoDao;
    private List<PedidoEntity> listaPedidos = new ArrayList<>();
    private ArrayList<String> listaStrings = new ArrayList<>();

    private PedidosAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPedidosBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        pedidoDao = AppDatabase.getInstance(this).pedidoDao();

        configurarLista();
        carregarPedidosDoBanco();

        binding.btnAdicionarLivro.setOnClickListener(v -> abrirDialogAdicionarLivro());

        binding.btnImprimirLista.setOnClickListener(v -> {
            if (listaStrings.isEmpty()) {
                Toast.makeText(this, "A lista está vazia!", Toast.LENGTH_SHORT).show();
                return;
            }
            gerarPDF();
        });

        binding.iconHome.setOnClickListener(v -> finish());
    }

    private void configurarLista() {
        adapter = new PedidosAdapter(this, listaPedidos, this);
        binding.listaPedidos.setAdapter(adapter);
    }

    private void carregarPedidosDoBanco() {
        listaPedidos = pedidoDao.getPedidos();

        listaStrings.clear();
        for (PedidoEntity pedido : listaPedidos) {
            listaStrings.add(pedido.titulo + " – " + pedido.autor + " (Qtd: " + pedido.quantidadeDesejada + ")");
        }

        adapter.atualizarLista(listaPedidos);
    }

    public void confirmarExclusao(PedidoEntity pedido) {
        new AlertDialog.Builder(this)
                .setTitle("Excluir Pedido")
                .setMessage("Deseja excluir este item da lista?")
                .setPositiveButton("Sim", (d, w) -> {
                    pedidoDao.excluirPedido(pedido);
                    carregarPedidosDoBanco();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void abrirDialogAdicionarLivro() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.dialog_add_livro, null);

        EditText inputTitulo = view.findViewById(R.id.inputTitulo);
        EditText inputAutor = view.findViewById(R.id.inputAutor);
        EditText inputQuantidade = view.findViewById(R.id.inputQuantidade);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(view);
        builder.setTitle("Adicionar Pedido Manual");

        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss());

        builder.setPositiveButton("Adicionar", (dialog, which) -> {
            String titulo = inputTitulo.getText().toString().trim();
            String autor = inputAutor.getText().toString().trim();
            String qtdStr = inputQuantidade.getText().toString().trim();

            if (titulo.isEmpty() || autor.isEmpty()) {
                Toast.makeText(this, "Preencha título e autor", Toast.LENGTH_SHORT).show();
                return;
            }

            int quantidade = 1; // Valor padrão
            if (!qtdStr.isEmpty()) {
                try {
                    quantidade = Integer.parseInt(qtdStr);
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Quantidade inválida, usando 1", Toast.LENGTH_SHORT).show();
                }
            }

            salvarPedido(titulo, autor, quantidade);
        });

        builder.create().show();
    }

    void abrirDialogEditarLivro(PedidoEntity pedido) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.dialog_add_livro, null);

        EditText inputTitulo = view.findViewById(R.id.inputTitulo);
        EditText inputAutor = view.findViewById(R.id.inputAutor);
        EditText inputQuantidade = view.findViewById(R.id.inputQuantidade);

        inputTitulo.setText(pedido.titulo);
        inputAutor.setText(pedido.autor);
        inputQuantidade.setText(String.valueOf(pedido.quantidadeDesejada));

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(view);
        builder.setTitle("Editar Pedido");

        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss());

        builder.setPositiveButton("Salvar", (dialog, which) -> {
            String novoTitulo = inputTitulo.getText().toString().trim();
            String novoAutor = inputAutor.getText().toString().trim();
            String qtdStr = inputQuantidade.getText().toString().trim();

            if (novoTitulo.isEmpty() || novoAutor.isEmpty()) {
                Toast.makeText(this, "Preencha os campos", Toast.LENGTH_SHORT).show();
                return;
            }

            int novaQtd = 1;
            try {
                novaQtd = Integer.parseInt(qtdStr);
            } catch (Exception e) {}

            pedido.titulo = novoTitulo;
            pedido.autor = novoAutor;
            pedido.quantidadeDesejada = novaQtd;
            pedidoDao.atualizarPedido(pedido);
            carregarPedidosDoBanco();
        });

        builder.create().show();
    }

    // CORREÇÃO DO ERRO DE COMPILAÇÃO AQUI: Adicionado parâmetro int quantidade
    private void salvarPedido(String titulo, String autor, int quantidade) {
        PedidoEntity pedido = new PedidoEntity(titulo, autor, quantidade);
        pedidoDao.inserirPedido(pedido);
        carregarPedidosDoBanco();
    }

    private void gerarPDF() {
        try {
            PdfDocument pdf = new PdfDocument();
            PdfDocument.PageInfo pageInfo =
                    new PdfDocument.PageInfo.Builder(595, 842, 1).create();
            PdfDocument.Page page = pdf.startPage(pageInfo);

            Paint paint = new Paint();
            int x = 40;
            int y = 40;

            // Cabeçalho
            Paint headerPaint = new Paint();
            headerPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            headerPaint.setTextSize(24);
            headerPaint.setTextAlign(Paint.Align.CENTER);

            int pageWidth = pageInfo.getPageWidth();
            int headerX = pageWidth / 2;
            int headerY = 50;

            page.getCanvas().drawText("Lista de Reposição/Pedidos", headerX, headerY, headerPaint);

            // Linha separadora
            page.getCanvas().drawLine(40, 70, pageWidth - 40, 70, paint);

            y = 100;

            // Conteúdo
            paint.setTextSize(14);
            paint.setTextAlign(Paint.Align.LEFT);

            for (String item : listaStrings) {
                page.getCanvas().drawText(item, x, y, paint);
                y += 30;
                if (y > 800) break;
            }

            pdf.finishPage(page);

            File pasta = new File(getExternalFilesDir(null), "pdfs");
            if (!pasta.exists()) pasta.mkdirs();

            String dataAtual = new SimpleDateFormat("dd-MM-yyyy", new Locale("pt", "BR")).format(new Date());
            String nomeArquivo = "Lista_Pedidos_" + dataAtual + ".pdf";

            File arquivo = new File(pasta, nomeArquivo);
            FileOutputStream fos = new FileOutputStream(arquivo);

            pdf.writeTo(fos);
            pdf.close();
            fos.close();

            Toast.makeText(this, "PDF salvo: " + arquivo.getName(), Toast.LENGTH_LONG).show();
            abrirPDF(arquivo);

        } catch (Exception e) {
            Toast.makeText(this, "Erro ao gerar PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void abrirPDF(File arquivo) {
        try {
            Uri uri = FileProvider.getUriForFile(
                    this,
                    "com.projeto.livrariadigitaleclb.fileprovider",
                    arquivo
            );

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/pdf");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Nenhum leitor de PDF instalado.", Toast.LENGTH_LONG).show();
        }
    }
}
