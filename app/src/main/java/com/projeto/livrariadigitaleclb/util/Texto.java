package com.projeto.livrariadigitaleclb.util;

import java.text.Normalizer;
import java.util.Locale;

/** Ajudas para comparar textos sem se importar com acentos e maiúsculas. */
public final class Texto {

    private Texto() {}

    /** "João  Paulo" → "joao paulo" */
    public static String normalizar(String s) {
        if (s == null) return "";
        String semAcento = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return semAcento.toLowerCase(Locale.ROOT).trim().replaceAll("\\s+", " ");
    }

    /** Verdadeiro se todas as palavras da busca aparecerem em algum dos campos. */
    public static boolean combina(String busca, String... campos) {
        String b = normalizar(busca);
        if (b.isEmpty()) return true;
        StringBuilder tudo = new StringBuilder();
        for (String c : campos) tudo.append(' ').append(normalizar(c));
        String alvo = tudo.toString();
        for (String palavra : b.split(" ")) {
            if (!alvo.contains(palavra)) return false;
        }
        return true;
    }

    public static boolean vazio(String s) {
        return s == null || s.trim().isEmpty();
    }

    /** Interpreta um número inteiro digitado; devolve null se inválido. */
    public static Integer inteiro(String s) {
        if (s == null) return null;
        String t = s.trim();
        if (t.isEmpty() || !t.matches("[0-9]{1,6}")) return null;
        return Integer.parseInt(t);
    }

    /** "1 livro" / "3 livros" */
    public static String plural(int n, String singular, String plural) {
        return n + " " + (n == 1 ? singular : plural);
    }
}
