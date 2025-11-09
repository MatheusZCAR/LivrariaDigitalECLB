package com.projeto.livrariadigitaleclb;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import com.projeto.livrariadigitaleclb.databinding.ActivityRealizarVendaBinding;
import java.util.ArrayList;
import java.util.List;



public class RealizarVendaActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityRealizarVendaBinding binding = ActivityRealizarVendaBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        List<Produto> produtos = gerarProdutosExemplo();

        ProdutoAdapter adapter = new ProdutoAdapter(produtos, produto -> {
            Intent intent = new Intent(this, ConcluirVendaActivity.class);
            intent.putExtra("produto", produto);
            startActivity(intent);
        });

        binding.recyclerProdutos.setLayoutManager(new GridLayoutManager(this, 3));
        binding.recyclerProdutos.setAdapter(adapter);

        binding.btnHome.setOnClickListener(v -> finish());
    }

    private List<Produto> gerarProdutosExemplo() {
        List<Produto> lista = new ArrayList<>();
        lista.add(new Produto("No Caminho da Luz", "Alexandre Timóteo", 29.90, R.drawable.caminho_luz));
        lista.add(new Produto("Anoitecer com Aline", "Graziele Farias", 35.50, R.drawable.anoitecer_aline));
        lista.add(new Produto("Encontrei Você", "Ana Morais", 27.90, R.drawable.encontrei_voce));
        return lista;
    }
}
