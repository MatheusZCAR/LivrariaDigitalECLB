package com.projeto.livrariadigitaleclb;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.projeto.livrariadigitaleclb.databinding.ActivityPedidosBinding;
import java.util.ArrayList;

public class PedidosActivity extends AppCompatActivity {

    private ActivityPedidosBinding binding;
    private ArrayList<String> listaLivros;
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPedidosBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Inicializa lista
        listaLivros = new ArrayList<>();
        listaLivros.add("1. Título do livro – Autor");
        listaLivros.add("2. Título do livro – Autor");
        listaLivros.add("3. Título do livro – Autor");
        listaLivros.add("4. Título do livro – Autor");
        listaLivros.add("5. Título do livro – Autor");

        // Conecta adapter à ListView
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, listaLivros);
        binding.listaPedidos.setAdapter(adapter);

        // Botões
        binding.btnAdicionarLivro.setOnClickListener(v ->
                Toast.makeText(this, "Adicionar livro clicado!", Toast.LENGTH_SHORT).show()
        );

        binding.btnImprimirLista.setOnClickListener(v ->
                Toast.makeText(this, "Imprimir lista clicado!", Toast.LENGTH_SHORT).show()
        );

        // Ícone Home
        binding.iconHome.setOnClickListener(v -> finish());
    }
}
