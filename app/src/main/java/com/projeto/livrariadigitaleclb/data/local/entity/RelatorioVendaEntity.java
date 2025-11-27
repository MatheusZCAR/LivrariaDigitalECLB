package com.projeto.livrariadigitaleclb.data.local.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;

import static androidx.room.ForeignKey.CASCADE;

@Entity(
        tableName = "relatorio_venda",
        primaryKeys = {"relatorioId", "vendaId"},
        foreignKeys = {
                @ForeignKey(
                        entity = RelatorioEntity.class,
                        parentColumns = "id",
                        childColumns = "relatorioId",
                        onDelete = CASCADE
                ),
                @ForeignKey(
                        entity = Venda.class,
                        parentColumns = "id",
                        childColumns = "vendaId",
                        onDelete = CASCADE
                )
        }
)
public class RelatorioVendaEntity {

    public int relatorioId;
    public int vendaId;
}
