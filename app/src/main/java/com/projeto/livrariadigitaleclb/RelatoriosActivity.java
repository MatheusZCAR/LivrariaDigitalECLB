package com.projeto.livrariadigitaleclb;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.projeto.livrariadigitaleclb.databinding.ActivityRelatoriosBinding;

public class RelatoriosActivity extends AppCompatActivity {

    private ActivityRelatoriosBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRelatoriosBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Botão Home — volta sem piscar preto
        binding.btnHome.setOnClickListener(v -> finish());

        // Botão Imprimir
        binding.btnImprimir.setOnClickListener(v -> {
            int checkedId = binding.radioGroupRelatorios.getCheckedRadioButtonId();

            String tipo = "";
            if (checkedId == binding.radioDiario.getId()) {
                tipo = "Relatório Diário";
            } else if (checkedId == binding.radioSemanal.getId()) {
                tipo = "Relatório Semanal";
            } else if (checkedId == binding.radioMensal.getId()) {
                tipo = "Relatório Mensal";
            }

            if (!tipo.isEmpty()) {
                Toast.makeText(this, "Gerando " + tipo, Toast.LENGTH_SHORT).show();
                // TODO: gerar ou exibir relatório real
            } else {
                Toast.makeText(this, "Selecione um tipo de relatório", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
