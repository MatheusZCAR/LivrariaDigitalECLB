package com.projeto.livrariadigitaleclb.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.projeto.livrariadigitaleclb.data.local.entity.EstoqueEntity;

import java.util.List;

@Dao
public interface EstoqueDao {

    @Insert
    void inserirEstoque(EstoqueEntity estoque);

    @Update
    void atualizarEstoque(EstoqueEntity estoque);

    @Query("SELECT * FROM estoque WHERE livroId = :livroId LIMIT 1")
    EstoqueEntity buscarPorLivro(int livroId);

    @Query("SELECT * FROM estoque WHERE quantidadeDisponivel <= 0")
    List<EstoqueEntity> listarFaltando();

    @Query("UPDATE estoque SET quantidadeDisponivel = quantidadeDisponivel - :qtd WHERE livroId = :livroId AND quantidadeDisponivel >= :qtd")
    void baixarEstoque(int livroId, int qtd);
}
