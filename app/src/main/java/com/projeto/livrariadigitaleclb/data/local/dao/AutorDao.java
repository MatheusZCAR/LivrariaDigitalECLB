package com.projeto.livrariadigitaleclb.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.projeto.livrariadigitaleclb.data.local.entity.AutorEntity;

import java.util.List;

@Dao
public interface AutorDao {

    @Insert
    long inserirAutor(AutorEntity autor);

    @Query("SELECT * FROM autores ORDER BY nome ASC")
    List<AutorEntity> listarAutores();
}
