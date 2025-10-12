package com.projeto.livrariadigitaleclb;

import android.content.Intent;
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

        // Navegar de volta para MainActivity
        binding.btnHome.setOnClickListener(v -> {
            Intent intent = new Intent(CatalogoActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        });

        // Setup RecyclerView
        List<Livro> livros = new ArrayList<>();
        livros.add(new Livro(true)); // Botão cadastrar livro
        // livros.add(new Livro("Título", "URL ou ID da imagem")) ← para uso futuro

        LivroAdapter adapter = new LivroAdapter(livros, this);
        binding.recyclerLivros.setLayoutManager(new GridLayoutManager(this, 3));
        binding.recyclerLivros.setAdapter(adapter);
    }
}
