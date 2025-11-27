package com.projeto.livrariadigitaleclb.data.local.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "pedidos")
public class PedidoEntity {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String titulo;
    public String autor;

    public PedidoEntity(String titulo, String autor) {
        this.titulo = titulo;
        this.autor = autor;
    }
}
