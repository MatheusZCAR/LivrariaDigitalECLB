package com.projeto.livrariadigitaleclb;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.projeto.livrariadigitaleclb.data.AppDatabase;
import com.projeto.livrariadigitaleclb.databinding.ActivityRealizarVendaBinding;

import java.util.List;

public class RealizarVendaActivity extends AppCompatActivity {

    private ProdutoAdapter adapter;
    private LivroDao livroDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityRealizarVendaBinding binding = ActivityRealizarVendaBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        livroDao = AppDatabase.getInstance(this).livroDao();

        List<LivroEntity> livros = livroDao.listarLivrosParaVenda();

        adapter = new ProdutoAdapter(livros, livro -> {

            Intent intent = new Intent(this, ConcluirVendaActivity.class);
            intent.putExtra("livroId", livro.id);
            startActivity(intent);

        }, this);

        binding.recyclerProdutos.setLayoutManager(new GridLayoutManager(this, 3));
        binding.recyclerProdutos.setAdapter(adapter);

        configurarBusca(binding);

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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);

        if (result != null && result.getContents() != null) {

            String codigo = result.getContents();
            LivroEntity livro = livroDao.buscarPorCodigo(codigo);

            if (livro != null) {
                // Livro encontrado → abrir tela de venda
                Intent intent = new Intent(this, ConcluirVendaActivity.class);
                intent.putExtra("livroId", livro.id);
                startActivity(intent);
            } else {
                // Livro não existe → cadastrar
                Intent intent = new Intent(this, CadastrarLivroActivity.class);
                intent.putExtra("codigo", codigo);
                startActivity(intent);
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }




    private void configurarBusca(ActivityRealizarVendaBinding binding) {
        binding.searchEditText.addTextChangedListener(new TextWatcher() {
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                List<LivroEntity> filtrados = livroDao.buscarLivros(s.toString());
                adapter.updateList(filtrados);
            }
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {}
        });
    }
}
