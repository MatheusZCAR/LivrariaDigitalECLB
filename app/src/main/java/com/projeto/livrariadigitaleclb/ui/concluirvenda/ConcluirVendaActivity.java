package com.projeto.livrariadigitaleclb.ui.concluirvenda;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.projeto.livrariadigitaleclb.data.local.AppDatabase;
import com.projeto.livrariadigitaleclb.data.local.dao.LivroDao;
import com.projeto.livrariadigitaleclb.data.local.dao.VendaDao;
import com.projeto.livrariadigitaleclb.data.local.entity.LivroEntity;
import com.projeto.livrariadigitaleclb.data.local.entity.Venda;
import com.projeto.livrariadigitaleclb.databinding.ActivityConcluirVendaBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ConcluirVendaActivity extends AppCompatActivity {

    private ActivityConcluirVendaBinding binding;
    private LivroDao livroDao;
    private VendaDao vendaDao;
    private CarrinhoAdapter adapter;
    private List<ItemCarrinho> itensCarrinho = new ArrayList<>();
    private double valorTotal = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityConcluirVendaBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        AppDatabase db = AppDatabase.getInstance(this);
        livroDao = db.livroDao();
        vendaDao = db.vendaDao();

        carregarCarrinho();
        configurarLista();
        configurarBotoes();
        atualizarTotal();
    }

    private void carregarCarrinho() {
        // Recebe os dados do carrinho
        ArrayList<Long> livroIds = (ArrayList<Long>) getIntent().getSerializableExtra("livroIds");
        ArrayList<Integer> quantidades = (ArrayList<Integer>) getIntent().getSerializableExtra("quantidades");

        if (livroIds != null && quantidades != null) {
            // Múltiplos itens
            for (int i = 0; i < livroIds.size(); i++) {
                LivroEntity livro = livroDao.getLivroById(livroIds.get(i).intValue()); // Conversão para int
                if (livro != null) {
                    itensCarrinho.add(new ItemCarrinho(livro, quantidades.get(i)));
                }
            }
        } else {
            // Compatibilidade com versão antiga (único item)
            long livroId = getIntent().getLongExtra("livroId", -1);
            if (livroId != -1) {
                LivroEntity livro = livroDao.getLivroById((int) livroId); // Conversão para int
                if (livro != null) {
                    itensCarrinho.add(new ItemCarrinho(livro, 1));
                }
            }
        }

        if (itensCarrinho.isEmpty()) {
            Toast.makeText(this, "Erro ao carregar carrinho", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void configurarLista() {
        adapter = new CarrinhoAdapter(
                itensCarrinho,
                this::onQuantidadeAlterada,
                this::onRemoverItem
        );

        binding.recyclerCarrinho.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerCarrinho.setAdapter(adapter);
    }

    private void onQuantidadeAlterada(ItemCarrinho item, int novaQuantidade) {
        if (novaQuantidade <= 0) {
            onRemoverItem(item);
            return;
        }

        item.quantidade = novaQuantidade;
        atualizarTotal();
        adapter.notifyDataSetChanged();
    }

    private void onRemoverItem(ItemCarrinho item) {
        new AlertDialog.Builder(this)
                .setTitle("Remover Item")
                .setMessage("Deseja remover \"" + item.livro.titulo + "\" do carrinho?")
                .setPositiveButton("Sim", (dialog, which) -> {
                    itensCarrinho.remove(item);
                    adapter.notifyDataSetChanged();
                    atualizarTotal();

                    if (itensCarrinho.isEmpty()) {
                        Toast.makeText(this, "Carrinho vazio", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_CANCELED);
                        finish();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void atualizarTotal() {
        valorTotal = 0.0;
        int totalItens = 0;

        for (ItemCarrinho item : itensCarrinho) {
            valorTotal += item.livro.preco * item.quantidade;
            totalItens += item.quantidade;
        }

        binding.txtTotalItens.setText(totalItens + " item(ns)");
        binding.txtValorTotal.setText(String.format("R$ %.2f", valorTotal));
    }

    private void configurarBotoes() {
        binding.btnCancelar.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Cancelar Venda")
                    .setMessage("Deseja realmente cancelar esta venda?")
                    .setPositiveButton("Sim", (dialog, which) -> {
                        setResult(RESULT_CANCELED);
                        finish();
                    })
                    .setNegativeButton("Não", null)
                    .show();
        });

        binding.btnConfirmar.setOnClickListener(v -> confirmarVenda());
    }

    private void confirmarVenda() {
        new AlertDialog.Builder(this)
                .setTitle("Confirmar Venda")
                .setMessage("Total: R$ " + String.format("%.2f", valorTotal) + "\n\nConfirmar venda?")
                .setPositiveButton("Confirmar", (dialog, which) -> {
                    registrarVendas();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void registrarVendas() {
        long dataAtual = System.currentTimeMillis(); // Timestamp atual

        try {
            // Registra cada item vendido
            for (ItemCarrinho item : itensCarrinho) {
                for (int i = 0; i < item.quantidade; i++) {
                    Venda venda = new Venda();
                    venda.livroId = item.livro.id;
                    venda.titulo = item.livro.titulo;
                    venda.preco = item.livro.preco;
                    venda.dataVenda = dataAtual;

                    vendaDao.registrarVenda(venda);  // MUDANÇA AQUI
                }
            }

            Toast.makeText(this,
                    "Venda registrada com sucesso!",
                    Toast.LENGTH_LONG).show();

            setResult(RESULT_OK);
            finish();

        } catch (Exception e) {
            Toast.makeText(this,
                    "Erro ao registrar venda: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    // Classe auxiliar para representar item do carrinho
    public static class ItemCarrinho {
        public LivroEntity livro;
        public int quantidade;

        public ItemCarrinho(LivroEntity livro, int quantidade) {
            this.livro = livro;
            this.quantidade = quantidade;
        }

        public double getSubtotal() {
            return livro.preco * quantidade;
        }
    }
}