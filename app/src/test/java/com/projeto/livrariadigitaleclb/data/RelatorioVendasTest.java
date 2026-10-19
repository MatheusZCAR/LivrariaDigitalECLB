package com.projeto.livrariadigitaleclb.data;

import static org.junit.Assert.assertEquals;

import com.projeto.livrariadigitaleclb.data.local.entity.Venda;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class RelatorioVendasTest {

    private static Venda venda(long codigo, String titulo, double preco, int qtd, String forma) {
        Venda v = new Venda();
        v.codigoVenda = codigo;
        v.dataVenda = codigo;
        v.titulo = titulo;
        v.preco = preco;
        v.quantidade = qtd;
        v.formaPagamento = forma;
        return v;
    }

    @Test
    public void somaTotaisAgrupaVendasEFormas() {
        RelatorioVendas r = RelatorioVendas.calcular(Arrays.asList(
                venda(100, "Livro A", 20, 2, "Dinheiro"),
                venda(100, "Livro B", 15.5, 1, "Dinheiro"),
                venda(200, "Livro A", 20, 1, "Pix")));

        assertEquals(75.5, r.totalGeral, 0.001);
        assertEquals(4, r.unidadesVendidas);
        assertEquals(2, r.vendas.size());
        assertEquals(200, r.vendas.get(0).codigo); // mais recente primeiro
        assertEquals(55.5, r.vendas.get(1).total(), 0.001);
        assertEquals(55.5, r.porFormaPagamento.get("Dinheiro"), 0.001);
        assertEquals(20.0, r.porFormaPagamento.get("Pix"), 0.001);
        assertEquals("Livro A", r.livros.get(0).titulo);
        assertEquals(3, r.livros.get(0).unidades);
    }

    @Test
    public void vendasAntigasSemFormaDePagamento() {
        // Vendas da versão antiga: uma linha por exemplar, sem forma de pagamento,
        // agrupadas pelo horário (codigoVenda = dataVenda após a migração).
        RelatorioVendas r = RelatorioVendas.calcular(Arrays.asList(
                venda(500, "Hinário", 30, 1, null),
                venda(500, "Hinário", 30, 1, null)));
        assertEquals(1, r.vendas.size());
        assertEquals(60.0, r.totalGeral, 0.001);
        assertEquals(60.0, r.porFormaPagamento.get("Não informado"), 0.001);
        assertEquals("Não informado", r.vendas.get(0).descricaoPagamento());
    }

    @Test
    public void parcelamentoApareceNaDescricao() {
        Venda v = venda(1, "X", 90, 1, "Crédito");
        v.condicaoPagamento = "3x";
        RelatorioVendas r = RelatorioVendas.calcular(Collections.singletonList(v));
        assertEquals("Crédito (3x)", r.vendas.get(0).descricaoPagamento());
    }

    @Test
    public void periodoSemVendas() {
        RelatorioVendas r = RelatorioVendas.calcular(Collections.emptyList());
        assertEquals(0, r.totalGeral, 0);
        assertEquals(0, r.vendas.size());
    }
}
