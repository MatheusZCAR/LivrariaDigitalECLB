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

    /** Vendas com dataVenda dentro de [inicio, fim). */
    @Query("SELECT * FROM vendas WHERE dataVenda >= :inicio AND dataVenda < :fim ORDER BY dataVenda DESC, id ASC")
    List<Venda> listarPorPeriodo(long inicio, long fim);

    @Query("SELECT * FROM vendas WHERE codigoVenda = :codigo")
    List<Venda> listarPorCodigo(long codigo);

    @Query("DELETE FROM vendas WHERE codigoVenda = :codigo")
    int excluirPorCodigo(long codigo);

    @Query("SELECT COUNT(*) FROM vendas")
    int contar();
}
