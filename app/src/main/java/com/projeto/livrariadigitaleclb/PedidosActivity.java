package com.projeto.livrariadigitaleclb;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.app.AlertDialog;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.projeto.livrariadigitaleclb.databinding.ActivityPedidosBinding;
import com.projeto.livrariadigitaleclb.room.AppDatabase;
import com.projeto.livrariadigitaleclb.room.PedidoDao;
import com.projeto.livrariadigitaleclb.room.PedidoEntity;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

public class PedidosActivity extends AppCompatActivity {

    private ActivityPedidosBinding binding;
    private ArrayAdapter<String> adapter;
    private ArrayList<String> listaStrings;

    private PedidoDao pedidoDao;

    private static final int PERMISSAO_PDF = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPedidosBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        pedidoDao = AppDatabase.getDatabase(this).pedidoDao();

        listaStrings = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, R.layout.item_lista_pedido, listaStrings);
        binding.listaPedidos.setAdapter(adapter);

        carregarPedidosDoBanco();

        binding.btnAdicionarLivro.setOnClickListener(v -> abrirDialogAdicionarLivro());

        binding.btnImprimirLista.setOnClickListener(v -> {
            if (listaStrings.isEmpty()) {
                Toast.makeText(this, "A lista está vazia!", Toast.LENGTH_SHORT).show();
                return;
            }

            gerarPDF();  // CHAMA DIRETO
        });


        binding.iconHome.setOnClickListener(v -> finish());
    }

    private void carregarPedidosDoBanco() {
        listaStrings.clear();
        List<PedidoEntity> pedidos = pedidoDao.getPedidos();
        for (PedidoEntity p : pedidos) {
            listaStrings.add(p.titulo + " – " + p.autor);
        }
        adapter.notifyDataSetChanged();
    }


    private void abrirDialogAdicionarLivro() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.dialog_add_livro, null);


        EditText inputTitulo = view.findViewById(R.id.inputTitulo);
        EditText inputAutor = view.findViewById(R.id.inputAutor);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(view);
        builder.setTitle("Adicionar Livro à Lista");

        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss());

        builder.setPositiveButton("Adicionar", (dialog, which) -> {
            String titulo = inputTitulo.getText().toString().trim();
            String autor = inputAutor.getText().toString().trim();

            if (titulo.isEmpty() || autor.isEmpty()) {
                Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show();
                return;
            }

            salvarPedido(titulo, autor);
        });

        builder.create().show();
    }

    private void salvarPedido(String titulo, String autor) {
        PedidoEntity pedido = new PedidoEntity(titulo, autor);
        pedidoDao.inserirPedido(pedido);
        carregarPedidosDoBanco(); // atualiza a lista na tela
    }


    private void gerarPDF() {
        try {
            PdfDocument pdf = new PdfDocument();
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
            PdfDocument.Page page = pdf.startPage(pageInfo);

            Paint paint = new Paint();
            paint.setTextSize(14);

            int x = 40;
            int y = 60;

            for (String item : listaStrings) {
                page.getCanvas().drawText(item, x, y, paint);
                y += 30; // espaçamento entre linhas
            }

            pdf.finishPage(page);

            File pasta = new File(getExternalFilesDir(null), "pdfs");
            if (!pasta.exists()) pasta.mkdirs();

            File arquivo = new File(pasta, "lista_pedidos.pdf");
            FileOutputStream fos = new FileOutputStream(arquivo);

            pdf.writeTo(fos);
            pdf.close();
            fos.close();

            Toast.makeText(this,
                    "PDF gerado em:\n" + arquivo.getAbsolutePath(),
                    Toast.LENGTH_LONG).show();

            abrirPDF(arquivo); // abre automaticamente!

        } catch (Exception e) {
            Toast.makeText(this,
                    "Erro ao gerar PDF: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
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
            Toast.makeText(this,
                    "Nenhum leitor de PDF instalado.",
                    Toast.LENGTH_LONG).show();
        }
    }

}

