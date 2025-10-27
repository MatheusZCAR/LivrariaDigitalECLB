package com.projeto.livrariadigitaleclb;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;

import com.projeto.livrariadigitaleclb.databinding.ActivityRealizarVendaBinding;

import java.util.ArrayList;
import java.util.List;

public class RealizarVendaActivity extends AppCompatActivity {

    private ActivityRealizarVendaBinding binding;
    private ProdutoAdapter adapter;
    private List<Produto> listaProdutos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRealizarVendaBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        inicializarLista();
        configurarRecycler();

        binding.btnHome.setOnClickListener(v -> {
            finish(); // ou volte para o menu inicial
        });

        binding.btnPesquisar.setOnClickListener(v -> {
            String termo = binding.edtPesquisa.getText().toString().trim();
            filtrarProdutos(termo);
        });
    }

    private void inicializarLista() {
        listaProdutos = new ArrayList<>();
        listaProdutos.add(new Produto("Leaf Shapes", "Angela Powers", 49.90, R.drawable.leaf_shapes));
        listaProdutos.add(new Produto("See You Later", "Connie May", 39.90, R.drawable.see_you_later));
        listaProdutos.add(new Produto("No Caminho da Luz", "Edmar Silva", 59.90, R.drawable.no_caminho_da_luz));
        listaProdutos.add(new Produto("Overcomer", "Patrick Jackson", 44.90, R.drawable.overcomer));
        listaProdutos.add(new Produto("As Aventuras de Lily", "Ana Mendes", 34.90, R.drawable.aventuras_lily));
        listaProdutos.add(new Produto("The Arrival", "Unknown", 29.90, R.drawable.the_arrival));
        // adicione mais se quiser
    }

    private void configurarRecycler() {
        adapter = new ProdutoAdapter(listaProdutos, produto -> {
            Intent intent = new Intent(this, ConcluirVendaActivity.class);
            intent.putExtra("titulo", produto.getTitulo());
            intent.putExtra("autor", produto.getAutor());
            intent.putExtra("preco", produto.getPreco());
            intent.putExtra("imagem", produto.getImagemRes());
            startActivity(intent);
        });

        GridLayoutManager layoutManager = new GridLayoutManager(this, 3);
        binding.recyclerProdutos.setLayoutManager(layoutManager);
        binding.recyclerProdutos.setHasFixedSize(true);
        binding.recyclerProdutos.setAdapter(adapter);
    }

    private void filtrarProdutos(String termo) {
        if (termo.isEmpty()) {
            adapter.atualizarLista(listaProdutos);
            return;
        }

        List<Produto> filtrados = new ArrayList<>();
        for (Produto p : listaProdutos) {
            if (p.getTitulo().toLowerCase().contains(termo.toLowerCase())) {
                filtrados.add(p);
            }
        }

        if (filtrados.isEmpty()) {
            Toast.makeText(this, "Nenhum produto encontrado", Toast.LENGTH_SHORT).show();
        }

        adapter.atualizarLista(filtrados);
    }
}
