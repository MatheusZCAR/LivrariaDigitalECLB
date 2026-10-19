package com.projeto.livrariadigitaleclb.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/** Intervalo de datas [inicio, fim) usado nos relatórios. */
public final class Periodo {

    public enum Tipo { HOJE, SEMANA, MES, PERSONALIZADO }

    public static final Locale BR = new Locale("pt", "BR");

    public final Tipo tipo;
    public final long inicio;
    public final long fim;

    private Periodo(Tipo tipo, long inicio, long fim) {
        this.tipo = tipo;
        this.inicio = inicio;
        this.fim = fim;
    }

    public static Periodo hoje(long agora, TimeZone fuso) {
        Calendar c = inicioDoDia(agora, fuso);
        long inicio = c.getTimeInMillis();
        c.add(Calendar.DAY_OF_MONTH, 1);
        return new Periodo(Tipo.HOJE, inicio, c.getTimeInMillis());
    }

    /** Semana de domingo a sábado (como no calendário brasileiro). */
    public static Periodo semana(long agora, TimeZone fuso) {
        Calendar c = inicioDoDia(agora, fuso);
        int diaDaSemana = c.get(Calendar.DAY_OF_WEEK); // domingo = 1
        c.add(Calendar.DAY_OF_MONTH, -(diaDaSemana - Calendar.SUNDAY));
        long inicio = c.getTimeInMillis();
        c.add(Calendar.DAY_OF_MONTH, 7);
        return new Periodo(Tipo.SEMANA, inicio, c.getTimeInMillis());
    }

    public static Periodo mes(long agora, TimeZone fuso) {
        Calendar c = inicioDoDia(agora, fuso);
        c.set(Calendar.DAY_OF_MONTH, 1);
        long inicio = c.getTimeInMillis();
        c.add(Calendar.MONTH, 1);
        return new Periodo(Tipo.MES, inicio, c.getTimeInMillis());
    }

    /**
     * Do primeiro dia (inclusive) até o último dia (inclusive).
     * Os parâmetros podem ser qualquer horário dentro de cada dia.
     */
    public static Periodo entre(long primeiroDia, long ultimoDia, TimeZone fuso) {
        if (ultimoDia < primeiroDia) {
            long t = primeiroDia; primeiroDia = ultimoDia; ultimoDia = t;
        }
        long inicio = inicioDoDia(primeiroDia, fuso).getTimeInMillis();
        Calendar c = inicioDoDia(ultimoDia, fuso);
        c.add(Calendar.DAY_OF_MONTH, 1);
        return new Periodo(Tipo.PERSONALIZADO, inicio, c.getTimeInMillis());
    }

    public boolean contem(long instante) {
        return instante >= inicio && instante < fim;
    }

    /** Texto para mostrar na tela e no PDF. */
    public String descricao(TimeZone fuso) {
        SimpleDateFormat dia = formato("dd/MM/yyyy", fuso);
        switch (tipo) {
            case HOJE:
                return "Hoje, " + formato("EEEE, dd/MM/yyyy", fuso).format(inicio);
            case SEMANA:
                return "Semana de " + formato("dd/MM", fuso).format(inicio)
                        + " a " + dia.format(fim - 1);
            case MES:
                String mes = formato("MMMM 'de' yyyy", fuso).format(inicio);
                return Character.toUpperCase(mes.charAt(0)) + mes.substring(1);
            case PERSONALIZADO:
            default:
                String a = dia.format(inicio);
                String b = dia.format(fim - 1);
                return a.equals(b) ? "Dia " + a : "De " + a + " a " + b;
        }
    }

    private static SimpleDateFormat formato(String padrao, TimeZone fuso) {
        SimpleDateFormat f = new SimpleDateFormat(padrao, BR);
        f.setTimeZone(fuso);
        return f;
    }

    private static Calendar inicioDoDia(long instante, TimeZone fuso) {
        Calendar c = Calendar.getInstance(fuso, BR);
        c.setTimeInMillis(instante);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c;
    }
}
