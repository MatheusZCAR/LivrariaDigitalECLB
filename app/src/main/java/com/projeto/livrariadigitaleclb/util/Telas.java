package com.projeto.livrariadigitaleclb.util;

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.View;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.projeto.livrariadigitaleclb.R;

/** Ajustes de tela comuns a todas as Activities. */
public final class Telas {

    private Telas() {}

    /**
     * No Android 15+ o app desenha atrás da barra de status e da barra de navegação.
     * Aqui a barra do topo (id barraTopo) ganha espaço para a barra de status e o resto da tela
     * ganha espaço para a barra de navegação e para o teclado — assim nada fica escondido.
     */
    public static void aplicarMargensDoSistema(View raiz) {
        View barraTopo = raiz.findViewById(R.id.barraTopo);
        final int topoOriginal = barraTopo != null ? barraTopo.getPaddingTop() : 0;
        final int esq = raiz.getPaddingLeft();
        final int dir = raiz.getPaddingRight();
        final int cima = raiz.getPaddingTop();
        final int baixo = raiz.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(raiz, (v, insets) -> {
            Insets barras = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            Insets teclado = insets.getInsets(WindowInsetsCompat.Type.ime());
            int inferior = Math.max(barras.bottom, teclado.bottom);

            if (barraTopo != null) {
                barraTopo.setPadding(barraTopo.getPaddingLeft(), topoOriginal + barras.top,
                        barraTopo.getPaddingRight(), barraTopo.getPaddingBottom());
                v.setPadding(esq + barras.left, cima, dir + barras.right, baixo + inferior);
            } else {
                v.setPadding(esq + barras.left, cima + barras.top, dir + barras.right, baixo + inferior);
            }
            return WindowInsetsCompat.CONSUMED;
        });
        ViewCompat.requestApplyInsets(raiz);
    }

    /** Quantas colunas cabem na largura da tela, com no mínimo 2. */
    public static int colunas(Context c, int larguraMinimaDimen) {
        DisplayMetrics dm = c.getResources().getDisplayMetrics();
        float larguraMinima = c.getResources().getDimension(larguraMinimaDimen);
        int n = (int) (dm.widthPixels / larguraMinima);
        return Math.max(2, n);
    }

    public static int dp(Context c, int valor) {
        return Math.round(valor * c.getResources().getDisplayMetrics().density);
    }
}
