package com.projeto.livrariadigitaleclb.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class CapasTest {

    @Test
    public void reduzFotosGrandesAntesDeCarregar() {
        assertEquals(4, Capas.calcularReducao(4000, 3000, 1000));
        assertEquals(1, Capas.calcularReducao(800, 600, 1000));
        assertEquals(2, Capas.calcularReducao(2000, 2000, 1000));
    }
}
