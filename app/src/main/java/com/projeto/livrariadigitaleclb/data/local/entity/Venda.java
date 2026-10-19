package com.projeto.livrariadigitaleclb.data.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Uma linha de venda: um livro (com quantidade) dentro de uma venda.
 * Todas as linhas da mesma venda compartilham o mesmo {@link #codigoVenda}.
 *
 * Bancos antigos (versão 15) gravavam uma linha por exemplar, sem forma de pagamento;
 * a migração 15→16 preenche quantidade = 1 e codigoVenda = dataVenda para elas.
 */
@Entity(tableName = "vendas")
public class Venda {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public long livroId;
    public String titulo;

    /** Preço unitário no momento da venda. */
    public double preco;

    public long dataVenda;  // Timestamp em milissegundos

    @ColumnInfo(defaultValue = "1")
    public int quantidade = 1;

    /** Dinheiro, Pix, Débito ou Crédito. Pode ser nulo em vendas antigas. */
    public String formaPagamento;

    /** "À vista" ou "3x", por exemplo. Pode ser nulo em vendas antigas. */
    public String condicaoPagamento;

    /** Identifica a venda inteira (várias linhas). */
    @ColumnInfo(defaultValue = "0")
    public long codigoVenda;

    public double getSubtotal() {
        return preco * quantidade;
    }
}
