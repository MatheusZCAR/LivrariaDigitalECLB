package com.projeto.livrariadigitaleclb.data;

import com.projeto.livrariadigitaleclb.data.local.AppDatabase;
import com.projeto.livrariadigitaleclb.data.local.dao.EstoqueDao;
import com.projeto.livrariadigitaleclb.data.local.dao.LivroDao;
import com.projeto.livrariadigitaleclb.data.local.dao.VendaDao;
import com.projeto.livrariadigitaleclb.data.local.entity.EstoqueEntity;
import com.projeto.livrariadigitaleclb.data.local.entity.LivroEntity;
import com.projeto.livrariadigitaleclb.data.local.entity.Venda;

import java.util.List;
import java.util.Map;

/**
 * Regras de venda. Tudo acontece dentro de uma transação: ou a venda inteira é gravada
 * (com baixa no estoque), ou nada é gravado.
 */
public final class VendaService {

    private VendaService() {}

    public static class EstoqueInsuficienteException extends RuntimeException {
        public final String titulo;
        public final int disponivel;

        public EstoqueInsuficienteException(String titulo, int disponivel) {
            super("Estoque insuficiente para \"" + titulo + "\" (disponível: " + disponivel + ")");
            this.titulo = titulo;
            this.disponivel = disponivel;
        }
    }

    /**
     * Registra a venda e baixa o estoque.
     * @param itens livroId → quantidade
     * @return o código da venda (usado para cancelar depois)
     */
    public static long registrar(AppDatabase db, Map<Integer, Integer> itens,
                                 String formaPagamento, String condicaoPagamento) {
        if (itens.isEmpty()) throw new IllegalArgumentException("Venda sem itens");

        return db.runInTransaction(() -> {
            LivroDao livroDao = db.livroDao();
            EstoqueDao estoqueDao = db.estoqueDao();
            VendaDao vendaDao = db.vendaDao();

            long agora = System.currentTimeMillis();
            for (Map.Entry<Integer, Integer> item : itens.entrySet()) {
                int livroId = item.getKey();
                int quantidade = item.getValue();
                if (quantidade <= 0) continue;

                LivroEntity livro = livroDao.getLivroById(livroId);
                if (livro == null) {
                    throw new EstoqueInsuficienteException("livro removido do catálogo", 0);
                }
                int alterados = estoqueDao.baixarEstoque(livroId, quantidade, agora);
                if (alterados == 0) {
                    EstoqueEntity e = estoqueDao.buscarPorLivro(livroId);
                    throw new EstoqueInsuficienteException(livro.titulo,
                            e == null ? 0 : e.quantidadeDisponivel);
                }

                Venda venda = new Venda();
                venda.livroId = livroId;
                venda.titulo = livro.titulo;
                venda.preco = livro.preco;
                venda.quantidade = quantidade;
                venda.dataVenda = agora;
                venda.codigoVenda = agora;
                venda.formaPagamento = formaPagamento;
                venda.condicaoPagamento = condicaoPagamento;
                vendaDao.registrarVenda(venda);
            }
            return agora;
        });
    }

    /** Cancela uma venda inteira e devolve os livros ao estoque. */
    public static void cancelar(AppDatabase db, long codigoVenda) {
        db.runInTransaction(() -> {
            long agora = System.currentTimeMillis();
            List<Venda> linhas = db.vendaDao().listarPorCodigo(codigoVenda);
            for (Venda v : linhas) {
                int livroId = (int) v.livroId;
                if (db.livroDao().getLivroById(livroId) == null) continue; // livro foi excluído
                int alterados = db.estoqueDao().devolverEstoque(livroId, v.quantidade, agora);
                if (alterados == 0) {
                    EstoqueEntity novo = new EstoqueEntity();
                    novo.livroId = livroId;
                    novo.quantidadeDisponivel = v.quantidade;
                    novo.dataAtualizacao = agora;
                    db.estoqueDao().salvarEstoque(novo);
                }
            }
            db.vendaDao().excluirPorCodigo(codigoVenda);
        });
    }
}
