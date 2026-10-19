package com.projeto.livrariadigitaleclb.util;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.view.View;

import androidx.core.graphics.Insets;
import androidx.core.widget.NestedScrollView;
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

    /**
     * Quantas colunas cabem na largura da janela, com no mínimo 2.
     * Usa a largura da janela (e não da tela inteira), então funciona também com tela dividida.
     */
    public static int colunas(Context c, int larguraMinimaDimen) {
        Resources r = c.getResources();
        float densidade = r.getDisplayMetrics().density;
        float larguraJanelaDp = r.getConfiguration().screenWidthDp;
        float larguraMinimaDp = r.getDimension(larguraMinimaDimen) / densidade;
        // desconta a margem lateral da grade
        int n = (int) ((larguraJanelaDp - 20) / larguraMinimaDp);
        return Math.max(2, n);
    }

    /** Verdadeiro em tablets (menor lado da tela com 600dp ou mais). */
    public static boolean ehTablet(Context c) {
        return c.getResources().getConfiguration().smallestScreenWidthDp >= 600;
    }

    /** Rola a tela até mostrar a view (por exemplo, um campo com erro), com um pouco de folga acima. */
    public static void rolarAte(NestedScrollView rolagem, View alvo) {
        rolagem.post(() -> {
            Rect r = new Rect();
            alvo.getDrawingRect(r);
            rolagem.offsetDescendantRectToMyCoords(alvo, r);
            rolagem.smoothScrollTo(0, Math.max(0, r.top - dp(rolagem.getContext(), 24)));
        });
    }

    public static int dp(Context c, int valor) {
        return Math.round(valor * c.getResources().getDisplayMetrics().density);
    }
}
