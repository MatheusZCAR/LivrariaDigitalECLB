package com.projeto.livrariadigitaleclb.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.projeto.livrariadigitaleclb.data.local.entity.RelatorioVendaEntity;

import java.util.List;

@Dao
public interface RelatorioVendaDao {

    @Insert
    void inserirRelatorioVenda(RelatorioVendaEntity relatorioVenda);

    @Query("SELECT * FROM relatorio_venda WHERE relatorioId = :relatorioId")
    List<RelatorioVendaEntity> listarPorRelatorio(int relatorioId);
}
