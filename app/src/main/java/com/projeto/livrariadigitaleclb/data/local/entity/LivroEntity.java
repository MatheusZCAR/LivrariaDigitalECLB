package com.projeto.livrariadigitaleclb.data.local.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "livros")
public class LivroEntity {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String titulo;
    public String autor;
    public String codigoBarras;

    public double preco;

    public boolean esgotado = false;

    // Caminho da imagem salva (opcional)
    public String imagemPath;
}

