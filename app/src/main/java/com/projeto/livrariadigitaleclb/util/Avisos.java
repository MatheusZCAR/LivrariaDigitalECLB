package com.projeto.livrariadigitaleclb.util;

import android.app.Activity;
import android.view.View;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.projeto.livrariadigitaleclb.R;
import com.projeto.livrariadigitaleclb.ui.BaseActivity;

/**
 * Avisos grandes e que ficam tempo suficiente na tela.
 * Substitui os Toasts, que eram pequenos e sumiam rápido demais para quem tem dificuldade de leitura.
 */
public final class Avisos {

    private static final int DURACAO_MS = 5000;

    private Avisos() {}

    public static Snackbar sucesso(Activity a, String mensagem) {
        return mostrar(a, mensagem, R.color.sucesso);
    }

    public static Snackbar info(Activity a, String mensagem) {
        return mostrar(a, mensagem, R.color.texto);
    }

    /** Aviso com botão de ação (ex.: "Desfazer"). */
    public static Snackbar comAcao(Activity a, String mensagem, String acao, Runnable aoTocar) {
        Snackbar s = criar(a, mensagem, R.color.texto);
        s.setAction(acao, v -> aoTocar.run());
        s.setActionTextColor(ContextCompat.getColor(a, R.color.white));
        s.show();
        return s;
    }

    /** Problemas importantes viram uma janela que só fecha quando a pessoa toca em "Entendi". */
    public static void erro(Activity a, String titulo, String mensagem) {
        if (a.isFinishing() || a.isDestroyed()) return;
        new MaterialAlertDialogBuilder(a)
                .setIcon(R.drawable.ic_aviso)
                .setTitle(titulo)
                .setMessage(mensagem)
                .setPositiveButton(R.string.entendi, null)
                .show();
    }

    private static Snackbar mostrar(Activity a, String mensagem, int cor) {
        Snackbar s = criar(a, mensagem, cor);
        s.show();
        return s;
    }

    private static Snackbar criar(Activity a, String mensagem, int cor) {
        View raiz = a.findViewById(android.R.id.content);
        Snackbar s = Snackbar.make(raiz, mensagem, DURACAO_MS);
        if (a instanceof BaseActivity) {
            View ancora = ((BaseActivity) a).ancoraDosAvisos();
            if (ancora != null && ancora.getVisibility() == View.VISIBLE) s.setAnchorView(ancora);
        }
        s.setBackgroundTint(ContextCompat.getColor(a, cor));
        TextView texto = s.getView().findViewById(com.google.android.material.R.id.snackbar_text);
        if (texto != null) {
            texto.setMaxLines(5);
        }
        return s;
    }
}
