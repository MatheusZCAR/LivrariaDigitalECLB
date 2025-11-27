package com.projeto.livrariadigitaleclb.data.local.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.projeto.livrariadigitaleclb.data.local.entity.PedidoEntity;

import java.util.List;

@Dao
public interface PedidoDao {

    @Insert
    void inserirPedido(PedidoEntity pedido);

    @Query("SELECT * FROM pedidos")
    List<PedidoEntity> getPedidos();

    @Query("DELETE FROM pedidos")
    void deletarTodos();

    @Update
    void atualizarPedido(PedidoEntity pedido);


    @Delete
    void excluirPedido(PedidoEntity pedido);


}
