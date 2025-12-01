package com.projeto.livrariadigitaleclb.ui.realizarvenda;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.projeto.livrariadigitaleclb.data.local.AppDatabase;
import com.projeto.livrariadigitaleclb.data.local.dao.LivroDao;
import com.projeto.livrariadigitaleclb.data.local.entity.LivroEntity;
import com.projeto.livrariadigitaleclb.databinding.ActivityRealizarVendaBinding;
import com.projeto.livrariadigitaleclb.ui.catalogo.CadastrarLivroActivity;
import com.projeto.livrariadigitaleclb.ui.concluirvenda.ConcluirVendaActivity;

import java.util.List;

public class RealizarVendaActivity extends AppCompatActivity {

    private ActivityRealizarVendaBinding binding;
    private ProdutoAdapter adapter;
    private LivroDao livroDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRealizarVendaBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        AppDatabase db = AppDatabase.getInstance(this);
        livroDao = db.livroDao();

        configurarLista();
        configurarBusca();
        configurarBotoes();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Atualiza a lista quando voltar para essa tela
        // (útil se cadastrou um novo livro)
        atualizarLista();
    }

    private void configurarLista() {
        List<LivroEntity> livros = livroDao.listarLivrosParaVenda();

        adapter = new ProdutoAdapter(
                livros,
                livro -> {
                    Intent intent = new Intent(this, ConcluirVendaActivity.class);
                    intent.putExtra("livroId", livro.id);
                    startActivity(intent);
                }
        );

        binding.recyclerProdutos.setLayoutManager(new GridLayoutManager(this, 3));
        binding.recyclerProdutos.setAdapter(adapter);
    }

    private void atualizarLista() {
        List<LivroEntity> livros = livroDao.listarLivrosParaVenda();
        adapter.updateList(livros);
    }

    private void configurarBusca() {
        binding.searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String busca = s.toString().trim();

                if (busca.isEmpty()) {
                    // Se a busca estiver vazia, mostra todos os livros
                    atualizarLista();
                } else {
                    // Filtra os livros
                    List<LivroEntity> filtrados = livroDao.buscarLivros(busca);
                    adapter.updateList(filtrados);
                }
            }

            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void afterTextChanged(Editable s) { }
        });
    }

    private void configurarBotoes() {
        binding.btnHome.setOnClickListener(v -> finish());
        binding.btnScan.setOnClickListener(v -> iniciarScanner());
    }

    private void iniciarScanner() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setPrompt("Aponte para o código de barras do livro");
        integrator.setBeepEnabled(true);
        integrator.setOrientationLocked(true); // Mantém em landscape
        integrator.initiateScan();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);

        if (result != null) {
            if (result.getContents() == null) {
                // Usuário cancelou o scan
                Toast.makeText(this, "Leitura cancelada", Toast.LENGTH_SHORT).show();
            } else {
                // Código de barras lido com sucesso
                String codigo = result.getContents();
                buscarLivroPorCodigo(codigo);
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void buscarLivroPorCodigo(String codigo) {
        // Busca o livro no banco de dados
        LivroEntity livro = livroDao.buscarPorCodigo(codigo);

        if (livro != null) {
            // Livro encontrado
            if (livro.esgotado) {
                // Livro está esgotado
                mostrarDialogoLivroEsgotado(livro, codigo);
            } else {
                // Livro disponível - vai para tela de concluir venda
                Toast.makeText(this, "Livro encontrado: " + livro.titulo, Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(this, ConcluirVendaActivity.class);
                intent.putExtra("livroId", livro.id);
                startActivity(intent);
            }
        } else {
            // Livro não encontrado - oferece cadastrar
            mostrarDialogoLivroNaoEncontrado(codigo);
        }
    }

    private void mostrarDialogoLivroEsgotado(LivroEntity livro, String codigo) {
        new AlertDialog.Builder(this)
                .setTitle("Livro Esgotado")
                .setMessage("O livro \"" + livro.titulo + "\" está marcado como esgotado.\n\nDeseja cadastrar um novo exemplar?")
                .setPositiveButton("Sim, Cadastrar", (dialog, which) -> {
                    Intent intent = new Intent(this, CadastrarLivroActivity.class);
                    intent.putExtra("codigo", codigo);
                    startActivity(intent);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void mostrarDialogoLivroNaoEncontrado(String codigo) {
        new AlertDialog.Builder(this)
                .setTitle("Livro Não Encontrado")
                .setMessage("Não foi encontrado nenhum livro com o código:\n" + codigo + "\n\nDeseja cadastrar este livro?")
                .setPositiveButton("Sim, Cadastrar", (dialog, which) -> {
                    Intent intent = new Intent(this, CadastrarLivroActivity.class);
                    intent.putExtra("codigo", codigo);
                    startActivity(intent);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}