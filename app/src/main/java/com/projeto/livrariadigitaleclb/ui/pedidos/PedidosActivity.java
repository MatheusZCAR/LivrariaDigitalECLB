package com.projeto.livrariadigitaleclb.ui.pedidos;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.projeto.livrariadigitaleclb.App;
import com.projeto.livrariadigitaleclb.R;
import com.projeto.livrariadigitaleclb.backup.BackupAgendador;
import com.projeto.livrariadigitaleclb.data.PedidosService;
import com.projeto.livrariadigitaleclb.data.local.AppDatabase;
import com.projeto.livrariadigitaleclb.data.local.dao.PedidoDao;
import com.projeto.livrariadigitaleclb.data.local.entity.PedidoEntity;
import com.projeto.livrariadigitaleclb.databinding.ActivityPedidosBinding;
import com.projeto.livrariadigitaleclb.ui.BaseActivity;
import com.projeto.livrariadigitaleclb.util.Avisos;
import com.projeto.livrariadigitaleclb.util.Dialogos;
import com.projeto.livrariadigitaleclb.util.Documentos;
import com.projeto.livrariadigitaleclb.util.PdfEscritor;
import com.projeto.livrariadigitaleclb.util.Periodo;
import com.projeto.livrariadigitaleclb.util.SeletorQuantidade;
import com.projeto.livrariadigitaleclb.util.Texto;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PedidosActivity extends BaseActivity implements PedidosAdapter.Acoes {

    private ActivityPedidosBinding binding;
    private PedidoDao pedidoDao;
    private PedidosAdapter adapter;
    private List<PedidoEntity> pedidos = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPedidosBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        definirTitulo(getString(R.string.pedidos_titulo));

        pedidoDao = AppDatabase.getInstance(this).pedidoDao();
        adapter = new PedidosAdapter(this);
        binding.listaPedidos.setLayoutManager(new LinearLayoutManager(this));
        binding.listaPedidos.setAdapter(adapter);

        binding.btnAdicionarLivro.setOnClickListener(v -> abrirFormulario(null));
        binding.btnImprimirLista.setOnClickListener(v -> gerarPdf());
        binding.btnLimparLista.setOnClickListener(v -> confirmarLimpar());
    }

    @Override
    protected void onResume() {
        super.onResume();
        carregar();
    }

    private void carregar() {
        pedidos = pedidoDao.getPedidos();
        adapter.atualizarLista(pedidos);
        boolean vazio = pedidos.isEmpty();
        binding.txtVazio.setVisibility(vazio ? View.VISIBLE : View.GONE);
        binding.btnImprimirLista.setEnabled(!vazio);
        binding.btnLimparLista.setEnabled(!vazio);

        int exemplares = 0;
        for (PedidoEntity p : pedidos) exemplares += p.quantidadeDesejada;
        binding.txtResumo.setText(vazio
                ? getString(R.string.pedidos_subtitulo)
                : Texto.plural(pedidos.size(), "livro", "livros") + " na lista, "
                + Texto.plural(exemplares, "exemplar", "exemplares") + " no total.");
    }

    @Override
    public void editar(PedidoEntity pedido) {
        abrirFormulario(pedido);
    }

    @Override
    public void excluir(PedidoEntity pedido) {
        Dialogos.confirmar(this, "Tirar da lista?",
                "\"" + pedido.titulo + "\" vai sair da lista de pedidos.",
                "Sim, tirar", "Não", () -> {
                    pedidoDao.excluirPedido(pedido);
                    carregar();
                    BackupAgendador.dadosMudaram(this);
                    Avisos.comAcao(this, "\"" + pedido.titulo + "\" saiu da lista.", "Desfazer", () -> {
                        pedido.id = 0;
                        pedidoDao.inserirPedido(pedido);
                        carregar();
                    });
                });
    }

    /** Formulário para adicionar (pedido == null) ou editar. Só fecha quando os dados estão certos. */
    private void abrirFormulario(@Nullable PedidoEntity pedido) {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_add_livro, null);
        TextInputLayout campoTitulo = v.findViewById(R.id.campoTituloPedido);
        TextInputEditText titulo = v.findViewById(R.id.inputTitulo);
        TextInputEditText autor = v.findViewById(R.id.inputAutor);
        SeletorQuantidade seletor = new SeletorQuantidade(v.findViewById(R.id.seletor),
                pedido == null ? 1 : Math.max(1, pedido.quantidadeDesejada), 1, 999);

        if (pedido != null) {
            titulo.setText(pedido.titulo);
            autor.setText(pedido.autor);
        }

        AlertDialog dialogo = new MaterialAlertDialogBuilder(this)
                .setTitle(pedido == null ? "Adicionar à lista" : "Editar pedido")
                .setView(v)
                .setPositiveButton(R.string.salvar, null)
                .setNegativeButton(R.string.cancelar, null)
                .create();

        // O botão "Salvar" só fecha a janela se o nome do livro estiver preenchido
        // (antes a janela fechava e a pessoa perdia o que tinha digitado).
        dialogo.setOnShowListener(d -> dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(x -> {
            String t = titulo.getText() == null ? "" : titulo.getText().toString().trim();
            String a = autor.getText() == null ? "" : autor.getText().toString().trim();
            if (t.isEmpty()) {
                campoTitulo.setError("Escreva o nome do livro.");
                titulo.requestFocus();
                return;
            }
            if (pedido == null) {
                int total = PedidosService.adicionarOuSomar(AppDatabase.getInstance(this), t, a, seletor.getValor());
                if (total != seletor.getValor()) {
                    Avisos.sucesso(this, "\"" + t + "\" já estava na lista. Agora são " + total + ".");
                } else {
                    Avisos.sucesso(this, "\"" + t + "\" foi adicionado à lista.");
                }
            } else {
                pedido.titulo = t;
                pedido.autor = a;
                pedido.quantidadeDesejada = seletor.getValor();
                pedidoDao.atualizarPedido(pedido);
                Avisos.sucesso(this, "Pedido atualizado.");
            }
            BackupAgendador.dadosMudaram(this);
            dialogo.dismiss();
            carregar();
        }));
        dialogo.show();
    }

    private void confirmarLimpar() {
        Dialogos.confirmar(this, "Limpar a lista inteira?",
                "Todos os " + pedidos.size() + " livros vão sair da lista de pedidos. "
                        + "Faça isso depois que a compra chegar.",
                "Sim, limpar tudo", "Não", () -> {
                    pedidoDao.deletarTodos();
                    carregar();
                    BackupAgendador.dadosMudaram(this);
                    Avisos.sucesso(this, "A lista de pedidos foi esvaziada.");
                });
    }

    private void gerarPdf() {
        if (pedidos.isEmpty()) {
            Avisos.info(this, "A lista está vazia.");
            return;
        }
        List<PedidoEntity> copia = new ArrayList<>(pedidos);
        App.SEGUNDO_PLANO.execute(() -> {
            try {
                Date agora = new Date();
                String data = new SimpleDateFormat("dd/MM/yyyy", Periodo.BR).format(agora);
                PdfEscritor pdf = new PdfEscritor("Livraria ECLB — Lista de pedidos de " + data);
                pdf.titulo("Lista de pedidos — Livraria ECLB");
                pdf.texto("Data: " + data);
                int exemplares = 0;
                for (PedidoEntity p : copia) exemplares += p.quantidadeDesejada;
                pdf.texto(Texto.plural(copia.size(), "livro", "livros") + ", "
                        + Texto.plural(exemplares, "exemplar", "exemplares") + " no total");
                pdf.separador();
                int n = 1;
                for (PedidoEntity p : copia) {
                    String linha = n++ + ". " + p.titulo + (Texto.vazio(p.autor) ? "" : " — " + p.autor);
                    pdf.linhaComValor(linha, "Qtd: " + p.quantidadeDesejada, false);
                }
                File arquivo = pdf.salvar(new File(Documentos.pastaPdfs(this),
                        "Lista_Pedidos_" + new SimpleDateFormat("yyyy-MM-dd", Periodo.BR).format(agora) + ".pdf"));
                runOnUiThread(() -> Documentos.oferecer(this, arquivo, "Lista de pedidos"));
            } catch (Exception e) {
                runOnUiThread(() -> Avisos.erro(this, "Não foi possível gerar o PDF", String.valueOf(e.getMessage())));
            }
        });
    }
}
