package com.projeto.livrariadigitaleclb.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.projeto.livrariadigitaleclb.data.local.entity.LivroEntity;

import java.util.List;

@Dao
public interface LivroDao {

    @Query("SELECT * FROM livros WHERE esgotado = 0 ORDER BY titulo ASC")
    List<LivroEntity> listarLivrosParaVenda();

    @Query("SELECT * FROM livros " +
            "WHERE esgotado = 0 AND (" +
            "LOWER(titulo) LIKE '%' || LOWER(:busca) || '%' " +
            "OR LOWER(autor) LIKE '%' || LOWER(:busca) || '%'" +
            ") ORDER BY titulo ASC")
    List<LivroEntity> buscarLivros(String busca);

    @Query("SELECT titulo FROM livros WHERE esgotado = 0 ORDER BY titulo ASC")
    List<String> listarTitulos();

    @Query("SELECT * FROM livros WHERE id = :id LIMIT 1")
    LivroEntity getLivroById(int id);

    @Insert
    long inserirLivro(LivroEntity livro);

    @Query("SELECT * FROM livros WHERE codigoBarras = :codigo LIMIT 1")
    LivroEntity buscarPorCodigo(String codigo);
}
