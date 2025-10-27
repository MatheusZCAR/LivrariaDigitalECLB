package com.projeto.livrariadigitaleclb;

public class Produto {
    private String titulo;
    private String autor;
    private double preco;
    private int imagemRes;

    public Produto(String titulo, String autor, double preco, int imagemRes) {
        this.titulo = titulo;
        this.autor = autor;
        this.preco = preco;
        this.imagemRes = imagemRes;
    }

    public String getTitulo() { return titulo; }
    public String getAutor() { return autor; }
    public double getPreco() { return preco; }
    public int getImagemRes() { return imagemRes; }
}

