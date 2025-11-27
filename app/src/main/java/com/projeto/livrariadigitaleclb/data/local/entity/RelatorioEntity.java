package com.projeto.livrariadigitaleclb.data.local.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "relatorios")
public class RelatorioEntity {

    @PrimaryKey(autoGenerate = true)
    public int id;

    // DIARIO, SEMANAL, MENSAL
    public String tipoRelatorio;

    // caminho do PDF ou algo assim
    public String localizacaoRelatorio;
}
