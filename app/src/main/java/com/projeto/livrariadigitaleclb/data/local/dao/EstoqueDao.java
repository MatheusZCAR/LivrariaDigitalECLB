package com.projeto.livrariadigitaleclb.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.projeto.livrariadigitaleclb.data.local.entity.EstoqueEntity;

import java.util.List;

@Dao
public interface EstoqueDao {

    @Insert
    void inserirEstoque(EstoqueEntity estoque);

    /** Insere ou substitui o estoque do livro. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void salvarEstoque(EstoqueEntity estoque);

    @Update
    void atualizarEstoque(EstoqueEntity estoque);

    @Query("SELECT * FROM estoque WHERE livroId = :livroId LIMIT 1")
    EstoqueEntity buscarPorLivro(int livroId);

    @Query("SELECT * FROM estoque WHERE quantidadeDisponivel <= 0")
    List<EstoqueEntity> listarFaltando();

    /**
     * Baixa o estoque somente se houver quantidade suficiente.
     * @return número de linhas alteradas (0 = estoque insuficiente ou inexistente)
     */
    @Query("UPDATE estoque SET quantidadeDisponivel = quantidadeDisponivel - :qtd, dataAtualizacao = :agora " +
            "WHERE livroId = :livroId AND quantidadeDisponivel >= :qtd")
    int baixarEstoque(int livroId, int qtd, long agora);

    /** Devolve exemplares ao estoque (ex.: venda cancelada). */
    @Query("UPDATE estoque SET quantidadeDisponivel = quantidadeDisponivel + :qtd, dataAtualizacao = :agora " +
            "WHERE livroId = :livroId")
    int devolverEstoque(int livroId, int qtd, long agora);

    @Query("DELETE FROM estoque WHERE livroId = :livroId")
    void excluirDoLivro(int livroId);
}
