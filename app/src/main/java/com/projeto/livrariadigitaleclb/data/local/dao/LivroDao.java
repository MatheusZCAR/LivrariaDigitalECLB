package com.projeto.livrariadigitaleclb.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import com.projeto.livrariadigitaleclb.data.local.entity.LivroComEstoque;
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

    @Query("SELECT * FROM livros WHERE id = :id LIMIT 1")
    LivroEntity buscarPorId(long id);

    @Query("SELECT * FROM livros WHERE codigoBarras = :codigo LIMIT 1")
    LivroEntity buscarPorCodigo(String codigo);

    @Insert
    long inserirLivro(LivroEntity livro);

    @Update
    void atualizarLivro(LivroEntity livro);

    @Query("SELECT L.*, E.quantidadeDisponivel as qtd " +
            "FROM livros L " +
            "LEFT JOIN estoque E ON L.id = E.livroId " +
            "ORDER BY L.titulo ASC")
    List<LivroComEstoque> listarLivrosComEstoque();


    @Query("SELECT L.*, E.quantidadeDisponivel as qtd " +
            "FROM livros L " +
            "LEFT JOIN estoque E ON L.id = E.livroId " +
            "WHERE L.esgotado = 0 " +
            "ORDER BY L.titulo ASC")
    List<LivroComEstoque> listarLivrosParaVendaComEstoque();

    @Query("SELECT L.*, E.quantidadeDisponivel as qtd " +
            "FROM livros L " +
            "LEFT JOIN estoque E ON L.id = E.livroId " +
            "WHERE L.esgotado = 0 AND (" +
            "LOWER(L.titulo) LIKE '%' || LOWER(:busca) || '%' " +
            "OR LOWER(L.autor) LIKE '%' || LOWER(:busca) || '%'" +
            ") ORDER BY L.titulo ASC")
    List<LivroComEstoque> buscarLivrosComEstoque(String busca);
}
