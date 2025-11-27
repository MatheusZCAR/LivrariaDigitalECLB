package com.projeto.livrariadigitaleclb.data.local.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "localizacoes")
public class LocalizacaoEntity {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String descricao;
}
