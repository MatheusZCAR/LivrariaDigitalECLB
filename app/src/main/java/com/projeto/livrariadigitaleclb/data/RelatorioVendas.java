package com.projeto.livrariadigitaleclb.data;

import com.projeto.livrariadigitaleclb.data.local.entity.Venda;
import com.projeto.livrariadigitaleclb.util.Moeda;
import com.projeto.livrariadigitaleclb.util.Texto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Calcula os números do relatório a partir das linhas de venda de um período. */
public final class RelatorioVendas {

    /** Uma venda completa (várias linhas com o mesmo código). */
    public static final class VendaAgrupada {
        public final long codigo;
        public final long data;
        public final String formaPagamento;
        public final String condicaoPagamento;
        public final List<Venda> itens = new ArrayList<>();

        VendaAgrupada(Venda primeira) {
            this.codigo = primeira.codigoVenda != 0 ? primeira.codigoVenda : primeira.dataVenda;
            this.data = primeira.dataVenda;
            this.formaPagamento = primeira.formaPagamento;
            this.condicaoPagamento = primeira.condicaoPagamento;
        }

        public double total() {
            double t = 0;
            for (Venda v : itens) t += v.getSubtotal();
            return Moeda.arredondar(t);
        }

        public int unidades() {
            int u = 0;
            for (Venda v : itens) u += v.quantidade;
            return u;
        }

        public String descricaoPagamento() {
            String forma = Texto.vazio(formaPagamento) ? "Não informado" : formaPagamento;
            if (!Texto.vazio(condicaoPagamento) && !"À vista".equalsIgnoreCase(condicaoPagamento)) {
                return forma + " (" + condicaoPagamento + ")";
            }
            return forma;
        }
    }

    /** Totais de um livro no período. */
    public static final class LivroVendido {
        public final String titulo;
        public int unidades;
        public double total;

        LivroVendido(String titulo) {
            this.titulo = titulo;
        }
    }

    public final List<VendaAgrupada> vendas;
    public final double totalGeral;
    public final int unidadesVendidas;
    /** forma de pagamento → total */
    public final Map<String, Double> porFormaPagamento;
    /** do mais vendido para o menos vendido */
    public final List<LivroVendido> livros;

    private RelatorioVendas(List<VendaAgrupada> vendas, double totalGeral, int unidadesVendidas,
                            Map<String, Double> porFormaPagamento, List<LivroVendido> livros) {
        this.vendas = vendas;
        this.totalGeral = totalGeral;
        this.unidadesVendidas = unidadesVendidas;
        this.porFormaPagamento = porFormaPagamento;
        this.livros = livros;
    }

    public static RelatorioVendas calcular(List<Venda> linhas) {
        LinkedHashMap<Long, VendaAgrupada> grupos = new LinkedHashMap<>();
        LinkedHashMap<String, Double> porForma = new LinkedHashMap<>();
        LinkedHashMap<String, LivroVendido> porLivro = new LinkedHashMap<>();
        double total = 0;
        int unidades = 0;

        for (Venda v : linhas) {
            long codigo = v.codigoVenda != 0 ? v.codigoVenda : v.dataVenda;
            VendaAgrupada g = grupos.get(codigo);
            if (g == null) {
                g = new VendaAgrupada(v);
                grupos.put(codigo, g);
            }
            g.itens.add(v);

            double subtotal = v.getSubtotal();
            total += subtotal;
            unidades += v.quantidade;

            String forma = Texto.vazio(v.formaPagamento) ? "Não informado" : v.formaPagamento;
            Double atual = porForma.get(forma);
            porForma.put(forma, (atual == null ? 0 : atual) + subtotal);

            String chave = Texto.normalizar(v.titulo);
            LivroVendido lv = porLivro.get(chave);
            if (lv == null) {
                lv = new LivroVendido(v.titulo == null ? "(sem título)" : v.titulo);
                porLivro.put(chave, lv);
            }
            lv.unidades += v.quantidade;
            lv.total += subtotal;
        }

        for (Map.Entry<String, Double> e : porForma.entrySet()) {
            e.setValue(Moeda.arredondar(e.getValue()));
        }
        List<LivroVendido> livros = new ArrayList<>(porLivro.values());
        for (LivroVendido lv : livros) lv.total = Moeda.arredondar(lv.total);
        Collections.sort(livros, (a, b) -> {
            if (b.unidades != a.unidades) return Integer.compare(b.unidades, a.unidades);
            return Double.compare(b.total, a.total);
        });

        List<VendaAgrupada> vendas = new ArrayList<>(grupos.values());
        Collections.sort(vendas, (a, b) -> Long.compare(b.data, a.data));

        return new RelatorioVendas(vendas, Moeda.arredondar(total), unidades, porForma, livros);
    }
}
