package com.projeto.livrariadigitaleclb.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Calendar;
import java.util.TimeZone;

public class DatasTest {

    private static final TimeZone SP = TimeZone.getTimeZone("America/Sao_Paulo");

    private static long em(int ano, int mes, int dia, int hora, int minuto) {
        Calendar c = Calendar.getInstance(SP);
        c.clear();
        c.set(ano, mes - 1, dia, hora, minuto, 0);
        return c.getTimeInMillis();
    }

    @Test
    public void escreveEmPortugues() {
        long agora = em(2026, 10, 19, 10, 0);
        assertEquals("hoje às 08:05", Datas.quando(em(2026, 10, 19, 8, 5), agora, SP));
        assertEquals("ontem às 21:30", Datas.quando(em(2026, 10, 18, 21, 30), agora, SP));
        assertEquals("quinta-feira, 15/10 às 16:00", Datas.quando(em(2026, 10, 15, 16, 0), agora, SP));
        assertEquals("01/09/2026 às 16:00", Datas.quando(em(2026, 9, 1, 16, 0), agora, SP));
    }

    @Test
    public void diasPeloCalendario() {
        long agora = em(2026, 10, 19, 0, 30);
        assertEquals(0, Datas.diasDesde(em(2026, 10, 19, 0, 1), agora, SP));
        assertEquals(1, Datas.diasDesde(em(2026, 10, 18, 23, 59), agora, SP));
        assertEquals(4, Datas.diasDesde(em(2026, 10, 15, 12, 0), agora, SP));
    }
}
