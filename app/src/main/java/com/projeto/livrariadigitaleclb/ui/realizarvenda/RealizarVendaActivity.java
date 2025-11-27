package com.projeto.livrariadigitaleclb.ui.realizarvenda;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;

import androidx.annotation.Nullable;
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

    private void configurarBusca() {
        binding.searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                List<LivroEntity> filtrados = livroDao.buscarLivros(s.toString());
                adapter.updateList(filtrados);
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
        integrator.setPrompt("Aponte para o código de barras");
        integrator.setBeepEnabled(true);
        integrator.initiateScan();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);

        if (result != null && result.getContents() != null) {
            String codigo = result.getContents();
            LivroEntity livro = livroDao.buscarPorCodigo(codigo);

            if (livro != null) {
                Intent intent = new Intent(this, ConcluirVendaActivity.class);
                intent.putExtra("livroId", livro.id);
                startActivity(intent);
            } else {
                Intent intent = new Intent(this, CadastrarLivroActivity.class);
                intent.putExtra("codigo", codigo);
                startActivity(intent);
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }
}
