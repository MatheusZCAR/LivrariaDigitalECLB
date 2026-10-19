package com.projeto.livrariadigitaleclb.ui.realizarvenda;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.recyclerview.widget.GridLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import com.projeto.livrariadigitaleclb.R;
import com.projeto.livrariadigitaleclb.data.Carrinho;
import com.projeto.livrariadigitaleclb.data.local.AppDatabase;
import com.projeto.livrariadigitaleclb.data.local.entity.LivroComEstoque;
import com.projeto.livrariadigitaleclb.data.local.entity.LivroEntity;
import com.projeto.livrariadigitaleclb.databinding.ActivityRealizarVendaBinding;
import com.projeto.livrariadigitaleclb.ui.BaseActivity;
import com.projeto.livrariadigitaleclb.ui.catalogo.CadastrarLivroActivity;
import com.projeto.livrariadigitaleclb.ui.catalogo.LivrosAdapter;
import com.projeto.livrariadigitaleclb.ui.concluirvenda.ConcluirVendaActivity;
import com.projeto.livrariadigitaleclb.util.Avisos;
import com.projeto.livrariadigitaleclb.util.Moeda;
import com.projeto.livrariadigitaleclb.util.Telas;
import com.projeto.livrariadigitaleclb.util.Texto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RealizarVendaActivity extends BaseActivity {

    private ActivityRealizarVendaBinding binding;
    private LivrosAdapter adapter;
    private AppDatabase db;
    private final Carrinho carrinho = Carrinho.get();
    private List<LivroComEstoque> todos = new ArrayList<>();

    private final ActivityResultLauncher<ScanOptions> leitor =
            registerForActivityResult(new ScanContract(), r -> {
                if (r.getContents() != null) buscarPorCodigo(r.getContents().trim());
            });

    private final ActivityResultLauncher<Intent> finalizar =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), r -> {
                if (r.getResultCode() == RESULT_OK) {
                    Avisos.sucesso(this, "Venda registrada. Pronto para a próxima!");
                }
            });

    private final ActivityResultLauncher<Intent> cadastro =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), r -> {
                if (r.getResultCode() == RESULT_OK && r.getData() != null) {
                    int id = r.getData().getIntExtra(CadastrarLivroActivity.EXTRA_LIVRO_ID, -1);
                    carregar();
                    LivroComEstoque novo = encontrar(id);
                    if (novo != null) adicionar(novo);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRealizarVendaBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        definirTitulo(getString(R.string.venda_titulo));
        db = AppDatabase.getInstance(this);

        adapter = new LivrosAdapter(this::adicionar, carrinho::quantidade);
        binding.recyclerProdutos.setLayoutManager(
                new GridLayoutManager(this, Telas.colunas(this, R.dimen.largura_minima_livro)));
        binding.recyclerProdutos.setAdapter(adapter);

        binding.searchEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { filtrar(); }
            @Override public void afterTextChanged(Editable s) { }
        });

        boolean temCamera = getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY);
        binding.btnScan.setVisibility(temCamera ? View.VISIBLE : View.GONE);
        binding.btnScan.setOnClickListener(v -> lerCodigo());
        binding.btnVerCarrinho.setOnClickListener(v -> abrirFinalizacao());
        binding.btnLimparCarrinho.setOnClickListener(v -> confirmarEsvaziar());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                sair();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        carregar();
    }

    @Override
    public View ancoraDosAvisos() {
        return binding.layoutCarrinho;
    }

    private void carregar() {
        todos = db.livroDao().listarLivrosParaVendaComEstoque();
        // Tira do carrinho livros que foram excluídos e ajusta ao estoque atual.
        for (Map.Entry<Integer, Integer> item : carrinho.itens().entrySet()) {
            LivroComEstoque l = encontrar(item.getKey());
            if (l == null) {
                carrinho.remover(item.getKey());
            } else if (item.getValue() > l.quantidade) {
                carrinho.definir(item.getKey(), l.quantidade);
            }
        }
        filtrar();
        atualizarBarra();
    }

    private LivroComEstoque encontrar(int livroId) {
        for (LivroComEstoque l : todos) if (l.livro.id == livroId) return l;
        return null;
    }

    private void filtrar() {
        String busca = binding.searchEditText.getText() == null ? "" : binding.searchEditText.getText().toString();
        List<LivroComEstoque> filtrados = new ArrayList<>();
        for (LivroComEstoque l : todos) {
            if (Texto.combina(busca, l.livro.titulo, l.livro.autor, l.livro.codigoBarras)) filtrados.add(l);
        }
        adapter.setItens(filtrados);
        if (filtrados.isEmpty()) {
            binding.txtVazio.setVisibility(View.VISIBLE);
            binding.txtVazio.setText(todos.isEmpty()
                    ? "Nenhum livro cadastrado.\nCadastre os livros na tela \"Livros\"."
                    : getString(R.string.busca_vazia));
        } else {
            binding.txtVazio.setVisibility(View.GONE);
        }
    }

    private void adicionar(LivroComEstoque item) {
        LivroEntity livro = item.livro;
        int naVenda = carrinho.quantidade(livro.id);
        if (item.quantidade <= 0) {
            Avisos.info(this, "\"" + livro.titulo + "\" está sem estoque.");
            return;
        }
        if (naVenda >= item.quantidade) {
            Avisos.info(this, "Só há " + Texto.plural(item.quantidade, "exemplar", "exemplares")
                    + " de \"" + livro.titulo + "\" e todos já estão na venda.");
            return;
        }
        carrinho.adicionar(livro.id, 1);
        adapter.atualizarLivro(livro.id);
        atualizarBarra();

        int agora = carrinho.quantidade(livro.id);
        Avisos.comAcao(this, "\"" + livro.titulo + "\": " + agora + " na venda.", "Desfazer", () -> {
            carrinho.definir(livro.id, carrinho.quantidade(livro.id) - 1);
            adapter.atualizarLivro(livro.id);
            atualizarBarra();
        });
    }

    private void atualizarBarra() {
        if (carrinho.vazio()) {
            binding.layoutCarrinho.setVisibility(View.GONE);
            return;
        }
        double total = 0;
        for (Map.Entry<Integer, Integer> item : carrinho.itens().entrySet()) {
            LivroComEstoque l = encontrar(item.getKey());
            if (l != null) total += l.livro.preco * item.getValue();
        }
        binding.layoutCarrinho.setVisibility(View.VISIBLE);
        binding.txtTotalItens.setText(Texto.plural(carrinho.totalUnidades(), "livro", "livros") + " na venda");
        binding.txtValorTotal.setText(Moeda.formatar(total));
    }

    private void lerCodigo() {
        ScanOptions opcoes = new ScanOptions();
        opcoes.setPrompt("Aponte a câmera para o código de barras do livro");
        opcoes.setBeepEnabled(true);
        opcoes.setOrientationLocked(false);
        leitor.launch(opcoes);
    }

    private void buscarPorCodigo(String codigo) {
        LivroEntity livro = db.livroDao().buscarPorCodigo(codigo);
        if (livro != null) {
            LivroComEstoque item = encontrar(livro.id);
            if (item != null) {
                adicionar(item);
                return;
            }
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle("Livro não encontrado")
                .setMessage("Nenhum livro tem o código " + codigo + ".\n\nQuer cadastrar este livro agora?")
                .setPositiveButton("Sim, cadastrar", (d, w) -> {
                    Intent i = new Intent(this, CadastrarLivroActivity.class);
                    i.putExtra(CadastrarLivroActivity.EXTRA_CODIGO, codigo);
                    cadastro.launch(i);
                })
                .setNegativeButton("Não", null)
                .show();
    }

    private void abrirFinalizacao() {
        if (carrinho.vazio()) {
            Avisos.info(this, "Toque em um livro para colocá-lo na venda.");
            return;
        }
        finalizar.launch(new Intent(this, ConcluirVendaActivity.class));
    }

    private void confirmarEsvaziar() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Esvaziar a venda?")
                .setMessage("Todos os livros escolhidos serão tirados desta venda.")
                .setPositiveButton("Sim, esvaziar", (d, w) -> {
                    carrinho.limpar();
                    adapter.notifyDataSetChanged();
                    atualizarBarra();
                })
                .setNegativeButton("Não", null)
                .show();
    }

    private void sair() {
        if (carrinho.vazio()) {
            finish();
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle("A venda não foi finalizada")
                .setMessage("Há " + Texto.plural(carrinho.totalUnidades(), "livro", "livros")
                        + " escolhidos. O que você quer fazer?")
                .setPositiveButton("Continuar vendendo", null)
                .setNegativeButton("Esvaziar e sair", (d, w) -> {
                    carrinho.limpar();
                    finish();
                })
                .setNeutralButton("Guardar e sair", (d, w) -> finish())
                .show();
    }
}
