package com.projeto.livrariadigitaleclb.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.projeto.livrariadigitaleclb.data.local.entity.ItemVendaEntity;

import java.util.List;

@Dao
public interface ItemVendaDao {

    @Insert
    void inserirItemVenda(ItemVendaEntity item);

    @Query("SELECT * FROM itens_venda WHERE vendaId = :vendaId")
    List<ItemVendaEntity> listarItensPorVenda(int vendaId);
}
