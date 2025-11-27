package com.projeto.livrariadigitaleclb.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.projeto.livrariadigitaleclb.data.local.entity.LocalizacaoEntity;

import java.util.List;

@Dao
public interface LocalizacaoDao {

    @Insert
    long inserirLocalizacao(LocalizacaoEntity localizacao);

    @Query("SELECT * FROM localizacoes ORDER BY descricao ASC")
    List<LocalizacaoEntity> listarLocalizacoes();
}
