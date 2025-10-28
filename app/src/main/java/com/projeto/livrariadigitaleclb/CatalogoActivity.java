package com.projeto.livrariadigitaleclb;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import com.projeto.livrariadigitaleclb.databinding.ActivityCatalogoBinding;
import java.util.ArrayList;
import java.util.List;

public class CatalogoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityCatalogoBinding binding = ActivityCatalogoBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Botão Home — volta sem piscar
        binding.btnHome.setOnClickListener(v -> finish());

        // Configuração da RecyclerView
        List<Livro> livros = new ArrayList<>();
        livros.add(new Livro(true)); // Botão "Cadastrar Livro"

        LivroAdapter adapter = new LivroAdapter(livros, this);
        binding.recyclerLivros.setLayoutManager(new GridLayoutManager(this, 3));
        binding.recyclerLivros.setAdapter(adapter);
    }
}
