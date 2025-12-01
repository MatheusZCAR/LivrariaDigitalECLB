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
import com.projeto.livrariadigitaleclb.data.local.dao.LivroDao;
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

    // Carrinho de compras: Map<livroId, quantidade>
    private Map<Long, Integer> carrinho = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRealizarVendaBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        AppDatabase db = AppDatabase.getInstance(this);
        livroDao = db.livroDao();

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
        List<LivroEntity> livros = livroDao.listarLivrosParaVenda();

        adapter = new ProdutoAdapter(
                livros,
                livro -> adicionarAoCarrinho(livro) // Adiciona ao carrinho ao clicar
        );

        binding.recyclerProdutos.setLayoutManager(new GridLayoutManager(this, 3));
        binding.recyclerProdutos.setAdapter(adapter);
    }

    private void adicionarAoCarrinho(LivroEntity livro) {
        // Verifica se o livro já está no carrinho
        int quantidadeAtual = carrinho.getOrDefault((long) livro.id, 0); // Cast para long
        carrinho.put((long) livro.id, quantidadeAtual + 1); // Cast para long

        Toast.makeText(this,
                livro.titulo + " adicionado ao carrinho",
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
        List<LivroEntity> livros = livroDao.listarLivrosParaVenda();
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
                    List<LivroEntity> filtrados = livroDao.buscarLivros(busca);
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
            if (livro.esgotado) {
                mostrarDialogoLivroEsgotado(livro, codigo);
            } else {
                // Adiciona direto ao carrinho
                adicionarAoCarrinho(livro);
            }
        } else {
            mostrarDialogoLivroNaoEncontrado(codigo);
        }
    }

    private void mostrarDialogoLivroEsgotado(LivroEntity livro, String codigo) {
        new AlertDialog.Builder(this)
                .setTitle("Livro Esgotado")
                .setMessage("O livro \"" + livro.titulo + "\" está marcado como esgotado.\n\nDeseja cadastrar um novo exemplar?")
                .setPositiveButton("Sim, Cadastrar", (dialog, which) -> {
                    Intent intent = new Intent(this, CadastrarLivroActivity.class);
                    intent.putExtra("codigo", codigo);
                    startActivity(intent);
                })
                .setNegativeButton("Cancelar", null)
                .show();
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