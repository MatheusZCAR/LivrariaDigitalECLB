package com.projeto.livrariadigitaleclb;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.projeto.livrariadigitaleclb.databinding.ActivityPedidosBinding;
import java.util.ArrayList;

public class PedidosActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityPedidosBinding binding = ActivityPedidosBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Inicializa lista
        ArrayAdapter<String> adapter = getStringArrayAdapter();
        binding.listaPedidos.setAdapter(adapter);

        // Botão: adicionar livro
        binding.btnAdicionarLivro.setOnClickListener(v ->
                Toast.makeText(this, "Adicionar livro clicado!", Toast.LENGTH_SHORT).show()
        );

        // Botão: imprimir lista
        binding.btnImprimirLista.setOnClickListener(v ->
                Toast.makeText(this, "Imprimir lista clicado!", Toast.LENGTH_SHORT).show()
        );

        // Ícone Home
        binding.iconHome.setOnClickListener(v -> finish());
    }

    @NonNull
    private ArrayAdapter<String> getStringArrayAdapter() {
        ArrayList<String> listaLivros = new ArrayList<>();
        listaLivros.add("1. Título do livro – Autor");
        listaLivros.add("2. Título do livro – Autor");
        listaLivros.add("3. Título do livro – Autor");
        listaLivros.add("4. Título do livro – Autor");
        listaLivros.add("5. Título do livro – Autor");

        // Conecta adapter à ListView com layout personalizado (texto preto)
        return new ArrayAdapter<>(this, R.layout.item_lista_pedido, listaLivros);
    }
}
