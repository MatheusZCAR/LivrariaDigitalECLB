package com.projeto.livrariadigitaleclb.ui.concluirvenda;

import android.content.Intent;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.projeto.livrariadigitaleclb.R;
import com.projeto.livrariadigitaleclb.data.local.AppDatabase;
import com.projeto.livrariadigitaleclb.data.local.dao.EstoqueDao;
import com.projeto.livrariadigitaleclb.data.local.dao.LivroDao;
import com.projeto.livrariadigitaleclb.data.local.dao.VendaDao;
import com.projeto.livrariadigitaleclb.data.local.entity.EstoqueEntity;
import com.projeto.livrariadigitaleclb.data.local.entity.LivroEntity;
import com.projeto.livrariadigitaleclb.data.local.entity.Venda;
import com.projeto.livrariadigitaleclb.databinding.ActivityConcluirVendaBinding;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ConcluirVendaActivity extends AppCompatActivity {

    private ActivityConcluirVendaBinding binding;
    private LivroDao livroDao;
    private VendaDao vendaDao;
    private EstoqueDao estoqueDao;

    private CarrinhoAdapter adapter;
    private final List<ItemCarrinho> itensCarrinho = new ArrayList<>();
    private double valorTotal = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityConcluirVendaBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        AppDatabase db = AppDatabase.getInstance(this);
        livroDao = db.livroDao();
        vendaDao = db.vendaDao();
        estoqueDao = db.estoqueDao();

        configurarSpinnerPagamento();
        configurarCondicaoParcelamento();
        carregarCarrinho();
        configurarLista();
        configurarBotoes();
        atualizarTotal();
    }

    private void configurarSpinnerPagamento() {
        String[] opcoes = {"Dinheiro", "Pix", "Crédito", "Débito"};

        ArrayAdapter<String> adapterSpinner = new ArrayAdapter<>(
                this,
                R.layout.spinner_item_selected,
                opcoes
        );
        adapterSpinner.setDropDownViewResource(R.layout.spinner_item_dropdown);

        binding.spinnerPagamento.setAdapter(adapterSpinner);
    }

    private void configurarCondicaoParcelamento() {
        // Estado inicial
        atualizarCampoParcelas();

        binding.radioGroupCondicao.setOnCheckedChangeListener((group, checkedId) -> {
            atualizarCampoParcelas();
        });
    }

    private void atualizarCampoParcelas() {
        boolean parcelado = binding.radioParcelado.isChecked();
        binding.editParcelas.setEnabled(parcelado);
        binding.editParcelas.setAlpha(parcelado ? 1f : 0.5f);

        if (!parcelado) {
            binding.editParcelas.setText("");
        }
    }

    private void carregarCarrinho() {
        ArrayList<Long> livroIds =
                (ArrayList<Long>) getIntent().getSerializableExtra("livroIds");
        ArrayList<Integer> quantidades =
                (ArrayList<Integer>) getIntent().getSerializableExtra("quantidades");

        if (livroIds != null && quantidades != null) {
            for (int i = 0; i < livroIds.size(); i++) {
                int id = livroIds.get(i).intValue();
                LivroEntity livro = livroDao.getLivroById(id);
                EstoqueEntity estoque = estoqueDao.buscarPorLivro(id);
                int maxEstoque = (estoque != null) ? estoque.quantidadeDisponivel : 0;

                if (livro != null) {
                    itensCarrinho.add(new ItemCarrinho(livro, quantidades.get(i), maxEstoque));
                }
            }
        }

        if (itensCarrinho.isEmpty()) {
            Toast.makeText(this, "Erro ao carregar carrinho", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void configurarLista() {
        adapter = new CarrinhoAdapter(itensCarrinho, this::onQuantidadeAlterada, this::onRemoverItem);
        binding.recyclerCarrinho.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerCarrinho.setAdapter(adapter);
    }

    private void onQuantidadeAlterada(ItemCarrinho item, int novaQuantidade) {
        if (novaQuantidade <= 0) {
            onRemoverItem(item);
            return;
        }
        if (novaQuantidade > item.estoqueMaximo) {
            Toast.makeText(this,
                    "Estoque máximo atingido (" + item.estoqueMaximo + ")",
                    Toast.LENGTH_SHORT
            ).show();
            adapter.notifyDataSetChanged();
            return;
        }
        item.quantidade = novaQuantidade;
        atualizarTotal();
        adapter.notifyDataSetChanged();
    }

    private void onRemoverItem(ItemCarrinho item) {
        new AlertDialog.Builder(this)
                .setTitle("Remover Item")
                .setMessage("Remover \"" + item.livro.titulo + "\"?")
                .setPositiveButton("Sim", (dialog, which) -> {
                    itensCarrinho.remove(item);
                    adapter.notifyDataSetChanged();
                    atualizarTotal();
                    if (itensCarrinho.isEmpty()) {
                        setResult(RESULT_CANCELED);
                        finish();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void atualizarTotal() {
        valorTotal = 0.0;
        int totalItens = 0;
        for (ItemCarrinho item : itensCarrinho) {
            valorTotal += item.livro.preco * item.quantidade;
            totalItens += item.quantidade;
        }
        binding.txtTotalItens.setText(totalItens + " item(ns)");
        binding.txtValorTotal.setText(
                String.format(Locale.getDefault(), "R$ %.2f", valorTotal)
        );
    }

    private void configurarBotoes() {
        binding.btnCancelar.setOnClickListener(v -> finish());
        binding.btnConfirmar.setOnClickListener(v -> confirmarVenda());
    }

    private void confirmarVenda() {
        String metodo = binding.spinnerPagamento.getSelectedItem().toString();

        String condicao;
        if (binding.radioVista.isChecked()) {
            condicao = "À Vista";
        } else {
            String textoParcelas = binding.editParcelas.getText().toString().trim();

            if (textoParcelas.isEmpty()) {
                Toast.makeText(this, "Informe o número de parcelas.", Toast.LENGTH_SHORT).show();
                return;
            }

            int parcelas;
            try {
                parcelas = Integer.parseInt(textoParcelas);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Número de parcelas inválido.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (parcelas < 2) {
                Toast.makeText(this,
                        "Número de parcelas deve ser pelo menos 2.",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            condicao = "Parcelado em " + parcelas + "x";
        }

        String mensagem = "Total: " +
                String.format(Locale.getDefault(), "R$ %.2f", valorTotal) +
                "\nPagamento: " + metodo + " (" + condicao + ")" +
                "\n\nConfirmar venda?";

        new AlertDialog.Builder(this)
                .setTitle("Confirmar Venda")
                .setMessage(mensagem)
                .setPositiveButton("Confirmar", (dialog, which) -> registrarVendas(metodo, condicao))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void registrarVendas(String metodoPagamento, String condicaoPagamento) {
        long dataAtual = System.currentTimeMillis();
        try {
            for (ItemCarrinho item : itensCarrinho) {
                for (int i = 0; i < item.quantidade; i++) {
                    Venda venda = new Venda();
                    venda.livroId = item.livro.id;
                    venda.titulo = item.livro.titulo;
                    venda.preco = item.livro.preco;
                    venda.dataVenda = dataAtual;
                    vendaDao.registrarVenda(venda);
                }
                estoqueDao.baixarEstoque(item.livro.id, item.quantidade);
            }

            gerarReciboPDF(metodoPagamento, condicaoPagamento);

            Toast.makeText(this, "Venda registrada!", Toast.LENGTH_LONG).show();
            setResult(RESULT_OK);
            finish();

        } catch (Exception e) {
            Toast.makeText(this, "Erro: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void gerarReciboPDF(String metodo, String condicao) {
        try {
            PdfDocument pdf = new PdfDocument();
            PdfDocument.PageInfo pageInfo =
                    new PdfDocument.PageInfo.Builder(595, 842, 1).create();
            PdfDocument.Page page = pdf.startPage(pageInfo);

            Paint paint = new Paint();
            Paint boldPaint = new Paint();
            boldPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

            int x = 40;
            int y = 50;

            // Título
            boldPaint.setTextSize(20);
            page.getCanvas().drawText("Comprovante de Venda - ECLB", x, y, boldPaint);
            y += 30;

            paint.setTextSize(12);
            String dataStr = new SimpleDateFormat(
                    "dd/MM/yyyy HH:mm:ss",
                    Locale.getDefault()
            ).format(new Date());
            page.getCanvas().drawText("Data: " + dataStr, x, y, paint);
            y += 20;

            page.getCanvas().drawLine(x, y, 555, y, paint);
            y += 30;

            // Itens vendidos
            boldPaint.setTextSize(14);
            page.getCanvas().drawText("ITENS VENDIDOS:", x, y, boldPaint);
            y += 25;

            paint.setTextSize(12);
            for (ItemCarrinho item : itensCarrinho) {
                String titulo = item.livro.titulo;
                String autor = item.livro.autor != null ? item.livro.autor : "";

                boldPaint.setTextSize(12);
                page.getCanvas().drawText(titulo + " - " + autor, x, y, boldPaint);
                y += 15;

                String detalhe = String.format(
                        Locale.getDefault(),
                        "%d x R$ %.2f = R$ %.2f",
                        item.quantidade,
                        item.livro.preco,
                        item.getSubtotal()
                );
                page.getCanvas().drawText(detalhe, x + 20, y, paint);
                y += 30;
            }

            page.getCanvas().drawLine(x, y, 555, y, paint);
            y += 30;

            // Descobrir número de parcelas a partir da string "Parcelado em 2x"
            int qtdParcelas = 1;
            if (condicao != null &&
                    condicao.toLowerCase(Locale.getDefault()).contains("parcelado")) {
                String digits = condicao.replaceAll("\\D+", "");
                if (!digits.isEmpty()) {
                    try {
                        qtdParcelas = Integer.parseInt(digits);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }

            double valorParcela = qtdParcelas > 1
                    ? valorTotal / qtdParcelas
                    : valorTotal;

            // Resumo do pagamento
            boldPaint.setTextSize(14);
            page.getCanvas().drawText("RESUMO DO PAGAMENTO:", x, y, boldPaint);
            y += 25;

            paint.setTextSize(14);
            page.getCanvas().drawText("Forma: " + metodo, x, y, paint);
            y += 20;

            page.getCanvas().drawText("Condição: " + condicao, x, y, paint);
            y += 20;

            if (qtdParcelas > 1) {
                String textoParcelas = String.format(
                        Locale.getDefault(),
                        "%dx de R$ %.2f",
                        qtdParcelas,
                        valorParcela
                );
                page.getCanvas().drawText("Parcelas: " + textoParcelas, x, y, paint);
                y += 20;
            }

            boldPaint.setTextSize(18);
            page.getCanvas().drawText(
                    "TOTAL: " + String.format(Locale.getDefault(), "R$ %.2f", valorTotal),
                    x,
                    y,
                    boldPaint
            );

            pdf.finishPage(page);

            File pasta = new File(getExternalFilesDir(null), "recibos");
            if (!pasta.exists()) {
                pasta.mkdirs();
            }

            String nomeArquivo = "Recibo_" + System.currentTimeMillis() + ".pdf";
            File arquivo = new File(pasta, nomeArquivo);
            FileOutputStream fos = new FileOutputStream(arquivo);
            pdf.writeTo(fos);
            pdf.close();
            fos.close();

            abrirPDF(arquivo);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Erro ao gerar recibo PDF", Toast.LENGTH_SHORT).show();
        }
    }

    private void abrirPDF(File arquivo) {
        try {
            Uri uri = FileProvider.getUriForFile(
                    this,
                    "com.projeto.livrariadigitaleclb.fileprovider",
                    arquivo
            );

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/pdf");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Nenhum leitor de PDF instalado.", Toast.LENGTH_LONG).show();
        }
    }

    public static class ItemCarrinho {
        public LivroEntity livro;
        public int quantidade;
        public int estoqueMaximo;

        public ItemCarrinho(LivroEntity livro, int quantidade, int estoqueMaximo) {
            this.livro = livro;
            this.quantidade = quantidade;
            this.estoqueMaximo = estoqueMaximo;
        }

        public double getSubtotal() {
            return livro.preco * quantidade;
        }
    }
}
