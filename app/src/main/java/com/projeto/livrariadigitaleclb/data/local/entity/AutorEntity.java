package com.projeto.livrariadigitaleclb.data.local.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "autores")
public class AutorEntity {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String nome;
}
