package com.projeto.livrariadigitaleclb.util;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.projeto.livrariadigitaleclb.R;

/** Janelas de escolha com botões grandes (em vez de listas de texto miúdo). */
public final class Dialogos {

    private Dialogos() {}

    public static final class Opcao {
        final String texto;
        final int icone;
        final Runnable acao;

        public Opcao(String texto, int icone, Runnable acao) {
            this.texto = texto;
            this.icone = icone;
            this.acao = acao;
        }
    }

    public static AlertDialog opcoes(Activity a, String titulo, String mensagem, Opcao... opcoes) {
        View v = LayoutInflater.from(a).inflate(R.layout.dialog_opcoes, null);
        TextView txt = v.findViewById(R.id.txtMensagemDialogo);
        if (mensagem == null) {
            txt.setVisibility(View.GONE);
        } else {
            txt.setText(mensagem);
        }
        LinearLayout lista = v.findViewById(R.id.listaOpcoes);

        AlertDialog dialogo = new MaterialAlertDialogBuilder(a)
                .setTitle(titulo)
                .setView(v)
                .setNegativeButton(R.string.cancelar, null)
                .create();

        for (Opcao o : opcoes) {
            MaterialButton b = (MaterialButton) LayoutInflater.from(a)
                    .inflate(R.layout.item_opcao_dialogo, lista, false);
            b.setText(o.texto);
            if (o.icone != 0) b.setIconResource(o.icone);
            b.setOnClickListener(x -> {
                dialogo.dismiss();
                o.acao.run();
            });
            lista.addView(b);
        }
        dialogo.show();
        return dialogo;
    }

    /** Pergunta de confirmação simples, com textos claros nos botões. */
    public static void confirmar(Activity a, String titulo, String mensagem,
                                 String textoSim, String textoNao, Runnable aoConfirmar) {
        new MaterialAlertDialogBuilder(a)
                .setTitle(titulo)
                .setMessage(mensagem)
                .setPositiveButton(textoSim, (d, w) -> aoConfirmar.run())
                .setNegativeButton(textoNao, null)
                .show();
    }
}
