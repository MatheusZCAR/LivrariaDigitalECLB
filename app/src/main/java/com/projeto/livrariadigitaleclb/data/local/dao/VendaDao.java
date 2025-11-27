package com.projeto.livrariadigitaleclb.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.projeto.livrariadigitaleclb.data.local.entity.Venda;

import java.util.List;

@Dao
public interface VendaDao {

    @Insert
    long registrarVenda(Venda venda);

    @Query("SELECT * FROM vendas ORDER BY dataVenda DESC")
    List<Venda> listarVendas();
}
