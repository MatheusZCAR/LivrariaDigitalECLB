package com.projeto.livrariadigitaleclb;

import android.graphics.Rect;
import android.view.View;

import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.espresso.matcher.ViewMatchers;

import org.hamcrest.Matcher;

import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

/** Ações de teste que funcionam igual no celular e no tablet. */
final class Acoes {

    private Acoes() {}

    /** Rola até a view, se ela estiver dentro de uma área com rolagem (senão não faz nada). */
    static ViewAction rolar() {
        return new ViewAction() {
            @Override public Matcher<View> getConstraints() { return ViewMatchers.isAssignableFrom(View.class); }
            @Override public String getDescription() { return "rolar até a view"; }
            @Override public void perform(UiController ui, View v) {
                v.requestRectangleOnScreen(new Rect(0, 0, v.getWidth(), v.getHeight()), true);
                ui.loopMainThreadUntilIdle();
            }
        };
    }

    /** Toca na view sem depender da posição na tela (um aviso por cima não atrapalha). */
    static ViewAction tocar() {
        return new ViewAction() {
            @Override public Matcher<View> getConstraints() { return ViewMatchers.isEnabled(); }
            @Override public String getDescription() { return "tocar"; }
            @Override public void perform(UiController ui, View v) {
                v.performClick();
                ui.loopMainThreadUntilIdle();
            }
        };
    }

    /** Toca no livro da grade com esse título (rolando a lista se precisar). */
    static ViewAction tocarLivro(String titulo) {
        return RecyclerViewActions.actionOnItem(hasDescendant(withText(titulo)), tocar());
    }
}
