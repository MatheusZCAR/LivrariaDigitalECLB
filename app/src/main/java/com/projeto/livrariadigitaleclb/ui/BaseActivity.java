package com.projeto.livrariadigitaleclb.ui;

import android.content.pm.ActivityInfo;
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
        // Tablet: sempre na horizontal (o app foi pensado para isso).
        // Celular: segue o aparelho, porque na horizontal sobraria pouca altura para as telas.
        // Obs.: a partir do Android 16 o sistema pode ignorar essa trava em tablets; por isso
        // todas as telas também se adaptam à largura disponível (layouts de celular e tablet).
        setRequestedOrientation(Telas.ehTablet(this)
                ? ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                : ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
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
