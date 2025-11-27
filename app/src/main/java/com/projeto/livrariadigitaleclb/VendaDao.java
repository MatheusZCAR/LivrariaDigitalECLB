package com.projeto.livrariadigitaleclb;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface VendaDao {

    @Insert
    long registrarVenda(Venda venda);

    @Query("SELECT * FROM vendas ORDER BY dataVenda DESC")
    List<Venda> listarVendas();
}
