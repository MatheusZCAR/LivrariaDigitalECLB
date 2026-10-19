package com.projeto.livrariadigitaleclb.ui.relatorios;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.util.Pair;

import com.google.android.material.datepicker.MaterialDatePicker;
import com.projeto.livrariadigitaleclb.App;
import com.projeto.livrariadigitaleclb.R;
import com.projeto.livrariadigitaleclb.backup.BackupAgendador;
import com.projeto.livrariadigitaleclb.data.RelatorioVendas;
import com.projeto.livrariadigitaleclb.data.VendaService;
import com.projeto.livrariadigitaleclb.data.local.AppDatabase;
import com.projeto.livrariadigitaleclb.data.local.entity.Venda;
import com.projeto.livrariadigitaleclb.databinding.ActivityRelatoriosBinding;
import com.projeto.livrariadigitaleclb.databinding.ItemResumoBinding;
import com.projeto.livrariadigitaleclb.ui.BaseActivity;
import com.projeto.livrariadigitaleclb.util.Avisos;
import com.projeto.livrariadigitaleclb.util.Dialogos;
import com.projeto.livrariadigitaleclb.util.Documentos;
import com.projeto.livrariadigitaleclb.util.Moeda;
import com.projeto.livrariadigitaleclb.util.PdfEscritor;
import com.projeto.livrariadigitaleclb.util.Periodo;
import com.projeto.livrariadigitaleclb.util.Texto;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

public class RelatoriosActivity extends BaseActivity {

    private static final int MAX_MAIS_VENDIDOS = 10;

    private ActivityRelatoriosBinding binding;
    private AppDatabase db;
    private Periodo periodo;
    private RelatorioVendas relatorio;
    private int botaoAnterior = R.id.btnHoje;
    private final TimeZone fuso = TimeZone.getDefault();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRelatoriosBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        definirTitulo(getString(R.string.relatorios_titulo));
        db = AppDatabase.getInstance(this);

        binding.resumoTotal.rotuloResumo.setText(R.string.total_vendido);
        binding.resumoVendas.rotuloResumo.setText(R.string.numero_vendas);
        binding.resumoLivros.rotuloResumo.setText(R.string.livros_vendidos);

        binding.grupoPeriodo.check(R.id.btnHoje);
        periodo = Periodo.hoje(System.currentTimeMillis(), fuso);

        binding.grupoPeriodo.addOnButtonCheckedListener((grupo, id, marcado) -> {
            if (!marcado) return;
            long agora = System.currentTimeMillis();
            if (id == R.id.btnHoje) periodo = Periodo.hoje(agora, fuso);
            else if (id == R.id.btnSemana) periodo = Periodo.semana(agora, fuso);
            else if (id == R.id.btnMes) periodo = Periodo.mes(agora, fuso);
            else return; // "Escolher datas" é tratado no clique
            botaoAnterior = id;
            carregar();
        });
        binding.btnEscolherDatas.setOnClickListener(v -> escolherDatas());
        binding.btnImprimir.setOnClickListener(v -> gerarPdf());
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Se a tela ficou aberta de um dia para o outro, "Hoje" passa a ser o dia novo.
        long agora = System.currentTimeMillis();
        if (botaoAnterior == R.id.btnHoje) periodo = Periodo.hoje(agora, fuso);
        else if (botaoAnterior == R.id.btnSemana) periodo = Periodo.semana(agora, fuso);
        else if (botaoAnterior == R.id.btnMes) periodo = Periodo.mes(agora, fuso);
        carregar();
    }

    private void escolherDatas() {
        MaterialDatePicker<Pair<Long, Long>> seletor = MaterialDatePicker.Builder.dateRangePicker()
                .setTitleText("Escolha o primeiro e o último dia")
                .setPositiveButtonText("Ver vendas")
                .setNegativeButtonText(R.string.cancelar)
                .build();
        seletor.addOnPositiveButtonClickListener(selecao -> {
            if (selecao == null || selecao.first == null) return;
            long fim = selecao.second != null ? selecao.second : selecao.first;
            periodo = Periodo.entre(diaUtcParaLocal(selecao.first), diaUtcParaLocal(fim), fuso);
            botaoAnterior = R.id.btnEscolherDatas;
            carregar();
        });
        seletor.addOnDismissListener(d -> {
            if (botaoAnterior != R.id.btnEscolherDatas) binding.grupoPeriodo.check(botaoAnterior);
        });
        seletor.show(getSupportFragmentManager(), "datas");
    }

    /** O calendário devolve a data à meia-noite UTC; converte para o mesmo dia no fuso local. */
    private long diaUtcParaLocal(long utc) {
        Calendar u = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        u.setTimeInMillis(utc);
        Calendar l = Calendar.getInstance(fuso);
        l.clear();
        l.set(u.get(Calendar.YEAR), u.get(Calendar.MONTH), u.get(Calendar.DAY_OF_MONTH), 12, 0, 0);
        return l.getTimeInMillis();
    }

    private void carregar() {
        List<Venda> linhas = db.vendaDao().listarPorPeriodo(periodo.inicio, periodo.fim);
        relatorio = RelatorioVendas.calcular(linhas);

        binding.txtPeriodo.setText(periodo.descricao(fuso));
        preencher(binding.resumoTotal, Moeda.formatar(relatorio.totalGeral));
        preencher(binding.resumoVendas, String.valueOf(relatorio.vendas.size()));
        preencher(binding.resumoLivros, String.valueOf(relatorio.unidadesVendidas));

        boolean vazio = relatorio.vendas.isEmpty();
        binding.txtSemVendas.setVisibility(vazio ? View.VISIBLE : View.GONE);
        binding.secaoDetalhes.setVisibility(vazio ? View.GONE : View.VISIBLE);

        LayoutInflater inflater = LayoutInflater.from(this);

        binding.listaFormas.removeAllViews();
        for (Map.Entry<String, Double> e : relatorio.porFormaPagamento.entrySet()) {
            adicionarLinha(inflater, binding.listaFormas, e.getKey(), Moeda.formatar(e.getValue()));
        }

        binding.listaMaisVendidos.removeAllViews();
        for (int i = 0; i < Math.min(MAX_MAIS_VENDIDOS, relatorio.livros.size()); i++) {
            RelatorioVendas.LivroVendido lv = relatorio.livros.get(i);
            adicionarLinha(inflater, binding.listaMaisVendidos, (i + 1) + ". " + lv.titulo,
                    Texto.plural(lv.unidades, "exemplar", "exemplares") + "  ·  " + Moeda.formatar(lv.total));
        }

        binding.listaVendas.removeAllViews();
        SimpleDateFormat formato = new SimpleDateFormat("EEEE, dd/MM 'às' HH:mm", Periodo.BR);
        formato.setTimeZone(fuso);
        for (RelatorioVendas.VendaAgrupada venda : relatorio.vendas) {
            View item = inflater.inflate(R.layout.item_venda_relatorio, binding.listaVendas, false);
            String quando = formato.format(new Date(venda.data));
            ((TextView) item.findViewById(R.id.txtQuando)).setText(
                    Character.toUpperCase(quando.charAt(0)) + quando.substring(1));
            ((TextView) item.findViewById(R.id.txtPagamento)).setText(venda.descricaoPagamento());
            StringBuilder itens = new StringBuilder();
            for (Venda v : venda.itens) {
                if (itens.length() > 0) itens.append('\n');
                itens.append(v.quantidade).append(" × ").append(v.titulo);
            }
            ((TextView) item.findViewById(R.id.txtItens)).setText(itens);
            ((TextView) item.findViewById(R.id.txtTotalVenda)).setText(Moeda.formatar(venda.total()));
            item.findViewById(R.id.btnCancelarVenda).setOnClickListener(v -> confirmarCancelamento(venda));
            binding.listaVendas.addView(item);
        }
    }

    private void preencher(ItemResumoBinding resumo, String valor) {
        resumo.valorResumo.setText(valor);
    }

    private void adicionarLinha(LayoutInflater inflater, LinearLayout lista, String esquerda, String direita) {
        View linha = inflater.inflate(R.layout.item_linha_valor, lista, false);
        ((TextView) linha.findViewById(R.id.txtEsquerda)).setText(esquerda);
        ((TextView) linha.findViewById(R.id.txtDireita)).setText(direita);
        lista.addView(linha);
    }

    private void confirmarCancelamento(RelatorioVendas.VendaAgrupada venda) {
        Dialogos.confirmar(this, "Cancelar esta venda?",
                "Venda de " + Moeda.formatar(venda.total()) + " ("
                        + Texto.plural(venda.unidades(), "livro", "livros") + ").\n\n"
                        + "Os livros voltam para o estoque e o valor sai dos relatórios.",
                "Sim, cancelar venda", "Não", () -> {
                    try {
                        VendaService.cancelar(db, venda.codigo);
                        BackupAgendador.dadosMudaram(this);
                        carregar();
                        Avisos.sucesso(this, "Venda cancelada. Os livros voltaram para o estoque.");
                    } catch (Exception e) {
                        Avisos.erro(this, "Não foi possível cancelar", String.valueOf(e.getMessage()));
                    }
                });
    }

    private void gerarPdf() {
        if (relatorio == null || relatorio.vendas.isEmpty()) {
            Avisos.info(this, "Não há vendas neste período para imprimir.");
            return;
        }
        RelatorioVendas r = relatorio;
        Periodo p = periodo;
        App.SEGUNDO_PLANO.execute(() -> {
            try {
                SimpleDateFormat dataHora = new SimpleDateFormat("dd/MM/yyyy HH:mm", Periodo.BR);
                dataHora.setTimeZone(fuso);
                PdfEscritor pdf = new PdfEscritor("Livraria ECLB — Relatório gerado em "
                        + dataHora.format(new Date()));
                pdf.titulo("Relatório de vendas — Livraria ECLB");
                pdf.textoNegrito(p.descricao(fuso));
                pdf.separador();
                pdf.linhaComValor("Total vendido", Moeda.formatar(r.totalGeral), true);
                pdf.linhaComValor("Número de vendas", String.valueOf(r.vendas.size()), false);
                pdf.linhaComValor("Livros vendidos", String.valueOf(r.unidadesVendidas), false);

                pdf.subtitulo("Por forma de pagamento");
                for (Map.Entry<String, Double> e : r.porFormaPagamento.entrySet()) {
                    pdf.linhaComValor(e.getKey(), Moeda.formatar(e.getValue()), false);
                }

                pdf.subtitulo("Livros vendidos");
                for (RelatorioVendas.LivroVendido lv : r.livros) {
                    pdf.linhaComValor(lv.unidades + " × " + lv.titulo, Moeda.formatar(lv.total), false);
                }

                pdf.subtitulo("Vendas");
                for (RelatorioVendas.VendaAgrupada v : r.vendas) {
                    pdf.linhaComValor(dataHora.format(new Date(v.data)) + " — " + v.descricaoPagamento(),
                            Moeda.formatar(v.total()), true);
                    for (Venda item : v.itens) {
                        pdf.linhaComValor("    " + item.quantidade + " × " + item.titulo,
                                Moeda.formatar(item.getSubtotal()), false);
                    }
                    pdf.espaco(4);
                }

                SimpleDateFormat nome = new SimpleDateFormat("yyyy-MM-dd", Periodo.BR);
                nome.setTimeZone(fuso);
                File arquivo = pdf.salvar(new File(Documentos.pastaPdfs(this), "Relatorio_Vendas_"
                        + nome.format(new Date(p.inicio)) + "_a_" + nome.format(new Date(p.fim - 1)) + ".pdf"));
                runOnUiThread(() -> Documentos.oferecer(this, arquivo, "Relatório de vendas"));
            } catch (Exception e) {
                runOnUiThread(() -> Avisos.erro(this, "Não foi possível gerar o PDF", String.valueOf(e.getMessage())));
            }
        });
    }
}
