package com.projeto.livrariadigitaleclb;

import java.io.Serializable;

public class Produto implements Serializable {
    private String titulo;
    private String autor;
    private double preco;
    private int imagem;

    public Produto(String titulo, String autor, double preco, int imagem) {
        this.titulo = titulo;
        this.autor = autor;
        this.preco = preco;
        this.imagem = imagem;
    }

    public String getTitulo() { return titulo; }
    public String getAutor() { return autor; }
    public double getPreco() { return preco; }
    public int getImagem() { return imagem; }
}
