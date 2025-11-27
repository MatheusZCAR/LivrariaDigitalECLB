package com.projeto.livrariadigitaleclb.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.projeto.livrariadigitaleclb.data.local.entity.RelatorioEntity;

import java.util.List;

@Dao
public interface RelatorioDao {

    @Insert
    long inserirRelatorio(RelatorioEntity relatorio);

    @Query("SELECT * FROM relatorios ORDER BY id DESC")
    List<RelatorioEntity> listarRelatorios();
}
