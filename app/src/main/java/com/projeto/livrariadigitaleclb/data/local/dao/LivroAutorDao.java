package com.projeto.livrariadigitaleclb.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.projeto.livrariadigitaleclb.data.local.entity.LivroAutorEntity;

import java.util.List;

@Dao
public interface LivroAutorDao {

    @Insert
    void inserirLivroAutor(LivroAutorEntity relacao);

    @Query("SELECT * FROM livro_autor WHERE livroId = :livroId")
    List<LivroAutorEntity> listarPorLivro(int livroId);

    @Query("SELECT * FROM livro_autor WHERE autorId = :autorId")
    List<LivroAutorEntity> listarPorAutor(int autorId);
}
