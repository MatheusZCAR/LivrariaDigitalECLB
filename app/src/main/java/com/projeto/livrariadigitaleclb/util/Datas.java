package com.projeto.livrariadigitaleclb.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/** Datas escritas do jeito que a gente fala, sempre em português (independe do idioma do tablet). */
public final class Datas {

    private Datas() {}

    /** "hoje às 14:32", "ontem às 09:10", "segunda-feira, 12/10 às 16:00" ou "12/10/2025 às 16:00". */
    public static String quando(long instante, long agora, TimeZone fuso) {
        SimpleDateFormat hora = formato("HH:mm", fuso);
        long hoje = Periodo.hoje(agora, fuso).inicio;
        long umDia = 24L * 60 * 60 * 1000;
        if (instante >= hoje && instante < hoje + umDia) return "hoje às " + hora.format(new Date(instante));
        if (instante >= hoje - umDia && instante < hoje) return "ontem às " + hora.format(new Date(instante));
        if (instante >= hoje - 6 * umDia && instante < hoje) {
            return formato("EEEE, dd/MM 'às' HH:mm", fuso).format(new Date(instante));
        }
        return formato("dd/MM/yyyy 'às' HH:mm", fuso).format(new Date(instante));
    }

    public static String quando(long instante) {
        return quando(instante, System.currentTimeMillis(), TimeZone.getDefault());
    }

    /** Quantos dias inteiros (pelo calendário) se passaram. */
    public static int diasDesde(long instante, long agora, TimeZone fuso) {
        Calendar a = Calendar.getInstance(fuso);
        a.setTimeInMillis(Periodo.hoje(instante, fuso).inicio);
        Calendar b = Calendar.getInstance(fuso);
        b.setTimeInMillis(Periodo.hoje(agora, fuso).inicio);
        int dias = 0;
        while (a.before(b) && dias < 10000) {
            a.add(Calendar.DAY_OF_MONTH, 1);
            dias++;
        }
        return dias;
    }

    private static SimpleDateFormat formato(String padrao, TimeZone fuso) {
        SimpleDateFormat f = new SimpleDateFormat(padrao, Periodo.BR);
        f.setTimeZone(fuso);
        return f;
    }
}
