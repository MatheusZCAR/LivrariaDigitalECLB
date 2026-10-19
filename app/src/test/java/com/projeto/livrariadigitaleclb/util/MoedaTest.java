package com.projeto.livrariadigitaleclb.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class MoedaTest {

    @Test
    public void formataNoPadraoBrasileiro() {
        assertEquals("R$ 25,00", Moeda.formatar(25));
        assertEquals("R$ 1.234,50", Moeda.formatar(1234.5));
        assertEquals("R$ 0,10", Moeda.formatar(0.1));
        assertEquals("R$ 0,00", Moeda.formatar(0));
        assertEquals("- R$ 5,00", Moeda.formatar(-5));
    }

    @Test
    public void formataSemSimbolo() {
        assertEquals("25,90", Moeda.formatarSemSimbolo(25.9));
        assertEquals("1.000,00", Moeda.formatarSemSimbolo(1000));
    }

    @Test
    public void interpretaJeitosComunsDeDigitar() {
        assertEquals(25.0, Moeda.interpretar("25"), 0.001);
        assertEquals(25.5, Moeda.interpretar("25,5"), 0.001);
        assertEquals(25.9, Moeda.interpretar("25,90"), 0.001);
        assertEquals(25.9, Moeda.interpretar("25.90"), 0.001);
        assertEquals(1234.56, Moeda.interpretar("1.234,56"), 0.001);
        assertEquals(1234.56, Moeda.interpretar("1,234.56"), 0.001);
        assertEquals(1234.0, Moeda.interpretar("1.234"), 0.001);
        assertEquals(10.0, Moeda.interpretar("R$ 10,00"), 0.001);
        assertEquals(0.0, Moeda.interpretar("0"), 0.001);
        assertEquals(0.5, Moeda.interpretar(",5"), 0.001);
    }

    @Test
    public void recusaTextoInvalido() {
        // Antes, "1.234,56" fechava o app (NumberFormatException).
        assertNull(Moeda.interpretar(""));
        assertNull(Moeda.interpretar(null));
        assertNull(Moeda.interpretar("abc"));
        assertNull(Moeda.interpretar("-5"));
        assertNull(Moeda.interpretar("."));
        assertNull(Moeda.interpretar("1,2,3,4"));
    }

    @Test
    public void arredondaCentavos() {
        assertEquals(0.3, Moeda.arredondar(0.1 + 0.2), 0);
        assertEquals(10.01, Moeda.arredondar(10.005), 0);
    }
}
