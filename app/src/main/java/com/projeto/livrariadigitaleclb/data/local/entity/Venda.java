package com.projeto.livrariadigitaleclb.data.local.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "vendas")
public class Venda {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public long livroId;  // Mudei de int para long para compatibilidade
    public String titulo;
    public double preco;

    public long dataVenda;  // Timestamp em millisegundos
}