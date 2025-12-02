package com.projeto.livrariadigitaleclb.data.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Embedded;

public class LivroComEstoque {
    @Embedded
    public LivroEntity livro;

    @ColumnInfo(name = "qtd")
    public int quantidade;
}
