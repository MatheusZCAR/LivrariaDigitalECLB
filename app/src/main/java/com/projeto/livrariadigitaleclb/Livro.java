package com.projeto.livrariadigitaleclb;

public class Livro {
    public boolean isBotaoCadastrar;
    public String titulo;
    public String imagem;

    public Livro(boolean isBotaoCadastrar) {
        this.isBotaoCadastrar = isBotaoCadastrar;
    }

    public Livro(String titulo, String imagem) {
        this.titulo = titulo;
        this.imagem = imagem;
        this.isBotaoCadastrar = false;
    }
}
