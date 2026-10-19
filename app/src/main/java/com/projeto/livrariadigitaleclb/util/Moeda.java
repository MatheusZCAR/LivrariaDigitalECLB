package com.projeto.livrariadigitaleclb.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** Formata e interpreta valores em reais de forma tolerante ao jeito que a pessoa digitar. */
public final class Moeda {

    private Moeda() {}

    private static DecimalFormat formato() {
        DecimalFormatSymbols simbolos = new DecimalFormatSymbols(new Locale("pt", "BR"));
        simbolos.setDecimalSeparator(',');
        simbolos.setGroupingSeparator('.');
        DecimalFormat df = new DecimalFormat("#,##0.00", simbolos);
        df.setRoundingMode(RoundingMode.HALF_UP);
        return df;
    }

    /** 1234.5 → "R$ 1.234,50" */
    public static String formatar(double valor) {
        String texto = formato().format(Math.abs(arredondar(valor)));
        return (valor < -0.004 ? "- R$ " : "R$ ") + texto;
    }

    /** 1234.5 → "1.234,50" (para colocar dentro de um campo de texto). */
    public static String formatarSemSimbolo(double valor) {
        return formato().format(arredondar(valor));
    }

    public static double arredondar(double valor) {
        return BigDecimal.valueOf(valor).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    /**
     * Interpreta o que a pessoa digitou: "25", "25,5", "25.50", "1.234,56", "R$ 10,00"...
     * @return o valor, ou null se não for um número válido (ou for negativo).
     */
    public static Double interpretar(String texto) {
        if (texto == null) return null;
        String s = texto.replace("R$", "").replace(" ", "").replace(" ", "").trim();
        if (s.isEmpty()) return null;
        if (!s.matches("[0-9.,]+")) return null;

        int ultimaVirgula = s.lastIndexOf(',');
        int ultimoPonto = s.lastIndexOf('.');
        String normalizado;

        if (ultimaVirgula >= 0 && ultimoPonto >= 0) {
            // Os dois separadores: o último que aparece é o decimal.
            if (ultimaVirgula > ultimoPonto) {
                if (!milharValido(s.substring(0, ultimaVirgula), '.')) return null;
                normalizado = s.replace(".", "").replace(',', '.');
            } else {
                if (!milharValido(s.substring(0, ultimoPonto), ',')) return null;
                normalizado = s.replace(",", "");
            }
        } else if (ultimaVirgula >= 0) {
            if (s.indexOf(',') != ultimaVirgula) {
                // "1,234,567" → separador de milhar
                if (!milharValido(s, ',')) return null;
                normalizado = s.replace(",", "");
            } else {
                normalizado = s.replace(',', '.');
            }
        } else if (ultimoPonto >= 0) {
            int digitosDepois = s.length() - ultimoPonto - 1;
            if (s.indexOf('.') != ultimoPonto || digitosDepois == 3) {
                // "1.234" ou "1.234.567" → milhar (jeito brasileiro)
                if (!milharValido(s, '.')) return null;
                normalizado = s.replace(".", "");
            } else {
                normalizado = s;
            }
        } else {
            normalizado = s;
        }

        if (normalizado.isEmpty() || normalizado.equals(".") || normalizado.indexOf('.') != normalizado.lastIndexOf('.')) {
            return null;
        }
        try {
            double valor = new BigDecimal(normalizado).setScale(2, RoundingMode.HALF_UP).doubleValue();
            if (valor < 0 || valor > 1_000_000_000d) return null;
            return valor;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** "1.234.567" é um número com separador de milhar válido; "1.2.3" não é. */
    private static boolean milharValido(String parteInteira, char separador) {
        if (parteInteira.indexOf(separador) < 0) return parteInteira.matches("[0-9]*");
        String sep = separador == '.' ? "\\." : ",";
        return parteInteira.matches("[0-9]{1,3}(" + sep + "[0-9]{3})+");
    }
}
