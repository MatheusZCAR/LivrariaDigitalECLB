package com.projeto.livrariadigitaleclb.data;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Carrinho da venda em andamento, compartilhado entre a tela "Vender" e "Finalizar venda".
 * Antes cada tela tinha a sua cópia e as alterações feitas numa não apareciam na outra.
 */
public final class Carrinho {

    private static final Carrinho INSTANCIA = new Carrinho();

    /** livroId → quantidade, na ordem em que foram adicionados. */
    private final LinkedHashMap<Integer, Integer> itens = new LinkedHashMap<>();

    public static Carrinho get() {
        return INSTANCIA;
    }

    /** Para testes. */
    public static Carrinho novo() {
        return new Carrinho();
    }

    public synchronized int quantidade(int livroId) {
        Integer q = itens.get(livroId);
        return q == null ? 0 : q;
    }

    /** Define a quantidade; 0 ou menos remove o livro. */
    public synchronized void definir(int livroId, int quantidade) {
        if (quantidade <= 0) {
            itens.remove(livroId);
        } else {
            itens.put(livroId, quantidade);
        }
    }

    public synchronized void adicionar(int livroId, int quantidade) {
        definir(livroId, quantidade(livroId) + quantidade);
    }

    public synchronized void remover(int livroId) {
        itens.remove(livroId);
    }

    public synchronized Map<Integer, Integer> itens() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(itens));
    }

    public synchronized boolean vazio() {
        return itens.isEmpty();
    }

    public synchronized int totalUnidades() {
        int total = 0;
        for (int q : itens.values()) total += q;
        return total;
    }

    public synchronized void limpar() {
        itens.clear();
    }
}
