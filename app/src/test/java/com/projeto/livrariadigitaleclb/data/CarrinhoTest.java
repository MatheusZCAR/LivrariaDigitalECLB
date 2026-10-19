package com.projeto.livrariadigitaleclb.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CarrinhoTest {

    @Test
    public void adicionaDefineERemove() {
        Carrinho c = Carrinho.novo();
        assertTrue(c.vazio());
        c.adicionar(7, 1);
        c.adicionar(7, 1);
        c.adicionar(3, 1);
        assertEquals(2, c.quantidade(7));
        assertEquals(3, c.totalUnidades());

        c.definir(7, 0);
        assertEquals(0, c.quantidade(7));
        assertFalse(c.itens().containsKey(7));

        c.limpar();
        assertTrue(c.vazio());
    }

    @Test
    public void mantemAOrdemEmQueOsLivrosForamEscolhidos() {
        Carrinho c = Carrinho.novo();
        c.adicionar(5, 1);
        c.adicionar(1, 1);
        c.adicionar(9, 1);
        assertEquals("[5, 1, 9]", c.itens().keySet().toString());
    }
}
