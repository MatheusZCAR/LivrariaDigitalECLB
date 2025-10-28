package com.projeto.livrariadigitaleclb;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.projeto.livrariadigitaleclb.databinding.ActivityConcluirVendaBinding;


public class ConcluirVendaActivity extends AppCompatActivity {

    private ActivityConcluirVendaBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityConcluirVendaBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Receber dados do produto selecionado
        String titulo = getIntent().getStringExtra("titulo");
        String autor = getIntent().getStringExtra("autor");
        double preco = getIntent().getDoubleExtra("preco", 0.0);
        int imagem = getIntent().getIntExtra("imagem", 0);

        binding.imgLivro.setImageResource(imagem);
        binding.txtInfoLivro.setText(
                titulo + ", " + autor + "\n" +
                        "Preço: R$ " + String.format("%.2f", preco) + "\n" +
                        "Outras informações..."
        );

        binding.btnCancelarVenda.setOnClickListener(v -> {
            Toast.makeText(this, "Venda cancelada", Toast.LENGTH_SHORT).show();
            finish();
        });

        binding.btnConfirmarVenda.setOnClickListener(v -> {
            Toast.makeText(this, "Venda confirmada com sucesso!", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}

