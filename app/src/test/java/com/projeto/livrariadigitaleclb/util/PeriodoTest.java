package com.projeto.livrariadigitaleclb.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Calendar;
import java.util.TimeZone;

public class PeriodoTest {

    private static final TimeZone SP = TimeZone.getTimeZone("America/Sao_Paulo");

    private static long em(int ano, int mes, int dia, int hora, int minuto) {
        Calendar c = Calendar.getInstance(SP);
        c.clear();
        c.set(ano, mes - 1, dia, hora, minuto, 0);
        return c.getTimeInMillis();
    }

    @Test
    public void hojeVaiDaMeiaNoiteAteAMeiaNoite() {
        // Antes, "diário" eram as últimas 24h: às 9h de domingo incluía vendas do sábado.
        long agora = em(2026, 10, 18, 9, 0);
        Periodo p = Periodo.hoje(agora, SP);
        assertEquals(em(2026, 10, 18, 0, 0), p.inicio);
        assertEquals(em(2026, 10, 19, 0, 0), p.fim);
        assertFalse(p.contem(em(2026, 10, 17, 20, 0)));
        assertTrue(p.contem(em(2026, 10, 18, 23, 59)));
    }

    @Test
    public void semanaComecaNoDomingo() {
        long quarta = em(2026, 10, 21, 15, 0);
        Periodo p = Periodo.semana(quarta, SP);
        assertEquals(em(2026, 10, 18, 0, 0), p.inicio);
        assertEquals(em(2026, 10, 25, 0, 0), p.fim);

        long domingo = em(2026, 10, 18, 10, 0);
        assertEquals(em(2026, 10, 18, 0, 0), Periodo.semana(domingo, SP).inicio);
    }

    @Test
    public void mesCalendario() {
        Periodo p = Periodo.mes(em(2026, 2, 15, 12, 0), SP);
        assertEquals(em(2026, 2, 1, 0, 0), p.inicio);
        assertEquals(em(2026, 3, 1, 0, 0), p.fim);
    }

    @Test
    public void periodoEscolhidoIncluiOUltimoDia() {
        Periodo p = Periodo.entre(em(2026, 10, 25, 8, 0), em(2026, 10, 1, 8, 0), SP);
        assertEquals(em(2026, 10, 1, 0, 0), p.inicio);
        assertEquals(em(2026, 10, 26, 0, 0), p.fim);
        assertEquals("De 01/10/2026 a 25/10/2026", p.descricao(SP));
        assertEquals("Dia 01/10/2026", Periodo.entre(em(2026, 10, 1, 9, 0), em(2026, 10, 1, 9, 0), SP).descricao(SP));
    }

    @Test
    public void descricoes() {
        assertEquals("Outubro de 2026", Periodo.mes(em(2026, 10, 5, 0, 0), SP).descricao(SP));
        assertEquals("Semana de 18/10 a 24/10/2026", Periodo.semana(em(2026, 10, 20, 0, 0), SP).descricao(SP));
    }
}
