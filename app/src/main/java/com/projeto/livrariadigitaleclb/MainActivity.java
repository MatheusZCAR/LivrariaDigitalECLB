package com.projeto.livrariadigitaleclb;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.projeto.livrariadigitaleclb.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // ViewBinding aqui!
        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Botões com clique
        binding.iconCatalogo.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CatalogoActivity.class);
            startActivity(intent);
        });

        binding.iconVenda.setOnClickListener(v -> showToast("Venda clicada!"));
        binding.iconPedidos.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, PedidosActivity.class);
            startActivity(intent);
        });
        binding.iconRelatorios.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RelatoriosActivity.class);
            startActivity(intent);
        });
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
