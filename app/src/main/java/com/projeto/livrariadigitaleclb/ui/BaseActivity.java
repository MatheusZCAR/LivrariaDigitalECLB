package com.projeto.livrariadigitaleclb.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.projeto.livrariadigitaleclb.R;
import com.projeto.livrariadigitaleclb.util.Telas;

/**
 * Base de todas as telas: barras do sistema corretas no Android 15/16 e
 * barra do topo padrão (botão "Voltar" grande + título).
 */
public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        // Ícones brancos na barra de status (sobre a barra verde) e escuros na barra de navegação.
        EdgeToEdge.enable(this,
                SystemBarStyle.dark(Color.TRANSPARENT),
                SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT));
        super.onCreate(savedInstanceState);
    }

    @Override
    public void setContentView(View view) {
        super.setContentView(view);
        Telas.aplicarMargensDoSistema(view);
        View voltar = view.findViewById(R.id.btnVoltar);
        if (voltar != null) {
            voltar.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        }
    }

    /**
     * Barra fixa no rodapé desta tela (se houver): os avisos aparecem acima dela,
     * em vez de cobrir os botões.
     */
    public View ancoraDosAvisos() {
        return null;
    }

    protected void definirTitulo(String titulo) {
        TextView t = findViewById(R.id.txtTituloTela);
        if (t != null) t.setText(titulo);
        setTitle(titulo);
    }
}
