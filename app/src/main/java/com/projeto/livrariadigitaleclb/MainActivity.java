package com.projeto.livrariadigitaleclb;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.projeto.livrariadigitaleclb.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.iconCatalogo.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CatalogoActivity.class);
            startActivity(intent);
        });

        binding.iconVenda.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RealizarVendaActivity.class);
            startActivity(intent);
        });

        binding.iconPedidos.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, PedidosActivity.class);
            startActivity(intent);
        });

        binding.iconRelatorios.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RelatoriosActivity.class);
            startActivity(intent);
        });
    }
}
