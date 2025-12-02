package com.projeto.livrariadigitaleclb.ui.realizarvenda;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.projeto.livrariadigitaleclb.data.local.AppDatabase;
import com.projeto.livrariadigitaleclb.data.local.dao.EstoqueDao;
import com.projeto.livrariadigitaleclb.data.local.dao.LivroDao;
import com.projeto.livrariadigitaleclb.data.local.entity.EstoqueEntity;
import com.projeto.livrariadigitaleclb.data.local.entity.LivroComEstoque;
import com.projeto.livrariadigitaleclb.data.local.entity.LivroEntity;
import com.projeto.livrariadigitaleclb.databinding.ActivityRealizarVendaBinding;
import com.projeto.livrariadigitaleclb.ui.catalogo.CadastrarLivroActivity;
import com.projeto.livrariadigitaleclb.ui.concluirvenda.ConcluirVendaActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RealizarVendaActivity extends AppCompatActivity {

    private ActivityRealizarVendaBinding binding;
    private ProdutoAdapter adapter;
    private LivroDao livroDao;
    private EstoqueDao estoqueDao;

    // Carrinho de compras: Map<livroId, quantidade>
    private Map<Long, Integer> carrinho = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRealizarVendaBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.btnHome.setOnClickListener(v -> finish());

        // Inicializa Banco e DAOs
        AppDatabase db = AppDatabase.getInstance(this);
        livroDao = db.livroDao();
        estoqueDao = db.estoqueDao();

        configurarLista();
        configurarBusca();
        configurarBotoes();
        atualizarCarrinho();
    }

    @Override
    protected void onResume() {
        super.onResume();
        atualizarLista();
    }

    private void configurarLista() {
        // Busca livros já com a informação de estoque (LivroComEstoque)
        List<LivroComEstoque> livros = livroDao.listarLivrosParaVendaComEstoque();

        adapter = new ProdutoAdapter(
                livros,
                (livro, estoqueDisponivel) -> adicionarAoCarrinho(livro, estoqueDisponivel)
        );
        binding.recyclerProdutos.setLayoutManager(new GridLayoutManager(this, 5));
        binding.recyclerProdutos.setAdapter(adapter);
    }

    private void adicionarAoCarrinho(LivroEntity livro, int estoqueDisponivel) {
        // REGRA 1: Não pode adicionar se estiver esgotado
        if (estoqueDisponivel <= 0) {
            Toast.makeText(this, "Produto esgotado!", Toast.LENGTH_SHORT).show();
            return;
        }

        long id = (long) livro.id;
        int quantidadeNoCarrinho = carrinho.getOrDefault(id, 0);

        // REGRA 2: Não pode adicionar mais do que tem no estoque real
        if (quantidadeNoCarrinho >= estoqueDisponivel) {
            Toast.makeText(this,
                    "Limite de estoque atingido (" + estoqueDisponivel + " unidades)",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Adiciona ao carrinho
        carrinho.put(id, quantidadeNoCarrinho + 1);

        Toast.makeText(this,
                livro.titulo + " adicionado",
                Toast.LENGTH_SHORT).show();

        atualizarCarrinho();
    }

    private void atualizarCarrinho() {
        int totalItens = 0;
        double valorTotal = 0.0;

        // Calcula totais
        for (Map.Entry<Long, Integer> entry : carrinho.entrySet()) {
            LivroEntity livro = livroDao.buscarPorId(entry.getKey());
            if (livro != null) {
                int quantidade = entry.getValue();
                totalItens += quantidade;
                valorTotal += livro.preco * quantidade;
            }
        }

        // Atualiza UI
        if (totalItens > 0) {
            binding.layoutCarrinho.setVisibility(View.VISIBLE);
            binding.txtTotalItens.setText(totalItens + " item(ns)");
            binding.txtValorTotal.setText(String.format("R$ %.2f", valorTotal));
        } else {
            binding.layoutCarrinho.setVisibility(View.GONE);
        }
    }

    private void atualizarLista() {
        // Atualiza a lista para refletir baixas de estoque recentes
        List<LivroComEstoque> livros = livroDao.listarLivrosParaVendaComEstoque();
        adapter.updateList(livros);
    }

    private void configurarBusca() {
        binding.searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String busca = s.toString().trim();

                if (busca.isEmpty()) {
                    atualizarLista();
                } else {
                    // Busca específica que retorna LivroComEstoque
                    List<LivroComEstoque> filtrados = livroDao.buscarLivrosComEstoque(busca);
                    adapter.updateList(filtrados);
                }
            }

            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void afterTextChanged(Editable s) { }
        });
    }

    private void configurarBotoes() {
        binding.btnHome.setOnClickListener(v -> {
            if (!carrinho.isEmpty()) {
                confirmarSaida();
            } else {
                finish();
            }
        });

        binding.btnScan.setOnClickListener(v -> iniciarScanner());

        binding.btnVerCarrinho.setOnClickListener(v -> abrirCarrinho());

        binding.btnLimparCarrinho.setOnClickListener(v -> limparCarrinho());
    }

    private void confirmarSaida() {
        new AlertDialog.Builder(this)
                .setTitle("Carrinho com itens")
                .setMessage("Você tem itens no carrinho. Deseja sair sem finalizar a venda?")
                .setPositiveButton("Sim, sair", (dialog, which) -> finish())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void limparCarrinho() {
        if (carrinho.isEmpty()) {
            Toast.makeText(this, "Carrinho já está vazio", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Limpar Carrinho")
                .setMessage("Deseja remover todos os itens do carrinho?")
                .setPositiveButton("Sim", (dialog, which) -> {
                    carrinho.clear();
                    atualizarCarrinho();
                    Toast.makeText(this, "Carrinho limpo", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void abrirCarrinho() {
        if (carrinho.isEmpty()) {
            Toast.makeText(this, "Carrinho vazio. Adicione itens para continuar.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Passa os dados do carrinho para a tela de concluir venda
        Intent intent = new Intent(this, ConcluirVendaActivity.class);

        // Converte o carrinho para arrays para passar via Intent
        ArrayList<Long> livroIds = new ArrayList<>();
        ArrayList<Integer> quantidades = new ArrayList<>();

        for (Map.Entry<Long, Integer> entry : carrinho.entrySet()) {
            livroIds.add(entry.getKey());
            quantidades.add(entry.getValue());
        }

        intent.putExtra("livroIds", livroIds);
        intent.putExtra("quantidades", quantidades);
        startActivityForResult(intent, 100);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        // Scanner
        IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);

        if (scanResult != null) {
            if (scanResult.getContents() == null) {
                Toast.makeText(this, "Leitura cancelada", Toast.LENGTH_SHORT).show();
            } else {
                String codigo = scanResult.getContents();
                buscarLivroPorCodigo(codigo);
            }
        }
        // Retorno da tela de concluir venda
        else if (requestCode == 100 && resultCode == RESULT_OK) {
            // Venda concluída com sucesso - limpa o carrinho
            carrinho.clear();
            atualizarCarrinho();
            Toast.makeText(this, "Venda concluída com sucesso!", Toast.LENGTH_LONG).show();
            atualizarLista();
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void iniciarScanner() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setPrompt("Aponte para o código de barras do livro");
        integrator.setBeepEnabled(true);
        integrator.setOrientationLocked(true);
        integrator.initiateScan();
    }

    private void buscarLivroPorCodigo(String codigo) {
        LivroEntity livro = livroDao.buscarPorCodigo(codigo);

        if (livro != null) {
            EstoqueEntity estoqueEntity = estoqueDao.buscarPorLivro(livro.id);
            int qtdDisponivel = (estoqueEntity != null) ? estoqueEntity.quantidadeDisponivel : 0;

            adicionarAoCarrinho(livro, qtdDisponivel);

        } else {
            mostrarDialogoLivroNaoEncontrado(codigo);
        }
    }

    private void mostrarDialogoLivroNaoEncontrado(String codigo) {
        new AlertDialog.Builder(this)
                .setTitle("Livro Não Encontrado")
                .setMessage("Não foi encontrado nenhum livro com o código:\n" + codigo + "\n\nDeseja cadastrar este livro?")
                .setPositiveButton("Sim, Cadastrar", (dialog, which) -> {
                    Intent intent = new Intent(this, CadastrarLivroActivity.class);
                    intent.putExtra("codigo", codigo);
                    startActivity(intent);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}
