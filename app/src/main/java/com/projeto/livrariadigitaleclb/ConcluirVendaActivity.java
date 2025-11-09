package com.projeto.livrariadigitaleclb;

import android.annotation.SuppressLint;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.projeto.livrariadigitaleclb.databinding.ActivityConcluirVendaBinding;

public class ConcluirVendaActivity extends AppCompatActivity {

    private ActivityConcluirVendaBinding binding;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityConcluirVendaBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Produto produto = (Produto) getIntent().getSerializableExtra("produto");

        if (produto != null) {
            binding.imageLivro.setImageResource(produto.getImagem());
            binding.textInfoLivro.setText(
                    produto.getTitulo() + "\n" +
                            produto.getAutor() + "\n" +
                            "R$ " + produto.getPreco());
        }

        binding.btnCancelar.setOnClickListener(v -> finish());
        binding.btnConfirmar.setOnClickListener(v -> {
            // Aqui você poderia salvar a venda
            finish();
        });
    }
}

