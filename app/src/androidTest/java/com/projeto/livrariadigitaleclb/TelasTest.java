package com.projeto.livrariadigitaleclb;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.projeto.livrariadigitaleclb.data.Carrinho;
import com.projeto.livrariadigitaleclb.data.RelatorioVendas;
import com.projeto.livrariadigitaleclb.data.local.AppDatabase;
import com.projeto.livrariadigitaleclb.data.local.entity.LivroEntity;
import com.projeto.livrariadigitaleclb.ui.backup.BackupActivity;
import com.projeto.livrariadigitaleclb.ui.catalogo.CadastrarLivroActivity;
import com.projeto.livrariadigitaleclb.ui.catalogo.CatalogoActivity;
import com.projeto.livrariadigitaleclb.ui.concluirvenda.ConcluirVendaActivity;
import com.projeto.livrariadigitaleclb.ui.main.MainActivity;
import com.projeto.livrariadigitaleclb.ui.pedidos.PedidosActivity;
import com.projeto.livrariadigitaleclb.ui.realizarvenda.RealizarVendaActivity;
import com.projeto.livrariadigitaleclb.ui.relatorios.RelatoriosActivity;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Abre todas as telas num tablet (emulador), faz uma venda completa e tira fotos das telas
 * para conferência visual.
 */
@RunWith(AndroidJUnit4.class)
public class TelasTest {

    @Before
    public void preparar() {
        Dados.recriarBanco();
        Dados.marcarBackupFeito();
    }

    private static <T extends android.app.Activity> ActivityScenario<T> abrir(Class<T> tela) {
        return ActivityScenario.launch(new Intent(Dados.contexto(), tela));
    }

    @Test
    public void telaInicial() {
        try (ActivityScenario<MainActivity> s = abrir(MainActivity.class)) {
            onView(withText("Vender")).check(matches(isDisplayed()));
            Dados.foto("01_inicio");
        }
    }

    @Test
    public void catalogoBuscaEDetalhes() {
        try (ActivityScenario<CatalogoActivity> s = abrir(CatalogoActivity.class)) {
            Dados.foto("02_catalogo");
            // Busca sem acento encontra "Bíblia"
            onView(withId(R.id.edtBusca)).perform(replaceText("biblia"), closeSoftKeyboard());
            onView(withId(R.id.txtContagem)).check(matches(withText("1 livro encontrado")));
            onView(withId(R.id.edtBusca)).perform(replaceText(""), closeSoftKeyboard());

            onView(withId(R.id.recyclerLivros)).perform(Acoes.tocarLivro("Hinos do Povo de Deus"));
            Dados.foto("03_detalhes_livro");
            onView(withId(R.id.btnReposicaoDetalhe)).inRoot(isDialog()).perform(click());
            Dados.foto("04_pedir_reposicao");
            onView(withText("Pôr na lista de pedidos")).inRoot(isDialog()).perform(click());
        }
        assertEquals(3, AppDatabase.getInstance(Dados.contexto()).pedidoDao().getPedidos().size());
    }

    @Test
    public void cadastroValidaCampos() {
        try (ActivityScenario<CadastrarLivroActivity> s = abrir(CadastrarLivroActivity.class)) {
            Dados.foto("05_cadastro");
            onView(withId(R.id.edtPreco)).perform(Acoes.rolar(), replaceText("1.2.3"), closeSoftKeyboard());
            onView(withId(R.id.btnSalvarLivro)).perform(Acoes.rolar(), click());
            Dados.foto("06_cadastro_erros");

            onView(withId(R.id.edtTitulo)).perform(Acoes.rolar(), replaceText("Novo Testamento de Bolso"), closeSoftKeyboard());
            onView(withId(R.id.edtPreco)).perform(Acoes.rolar(), replaceText("1.234,50"), closeSoftKeyboard());
            onView(withId(R.id.edtQuantidadeEstoque)).perform(Acoes.rolar(), replaceText("4"), closeSoftKeyboard());
            onView(withId(R.id.btnSalvarLivro)).perform(Acoes.rolar(), click());
        }
        LivroEntity novo = null;
        for (LivroEntity l : AppDatabase.getInstance(Dados.contexto()).livroDao().listarTodos()) {
            if (l.titulo.equals("Novo Testamento de Bolso")) novo = l;
        }
        assertEquals(1234.5, novo.preco, 0.001);
        assertEquals(4, AppDatabase.getInstance(Dados.contexto()).estoqueDao().buscarPorLivro(novo.id).quantidadeDisponivel);
    }

    @Test
    public void vendaCompletaComTroco() {
        AppDatabase db = AppDatabase.getInstance(Dados.contexto());
        try (ActivityScenario<RealizarVendaActivity> s = abrir(RealizarVendaActivity.class)) {
            onView(withId(R.id.recyclerProdutos)).perform(Acoes.tocarLivro("Bíblia Sagrada — Nova Almeida Atualizada"));
            onView(withId(R.id.recyclerProdutos)).perform(Acoes.tocarLivro("Bíblia Sagrada — Nova Almeida Atualizada"));
            onView(withId(R.id.recyclerProdutos)).perform(Acoes.tocarLivro("Orações para cada dia"));
            Dados.foto("07_vender");
            // Livro esgotado não entra na venda
            onView(withId(R.id.recyclerProdutos)).perform(Acoes.tocarLivro("Catecismo Menor"));
            assertEquals(3, Carrinho.get().totalUnidades());
            onView(withId(R.id.btnVerCarrinho)).perform(Acoes.tocar());

            onView(withId(R.id.txtValorTotal)).check(matches(withText(containsString("204,80"))));
            onView(withId(R.id.btnConfirmar)).perform(Acoes.rolar(), Acoes.tocar());
            Dados.foto("08_finalizar_sem_pagamento");

            onView(withId(R.id.btnDinheiro)).perform(Acoes.rolar(), Acoes.tocar());
            onView(withId(R.id.edtRecebido)).perform(Acoes.rolar(), replaceText("250"), closeSoftKeyboard());
            onView(withId(R.id.txtTroco)).check(matches(withText(containsString("45,20"))));
            Dados.foto("09_finalizar_dinheiro");

            onView(withId(R.id.btnConfirmar)).perform(Acoes.rolar(), Acoes.tocar());
            Dados.foto("10_confirmar");
            onView(withText("Sim, confirmar")).inRoot(isDialog()).perform(click());
            Dados.foto("11_venda_registrada");
            onView(withId(R.id.btnNovaVenda)).inRoot(isDialog()).perform(click());
        }

        assertEquals(true, Carrinho.get().vazio());
        RelatorioVendas r = RelatorioVendas.calcular(db.vendaDao().listarVendas());
        assertEquals(1, r.vendas.size());
        assertEquals(204.8, r.totalGeral, 0.001);
        assertEquals("Dinheiro", r.vendas.get(0).formaPagamento);
        int bibliaId = -1;
        for (LivroEntity l : db.livroDao().listarTodos()) if (l.titulo.startsWith("Bíblia")) bibliaId = l.id;
        assertEquals(3, db.estoqueDao().buscarPorLivro(bibliaId).quantidadeDisponivel);
    }

    @Test
    public void concluirVendaNoCredito() {
        AppDatabase db = AppDatabase.getInstance(Dados.contexto());
        for (LivroEntity l : db.livroDao().listarTodos()) {
            if (l.titulo.startsWith("Castelo")) Carrinho.get().adicionar(l.id, 2);
        }
        try (ActivityScenario<ConcluirVendaActivity> s = abrir(ConcluirVendaActivity.class)) {
            onView(withId(R.id.btnCredito)).perform(Acoes.rolar(), Acoes.tocar());
            onView(withId(R.id.seletorParcelas)).perform(Acoes.rolar());
            onView(withId(R.id.secaoCredito)).check(matches(isDisplayed()));
            Dados.foto("12_finalizar_credito");
        }
    }

    @Test
    public void relatoriosEPedidosEBackup() {
        AppDatabase db = AppDatabase.getInstance(Dados.contexto());
        java.util.Map<Integer, Integer> itens = new java.util.LinkedHashMap<>();
        for (LivroEntity l : db.livroDao().listarTodos()) {
            if (l.titulo.startsWith("Hinos")) itens.put(l.id, 1);
            if (l.titulo.startsWith("Livro de Culto")) itens.put(l.id, 2);
        }
        com.projeto.livrariadigitaleclb.data.VendaService.registrar(db, itens, "Pix", "À vista");

        try (ActivityScenario<RelatoriosActivity> s = abrir(RelatoriosActivity.class)) {
            onView(allOf(withId(R.id.valorResumo), isDescendantOfA(withId(R.id.resumoTotal))))
                    .check(matches(withText("R$ 165,00")));
            Dados.foto("13_relatorios");
        }
        try (ActivityScenario<PedidosActivity> s = abrir(PedidosActivity.class)) {
            Dados.foto("14_pedidos");
            onView(withId(R.id.btnAdicionarLivro)).perform(click());
            Dados.foto("15_pedidos_adicionar");
            // Salvar sem nome não fecha a janela
            onView(withText("Salvar")).inRoot(isDialog()).perform(click());
            onView(withText("Escreva o nome do livro.")).inRoot(isDialog()).check(matches(isDisplayed()));
        }
        try (ActivityScenario<BackupActivity> s = abrir(BackupActivity.class)) {
            Dados.foto("16_backup");
        }
    }
}
