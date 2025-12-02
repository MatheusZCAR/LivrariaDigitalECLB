package com.projeto.livrariadigitaleclb.data.local.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "pedidos")
public class PedidoEntity {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String titulo;
    public String autor;
    public int quantidadeDesejada;

    public PedidoEntity(String titulo, String autor, int quantidadeDesejada) {
        this.titulo = titulo;
        this.autor = autor;
        this.quantidadeDesejada = quantidadeDesejada;
    }
}
