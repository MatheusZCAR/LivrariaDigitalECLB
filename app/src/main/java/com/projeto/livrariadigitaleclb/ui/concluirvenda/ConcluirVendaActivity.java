package com.projeto.livrariadigitaleclb.ui.concluirvenda;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.projeto.livrariadigitaleclb.App;
import com.projeto.livrariadigitaleclb.R;
import com.projeto.livrariadigitaleclb.backup.BackupAgendador;
import com.projeto.livrariadigitaleclb.data.Carrinho;
import com.projeto.livrariadigitaleclb.data.VendaService;
import com.projeto.livrariadigitaleclb.data.local.AppDatabase;
import com.projeto.livrariadigitaleclb.data.local.entity.EstoqueEntity;
import com.projeto.livrariadigitaleclb.data.local.entity.LivroEntity;
import com.projeto.livrariadigitaleclb.databinding.ActivityConcluirVendaBinding;
import com.projeto.livrariadigitaleclb.ui.BaseActivity;
import com.projeto.livrariadigitaleclb.util.Avisos;
import com.projeto.livrariadigitaleclb.util.Capas;
import com.projeto.livrariadigitaleclb.util.Documentos;
import com.projeto.livrariadigitaleclb.util.Moeda;
import com.projeto.livrariadigitaleclb.util.PdfEscritor;
import com.projeto.livrariadigitaleclb.util.Periodo;
import com.projeto.livrariadigitaleclb.util.SeletorQuantidade;
import com.projeto.livrariadigitaleclb.util.Texto;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ConcluirVendaActivity extends BaseActivity {

    /** Um livro da venda, com o estoque disponível para limitar a quantidade. */
    static final class Item {
        final LivroEntity livro;
        final int estoqueMaximo;
        int quantidade;

        Item(LivroEntity livro, int quantidade, int estoqueMaximo) {
            this.livro = livro;
            this.quantidade = quantidade;
            this.estoqueMaximo = estoqueMaximo;
        }

        double subtotal() {
            return livro.preco * quantidade;
        }
    }

    private ActivityConcluirVendaBinding binding;
    private AppDatabase db;
    private final Carrinho carrinho = Carrinho.get();
    private final List<Item> itens = new ArrayList<>();
    private ItensAdapter adapter;
    private SeletorQuantidade seletorParcelas;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityConcluirVendaBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        definirTitulo(getString(R.string.concluir_titulo));
        db = AppDatabase.getInstance(this);

        carregarItens();
        if (itens.isEmpty()) {
            finish();
            return;
        }

        adapter = new ItensAdapter();
        binding.recyclerCarrinho.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerCarrinho.setAdapter(adapter);

        seletorParcelas = new SeletorQuantidade(binding.seletorParcelas.getRoot(), 1, 1, 12);
        seletorParcelas.setAoMudar(n -> atualizarParcelas());

        binding.grupoPagamento.addOnButtonCheckedListener((grupo, id, marcado) -> atualizarSecoes());

        binding.edtRecebido.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { atualizarTroco(); }
            @Override public void afterTextChanged(Editable s) { }
        });
        binding.btnValorExato.setOnClickListener(v -> definirRecebido(total()));
        binding.btnValor20.setOnClickListener(v -> definirRecebido(20));
        binding.btnValor50.setOnClickListener(v -> definirRecebido(50));
        binding.btnValor100.setOnClickListener(v -> definirRecebido(100));

        binding.btnConfirmar.setOnClickListener(v -> confirmar());

        atualizarTotais();
        atualizarSecoes();
    }

    private void carregarItens() {
        itens.clear();
        for (Map.Entry<Integer, Integer> e : carrinho.itens().entrySet()) {
            LivroEntity livro = db.livroDao().getLivroById(e.getKey());
            if (livro == null) {
                carrinho.remover(e.getKey());
                continue;
            }
            EstoqueEntity estoque = db.estoqueDao().buscarPorLivro(livro.id);
            int max = estoque == null ? 0 : estoque.quantidadeDisponivel;
            int qtd = Math.min(e.getValue(), max);
            if (qtd <= 0) {
                carrinho.remover(livro.id);
                continue;
            }
            carrinho.definir(livro.id, qtd);
            itens.add(new Item(livro, qtd, max));
        }
    }

    private double total() {
        double t = 0;
        for (Item i : itens) t += i.subtotal();
        return Moeda.arredondar(t);
    }

    private int unidades() {
        int u = 0;
        for (Item i : itens) u += i.quantidade;
        return u;
    }

    private void atualizarTotais() {
        binding.txtTotalItens.setText("Total a pagar (" + Texto.plural(unidades(), "livro", "livros") + ")");
        binding.txtValorTotal.setText(Moeda.formatar(total()));
        atualizarTroco();
        atualizarParcelas();
    }

    // ------------------------------------------------------------- pagamento

    private String formaEscolhida() {
        int id = binding.grupoPagamento.getCheckedButtonId();
        if (id == R.id.btnDinheiro) return getString(R.string.dinheiro);
        if (id == R.id.btnPix) return getString(R.string.pix);
        if (id == R.id.btnDebito) return getString(R.string.debito);
        if (id == R.id.btnCredito) return getString(R.string.credito);
        return null;
    }

    private void atualizarSecoes() {
        int id = binding.grupoPagamento.getCheckedButtonId();
        binding.secaoDinheiro.setVisibility(id == R.id.btnDinheiro ? View.VISIBLE : View.GONE);
        binding.secaoCredito.setVisibility(id == R.id.btnCredito ? View.VISIBLE : View.GONE);
        if (id != View.NO_ID) binding.txtErroPagamento.setVisibility(View.GONE);
        atualizarTroco();
        atualizarParcelas();
    }

    private void definirRecebido(double valor) {
        binding.edtRecebido.setText(Moeda.formatarSemSimbolo(valor));
        if (binding.edtRecebido.getText() != null) {
            binding.edtRecebido.setSelection(binding.edtRecebido.getText().length());
        }
    }

    private Double recebido() {
        return Moeda.interpretar(binding.edtRecebido.getText() == null ? "" : binding.edtRecebido.getText().toString());
    }

    private void atualizarTroco() {
        Double recebido = recebido();
        binding.campoRecebido.setError(null);
        if (recebido == null || binding.grupoPagamento.getCheckedButtonId() != R.id.btnDinheiro) {
            binding.txtTroco.setVisibility(View.GONE);
            return;
        }
        double diferenca = Moeda.arredondar(recebido - total());
        binding.txtTroco.setVisibility(View.VISIBLE);
        if (diferenca >= 0) {
            binding.txtTroco.setText("Troco: " + Moeda.formatar(diferenca));
            binding.txtTroco.setTextColor(getColor(R.color.sucesso));
            binding.txtTroco.setBackgroundColor(getColor(R.color.sucesso_claro));
        } else {
            binding.txtTroco.setText("Faltam " + Moeda.formatar(-diferenca));
            binding.txtTroco.setTextColor(getColor(R.color.perigo));
            binding.txtTroco.setBackgroundColor(getColor(R.color.perigo_claro));
        }
    }

    private void atualizarParcelas() {
        if (seletorParcelas == null) return;
        int n = seletorParcelas.getValor();
        binding.txtParcelas.setText(n == 1
                ? "À vista: " + Moeda.formatar(total())
                : n + "x de " + Moeda.formatar(total() / n));
    }

    private String condicaoEscolhida() {
        if (binding.grupoPagamento.getCheckedButtonId() == R.id.btnCredito && seletorParcelas.getValor() > 1) {
            return seletorParcelas.getValor() + "x";
        }
        return "À vista";
    }

    // ------------------------------------------------------------- confirmação

    private void confirmar() {
        String forma = formaEscolhida();
        if (forma == null) {
            binding.txtErroPagamento.setVisibility(View.VISIBLE);
            binding.rolagemPagamento.smoothScrollTo(0, 0);
            Avisos.info(this, "Escolha como a pessoa vai pagar.");
            return;
        }
        double total = total();
        Double recebido = forma.equals(getString(R.string.dinheiro)) ? recebido() : null;
        if (recebido != null && recebido < total) {
            binding.campoRecebido.setError("O valor recebido é menor que o total.");
            return;
        }

        StringBuilder msg = new StringBuilder();
        msg.append(Texto.plural(unidades(), "livro", "livros")).append("\n");
        msg.append("Total: ").append(Moeda.formatar(total)).append("\n");
        msg.append("Pagamento: ").append(forma);
        String condicao = condicaoEscolhida();
        if (!"À vista".equals(condicao)) msg.append(" em ").append(condicao);
        if (recebido != null) {
            msg.append("\nRecebido: ").append(Moeda.formatar(recebido));
            msg.append("\nTroco: ").append(Moeda.formatar(recebido - total));
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Confirmar a venda?")
                .setMessage(msg)
                .setPositiveButton("Sim, confirmar", (d, w) -> registrar(forma, condicao, recebido))
                .setNegativeButton(R.string.voltar, null)
                .show();
    }

    private void registrar(String forma, String condicao, Double recebido) {
        Map<Integer, Integer> paraVender = new LinkedHashMap<>();
        for (Item i : itens) paraVender.put(i.livro.id, i.quantidade);
        double total = total();
        List<Item> vendidos = new ArrayList<>(itens);

        try {
            VendaService.registrar(db, paraVender, forma, condicao);
        } catch (VendaService.EstoqueInsuficienteException e) {
            Avisos.erro(this, "Estoque mudou",
                    "Não há exemplares suficientes de \"" + e.titulo + "\" (restam " + e.disponivel
                            + "). As quantidades foram ajustadas; confira e confirme de novo.");
            carregarItens();
            adapter.notifyDataSetChanged();
            atualizarTotais();
            if (itens.isEmpty()) finish();
            return;
        } catch (Exception e) {
            Avisos.erro(this, "A venda não foi registrada",
                    "Aconteceu um erro e nada foi gravado. Tente de novo.\n\n(" + e.getMessage() + ")");
            return;
        }

        carrinho.limpar();
        BackupAgendador.dadosMudaram(this);
        setResult(RESULT_OK);
        mostrarSucesso(vendidos, total, forma, condicao, recebido);
    }

    private void mostrarSucesso(List<Item> vendidos, double total, String forma, String condicao, Double recebido) {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_venda_concluida, null);
        String resumo = "Total: " + Moeda.formatar(total) + " — " + forma
                + ("À vista".equals(condicao) ? "" : " em " + condicao);
        ((TextView) v.findViewById(R.id.txtResumoVenda)).setText(resumo);
        TextView troco = v.findViewById(R.id.txtTrocoFinal);
        if (recebido != null && recebido - total > 0.004) {
            troco.setVisibility(View.VISIBLE);
            troco.setText("Troco: " + Moeda.formatar(recebido - total));
        }

        AlertDialog dialogo = new MaterialAlertDialogBuilder(this)
                .setView(v)
                .setCancelable(false)
                .create();
        v.findViewById(R.id.btnImprimirRecibo).setOnClickListener(x ->
                gerarRecibo(vendidos, total, forma, condicao, recebido, true));
        v.findViewById(R.id.btnEnviarRecibo).setOnClickListener(x ->
                gerarRecibo(vendidos, total, forma, condicao, recebido, false));
        v.findViewById(R.id.btnNovaVenda).setOnClickListener(x -> {
            dialogo.dismiss();
            finish();
        });
        dialogo.show();
    }

    private void gerarRecibo(List<Item> vendidos, double total, String forma, String condicao,
                             Double recebido, boolean imprimir) {
        App.SEGUNDO_PLANO.execute(() -> {
            try {
                long agora = System.currentTimeMillis();
                PdfEscritor pdf = new PdfEscritor("Livraria ECLB — documento sem valor fiscal");
                pdf.titulo("Livraria ECLB — Comprovante de venda");
                pdf.texto(new SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Periodo.BR).format(new Date(agora)));
                pdf.separador();
                for (Item i : vendidos) {
                    pdf.linhaComValor(i.quantidade + " x " + i.livro.titulo
                                    + (Texto.vazio(i.livro.autor) ? "" : " — " + i.livro.autor),
                            Moeda.formatar(i.subtotal()), false);
                }
                pdf.separador();
                pdf.linhaComValor("TOTAL", Moeda.formatar(total), true);
                pdf.texto("Pagamento: " + forma + ("À vista".equals(condicao) ? "" : " em " + condicao));
                if (recebido != null) {
                    pdf.texto("Recebido: " + Moeda.formatar(recebido));
                    pdf.texto("Troco: " + Moeda.formatar(recebido - total));
                }
                pdf.espaco(16);
                pdf.texto("Obrigado e volte sempre!");
                File arquivo = pdf.salvar(new File(Documentos.pastaPdfs(this),
                        "Recibo_" + new SimpleDateFormat("yyyy-MM-dd_HHmmss", Periodo.BR).format(new Date(agora)) + ".pdf"));
                runOnUiThread(() -> {
                    if (imprimir) Documentos.imprimir(this, arquivo, "Recibo Livraria ECLB");
                    else Documentos.compartilhar(this, arquivo, "Recibo Livraria ECLB");
                });
            } catch (Exception e) {
                runOnUiThread(() -> Avisos.erro(this, "Recibo não gerado",
                        "Não foi possível gerar o recibo. A venda já está registrada."));
            }
        });
    }

    // ------------------------------------------------------------- lista

    private void alterarQuantidade(Item item, int nova, int posicao) {
        if (nova <= 0) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Tirar da venda?")
                    .setMessage("\"" + item.livro.titulo + "\" vai sair desta venda.")
                    .setPositiveButton("Sim, tirar", (d, w) -> remover(item))
                    .setNegativeButton("Não", (d, w) -> adapter.notifyItemChanged(posicao))
                    .setOnCancelListener(d -> adapter.notifyItemChanged(posicao))
                    .show();
            return;
        }
        item.quantidade = nova;
        carrinho.definir(item.livro.id, nova);
        adapter.notifyItemChanged(posicao);
        atualizarTotais();
    }

    private void remover(Item item) {
        itens.remove(item);
        carrinho.remover(item.livro.id);
        adapter.notifyDataSetChanged();
        atualizarTotais();
        if (itens.isEmpty()) {
            Avisos.info(this, "A venda ficou vazia.");
            finish();
        }
    }

    private final class ItensAdapter extends RecyclerView.Adapter<ItensAdapter.VH> {

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_carrinho, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            Item item = itens.get(position);
            h.titulo.setText(item.livro.titulo);
            h.preco.setText(Moeda.formatar(item.livro.preco) + " cada");
            h.subtotal.setText(Moeda.formatar(item.subtotal()));
            Capas.carregar(h.capa, item.livro.imagemPath);

            h.seletor.setAoMudar(null);
            h.seletor.setLimites(0, item.estoqueMaximo);
            h.seletor.definir(item.quantidade, false);
            h.seletor.setAoMudar(n -> alterarQuantidade(item, n, h.getBindingAdapterPosition()));
            h.seletor.setAoPassarDoMaximo(() -> Avisos.info(ConcluirVendaActivity.this,
                    "Só há " + Texto.plural(item.estoqueMaximo, "exemplar", "exemplares")
                            + " de \"" + item.livro.titulo + "\" em estoque."));
            h.remover.setOnClickListener(v -> alterarQuantidade(item, 0, h.getBindingAdapterPosition()));
        }

        @Override
        public int getItemCount() {
            return itens.size();
        }

        final class VH extends RecyclerView.ViewHolder {
            final ImageView capa;
            final TextView titulo, preco, subtotal;
            final View remover;
            final SeletorQuantidade seletor;

            VH(View v) {
                super(v);
                capa = v.findViewById(R.id.imgCapa);
                titulo = v.findViewById(R.id.txtTitulo);
                preco = v.findViewById(R.id.txtPreco);
                subtotal = v.findViewById(R.id.txtSubtotal);
                remover = v.findViewById(R.id.btnRemover);
                seletor = new SeletorQuantidade(v.findViewById(R.id.seletor), 1, 0, 9999);
            }
        }
    }
}
