package com.projeto.livrariadigitaleclb.util;

import android.view.View;
import android.widget.TextView;

/**
 * Controla um trio de botões "−  3  +" para escolher quantidades sem precisar digitar.
 * Usado com o layout view_seletor_quantidade.xml.
 */
public class SeletorQuantidade {

    public interface AoMudar {
        void mudou(int novoValor);
    }

    private final View menos;
    private final View mais;
    private final TextView valorView;
    private int valor;
    private int minimo;
    private int maximo;
    private AoMudar ouvinte;
    private Runnable aoPassarDoMaximo;

    public SeletorQuantidade(View raiz, int inicial, int minimo, int maximo) {
        this.menos = raiz.findViewById(com.projeto.livrariadigitaleclb.R.id.btnMenos);
        this.mais = raiz.findViewById(com.projeto.livrariadigitaleclb.R.id.btnMais);
        this.valorView = raiz.findViewById(com.projeto.livrariadigitaleclb.R.id.txtValorQuantidade);
        this.minimo = minimo;
        this.maximo = maximo;
        menos.setOnClickListener(v -> definir(valor - 1, true));
        mais.setOnClickListener(v -> {
            if (valor >= this.maximo && aoPassarDoMaximo != null) {
                aoPassarDoMaximo.run();
            } else {
                definir(valor + 1, true);
            }
        });
        definir(inicial, false);
    }

    public void setAoMudar(AoMudar ouvinte) {
        this.ouvinte = ouvinte;
    }

    public void setAoPassarDoMaximo(Runnable r) {
        this.aoPassarDoMaximo = r;
    }

    public void setLimites(int minimo, int maximo) {
        this.minimo = minimo;
        this.maximo = maximo;
        definir(valor, false);
    }

    public int getValor() {
        return valor;
    }

    public void definir(int novo, boolean avisar) {
        int ajustado = Math.max(minimo, Math.min(maximo, novo));
        boolean mudou = ajustado != valor;
        valor = ajustado;
        valorView.setText(String.valueOf(valor));
        menos.setEnabled(valor > minimo);
        menos.setAlpha(valor > minimo ? 1f : 0.4f);
        mais.setAlpha(valor < maximo ? 1f : 0.4f);
        if (avisar && mudou && ouvinte != null) ouvinte.mudou(valor);
    }
}
