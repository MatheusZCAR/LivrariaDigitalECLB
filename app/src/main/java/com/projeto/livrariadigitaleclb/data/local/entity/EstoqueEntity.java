package com.projeto.livrariadigitaleclb.data.local.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

import static androidx.room.ForeignKey.CASCADE;

@Entity(
        tableName = "estoque",
        foreignKeys = @ForeignKey(
                entity = LivroEntity.class,
                parentColumns = "id",
                childColumns = "livroId",
                onDelete = CASCADE
        )
)
public class EstoqueEntity {

    @PrimaryKey
    public int livroId; // mesmo id do Livro

    public int quantidadeDisponivel;

    public long dataAtualizacao; // System.currentTimeMillis()
}
