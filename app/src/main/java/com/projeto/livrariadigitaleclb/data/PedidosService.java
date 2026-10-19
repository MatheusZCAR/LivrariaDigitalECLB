package com.projeto.livrariadigitaleclb.data;

import com.projeto.livrariadigitaleclb.data.local.AppDatabase;
import com.projeto.livrariadigitaleclb.data.local.entity.PedidoEntity;
import com.projeto.livrariadigitaleclb.util.Texto;

/** Regras da lista de pedidos. */
public final class PedidosService {

    private PedidosService() {}

    /**
     * Adiciona o livro à lista. Se ele já estiver lá, soma a quantidade (em vez de repetir a linha).
     * @return a quantidade total do livro na lista
     */
    public static int adicionarOuSomar(AppDatabase db, String titulo, String autor, int quantidade) {
        return db.runInTransaction(() -> {
            String chaveTitulo = Texto.normalizar(titulo);
            String chaveAutor = Texto.normalizar(autor);
            for (PedidoEntity p : db.pedidoDao().getPedidos()) {
                if (Texto.normalizar(p.titulo).equals(chaveTitulo)
                        && Texto.normalizar(p.autor).equals(chaveAutor)) {
                    p.quantidadeDesejada += quantidade;
                    db.pedidoDao().atualizarPedido(p);
                    return p.quantidadeDesejada;
                }
            }
            db.pedidoDao().inserirPedido(new PedidoEntity(titulo.trim(), autor == null ? "" : autor.trim(), quantidade));
            return quantidade;
        });
    }
}
