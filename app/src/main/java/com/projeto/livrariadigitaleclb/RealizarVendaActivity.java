package com.projeto.livrariadigitaleclb;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import com.projeto.livrariadigitaleclb.databinding.ActivityRealizarVendaBinding;
import java.util.ArrayList;
import java.util.List;



public class RealizarVendaActivity extends AppCompatActivity {

    private ActivityRealizarVendaBinding binding;
    private ProdutoAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRealizarVendaBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        List<Produto> produtos = gerarProdutosExemplo();

        adapter = new ProdutoAdapter(produtos, produto -> {
            Intent intent = new Intent(this, ConcluirVendaActivity.class);
            intent.putExtra("produto", produto);
            startActivity(intent);
        });

        binding.recyclerProdutos.setLayoutManager(new GridLayoutManager(this, 3));
        binding.recyclerProdutos.setAdapter(adapter);
    }

    private List<Produto> gerarProdutosExemplo() {
        List<Produto> lista = new ArrayList<>();
        lista.add(new Produto("Leaf Shapes", "Angela Mireles", 29.90, R.drawable.leaf_shapes));
        lista.add(new Produto("See You Later", "Joe Mendes", 35.50, R.drawable.see_you_later));
        lista.add(new Produto("No Caminho da Luz", "A. Souza", 27.90, R.drawable.no_caminho));
        return lista;
    }
}

