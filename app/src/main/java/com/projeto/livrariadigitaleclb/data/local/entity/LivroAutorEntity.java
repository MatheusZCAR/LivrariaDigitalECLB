package com.projeto.livrariadigitaleclb.data.local.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;

import static androidx.room.ForeignKey.CASCADE;

@Entity(
        tableName = "livro_autor",
        primaryKeys = {"livroId", "autorId"},
        foreignKeys = {
                @ForeignKey(
                        entity = LivroEntity.class,
                        parentColumns = "id",
                        childColumns = "livroId",
                        onDelete = CASCADE
                ),
                @ForeignKey(
                        entity = AutorEntity.class,
                        parentColumns = "id",
                        childColumns = "autorId",
                        onDelete = CASCADE
                )
        }
)
public class LivroAutorEntity {

    public int livroId;
    public int autorId;
}
