package com.projeto.livrariadigitaleclb.ui.catalogo;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;

import com.projeto.livrariadigitaleclb.data.local.AppDatabase;
import com.projeto.livrariadigitaleclb.data.local.entity.LivroEntity;
import com.projeto.livrariadigitaleclb.databinding.ActivityCatalogoBinding;

import java.util.List;

public class CatalogoActivity extends AppCompatActivity {

    private ActivityCatalogoBinding binding;
    private LivroAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCatalogoBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnHome.setOnClickListener(v -> finish());

        configurarRecycler();
        carregarLivros();
    }

    private void configurarRecycler() {
        binding.recyclerLivros.setLayoutManager(new GridLayoutManager(this, 3));
        adapter = new LivroAdapter(this, null);
        binding.recyclerLivros.setAdapter(adapter);
    }

    private void carregarLivros() {
        AppDatabase db = AppDatabase.getInstance(this);
        List<LivroEntity> livros = db.livroDao().listarLivrosParaVenda();
        adapter.atualizarLista(livros);
    }
}
