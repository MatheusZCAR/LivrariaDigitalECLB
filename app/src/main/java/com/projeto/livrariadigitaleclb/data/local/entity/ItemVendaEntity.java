package com.projeto.livrariadigitaleclb.data.local.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;

import static androidx.room.ForeignKey.CASCADE;

@Entity(
        tableName = "itens_venda",
        primaryKeys = {"vendaId", "livroId"},
        foreignKeys = {
                @ForeignKey(
                        entity = Venda.class,
                        parentColumns = "id",
                        childColumns = "vendaId",
                        onDelete = CASCADE
                ),
                @ForeignKey(
                        entity = LivroEntity.class,
                        parentColumns = "id",
                        childColumns = "livroId",
                        onDelete = CASCADE
                )
        }
)
public class ItemVendaEntity {

    public int vendaId;
    public int livroId;

    public int quantidadeVendida;
    public double precoUnitario;
}
