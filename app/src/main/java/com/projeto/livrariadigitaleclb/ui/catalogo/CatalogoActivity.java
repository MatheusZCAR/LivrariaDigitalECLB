package com.projeto.livrariadigitaleclb.ui.catalogo;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.GridLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.projeto.livrariadigitaleclb.R;
import com.projeto.livrariadigitaleclb.data.PedidosService;
import com.projeto.livrariadigitaleclb.data.local.AppDatabase;
import com.projeto.livrariadigitaleclb.data.local.entity.LivroComEstoque;
import com.projeto.livrariadigitaleclb.data.local.entity.LivroEntity;
import com.projeto.livrariadigitaleclb.databinding.ActivityCatalogoBinding;
import com.projeto.livrariadigitaleclb.ui.BaseActivity;
import com.projeto.livrariadigitaleclb.util.Avisos;
import com.projeto.livrariadigitaleclb.util.Capas;
import com.projeto.livrariadigitaleclb.util.Moeda;
import com.projeto.livrariadigitaleclb.util.SeletorQuantidade;
import com.projeto.livrariadigitaleclb.util.Telas;
import com.projeto.livrariadigitaleclb.util.Texto;

import java.util.ArrayList;
import java.util.List;

public class CatalogoActivity extends BaseActivity {

    private ActivityCatalogoBinding binding;
    private LivrosAdapter adapter;
    private List<LivroComEstoque> todos = new ArrayList<>();

    /** Abre o cadastro/edição e, na volta, mostra a mensagem de confirmação. */
    private final ActivityResultLauncher<Intent> cadastro =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), r -> {
                if (r.getResultCode() == RESULT_OK && r.getData() != null) {
                    String msg = r.getData().getStringExtra(CadastrarLivroActivity.EXTRA_MENSAGEM);
                    if (msg != null) Avisos.sucesso(this, msg);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCatalogoBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        definirTitulo(getString(R.string.catalogo_titulo));

        adapter = new LivrosAdapter(this::mostrarDetalhes, null);
        binding.recyclerLivros.setLayoutManager(
                new GridLayoutManager(this, Telas.colunas(this, R.dimen.largura_minima_livro)));
        binding.recyclerLivros.setAdapter(adapter);

        binding.btnCadastrarLivro.setOnClickListener(v ->
                cadastro.launch(new Intent(this, CadastrarLivroActivity.class)));

        binding.edtBusca.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { filtrar(); }
            @Override public void afterTextChanged(Editable s) { }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        carregar();
    }

    private void carregar() {
        todos = AppDatabase.getInstance(this).livroDao().listarLivrosComEstoque();
        filtrar();
    }

    private String busca() {
        return binding.edtBusca.getText() == null ? "" : binding.edtBusca.getText().toString();
    }

    private void filtrar() {
        String busca = busca();
        List<LivroComEstoque> filtrados = new ArrayList<>();
        for (LivroComEstoque l : todos) {
            if (Texto.combina(busca, l.livro.titulo, l.livro.autor, l.livro.codigoBarras)) {
                filtrados.add(l);
            }
        }
        adapter.setItens(filtrados);

        binding.txtContagem.setText(Texto.vazio(busca)
                ? Texto.plural(todos.size(), "livro cadastrado", "livros cadastrados")
                : Texto.plural(filtrados.size(), "livro encontrado", "livros encontrados"));

        if (filtrados.isEmpty()) {
            binding.txtVazio.setVisibility(View.VISIBLE);
            binding.txtVazio.setText(todos.isEmpty() ? R.string.catalogo_vazio : R.string.busca_vazia);
        } else {
            binding.txtVazio.setVisibility(View.GONE);
        }
    }

    private void mostrarDetalhes(LivroComEstoque item) {
        LivroEntity livro = item.livro;
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_detalhes_livro, null);

        ((TextView) v.findViewById(R.id.txtTituloDetalhe)).setText(livro.titulo);
        TextView autor = v.findViewById(R.id.txtAutorDetalhe);
        if (Texto.vazio(livro.autor)) autor.setVisibility(View.GONE); else autor.setText(livro.autor);
        ((TextView) v.findViewById(R.id.txtPrecoDetalhe)).setText(Moeda.formatar(livro.preco));

        StringBuilder info = new StringBuilder();
        info.append("Estoque: ").append(item.quantidade <= 0
                ? "esgotado"
                : Texto.plural(item.quantidade, "exemplar", "exemplares"));
        info.append("\nOnde fica: ").append(Texto.vazio(livro.localizacao) ? "não informado" : livro.localizacao);
        info.append("\nCódigo de barras: ").append(Texto.vazio(livro.codigoBarras) ? "não cadastrado" : livro.codigoBarras);
        ((TextView) v.findViewById(R.id.txtInfoDetalhe)).setText(info);

        Capas.carregar((ImageView) v.findViewById(R.id.imgCapaDetalhe), livro.imagemPath);

        AlertDialog dialogo = new MaterialAlertDialogBuilder(this).setView(v).create();
        v.findViewById(R.id.btnEditarDetalhe).setOnClickListener(x -> {
            dialogo.dismiss();
            Intent intent = new Intent(this, CadastrarLivroActivity.class);
            intent.putExtra(CadastrarLivroActivity.EXTRA_LIVRO_ID, livro.id);
            cadastro.launch(intent);
        });
        v.findViewById(R.id.btnReposicaoDetalhe).setOnClickListener(x -> {
            dialogo.dismiss();
            pedirReposicao(livro);
        });
        v.findViewById(R.id.btnFecharDetalhe).setOnClickListener(x -> dialogo.dismiss());
        dialogo.show();
    }

    private void pedirReposicao(LivroEntity livro) {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_quantidade, null);
        ((TextView) v.findViewById(R.id.txtPerguntaQuantidade))
                .setText("Quantos exemplares de \"" + livro.titulo + "\" você quer pedir?");
        SeletorQuantidade seletor = new SeletorQuantidade(v.findViewById(R.id.seletor), 1, 1, 999);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Pedir mais exemplares")
                .setView(v)
                .setPositiveButton("Pôr na lista de pedidos", (d, w) -> {
                    int total = PedidosService.adicionarOuSomar(AppDatabase.getInstance(this),
                            livro.titulo, livro.autor, seletor.getValor());
                    Avisos.sucesso(this, "\"" + livro.titulo + "\" está na lista de pedidos ("
                            + Texto.plural(total, "exemplar", "exemplares") + ").");
                })
                .setNegativeButton(R.string.cancelar, null)
                .show();
    }
}
