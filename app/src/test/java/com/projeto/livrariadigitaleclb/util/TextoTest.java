package com.projeto.livrariadigitaleclb.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TextoTest {

    @Test
    public void normalizaAcentosEMaiusculas() {
        assertEquals("joao paulo", Texto.normalizar("  João   Paulo "));
        assertEquals("oracao", Texto.normalizar("ORAÇÃO"));
        assertEquals("", Texto.normalizar(null));
    }

    @Test
    public void buscaIgnoraAcentoEOrdemDasPalavras() {
        assertTrue(Texto.combina("joao", "Evangelho de João", "Autor"));
        assertTrue(Texto.combina("luz caminho", "O Caminho da Luz", null));
        assertTrue(Texto.combina("lutero", "Catecismo", "Martinho Lutero"));
        assertTrue(Texto.combina("", "Qualquer"));
        assertFalse(Texto.combina("biblia", "Hinário", "IECLB"));
    }

    @Test
    public void inteiroValidaEntrada() {
        assertEquals(Integer.valueOf(5), Texto.inteiro(" 5 "));
        assertEquals(Integer.valueOf(0), Texto.inteiro("0"));
        assertNull(Texto.inteiro(""));
        assertNull(Texto.inteiro("-1"));
        assertNull(Texto.inteiro("12345678901")); // antes estourava o int e fechava o app
        assertNull(Texto.inteiro("3,5"));
    }

    @Test
    public void plural() {
        assertEquals("1 livro", Texto.plural(1, "livro", "livros"));
        assertEquals("0 livros", Texto.plural(0, "livro", "livros"));
        assertEquals("3 livros", Texto.plural(3, "livro", "livros"));
    }
}
