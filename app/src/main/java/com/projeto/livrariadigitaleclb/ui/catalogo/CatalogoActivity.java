package com.projeto.livrariadigitaleclb.ui.catalogo;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;

import com.projeto.livrariadigitaleclb.data.local.AppDatabase;
import com.projeto.livrariadigitaleclb.data.local.entity.LivroComEstoque; // Importante: Novo import
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

    @Override
    protected void onResume() {
        super.onResume();
        carregarLivros();
    }

    private void configurarRecycler() {
        binding.recyclerLivros.setLayoutManager(new GridLayoutManager(this, 5));
        adapter = new LivroAdapter(this, null);
        binding.recyclerLivros.setAdapter(adapter);
    }

    private void carregarLivros() {
        AppDatabase db = AppDatabase.getInstance(this);

        // MUDANÇA PRINCIPAL: Usamos o método que traz o estoque junto (JOIN)
        List<LivroComEstoque> livros = db.livroDao().listarLivrosComEstoque();

        adapter.atualizarLista(livros);
    }
}
