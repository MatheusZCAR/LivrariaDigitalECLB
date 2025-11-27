package com.projeto.livrariadigitaleclb.ui.catalogo;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.projeto.livrariadigitaleclb.data.local.AppDatabase;
import com.projeto.livrariadigitaleclb.data.local.dao.LivroDao;
import com.projeto.livrariadigitaleclb.data.local.entity.LivroEntity;
import com.projeto.livrariadigitaleclb.databinding.ActivityCadastrarLivroBinding;

public class CadastrarLivroActivity extends AppCompatActivity {

    private ActivityCadastrarLivroBinding binding;
    private LivroDao livroDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCadastrarLivroBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        livroDao = AppDatabase.getInstance(this).livroDao();

        // Se veio um código do scanner, já preenche
        String codigoExtra = getIntent().getStringExtra("codigo");
        if (codigoExtra != null) {
            binding.edtCodigoBarras.setText(codigoExtra);
        }

        binding.btnLerCodigo.setOnClickListener(v -> iniciarScanner());
        binding.btnSalvarLivro.setOnClickListener(v -> salvarLivro());
    }

    private void iniciarScanner() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setPrompt("Aponte para o código de barras");
        integrator.setBeepEnabled(true);
        integrator.initiateScan();
    }

    private void salvarLivro() {
        String titulo = binding.edtTitulo.getText().toString().trim();
        String autor = binding.edtAutor.getText().toString().trim();
        String codigo = binding.edtCodigoBarras.getText().toString().trim();
        String precoStr = binding.edtPreco.getText().toString().trim();

        if (TextUtils.isEmpty(titulo) ||
                TextUtils.isEmpty(autor) ||
                TextUtils.isEmpty(codigo) ||
                TextUtils.isEmpty(precoStr)) {

            Toast.makeText(this, "Preencha todos os campos!", Toast.LENGTH_SHORT).show();
            return;
        }

        double preco;
        try {
            preco = Double.parseDouble(precoStr.replace(',', '.'));
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Preço inválido!", Toast.LENGTH_SHORT).show();
            return;
        }

        LivroEntity livro = new LivroEntity();
        livro.titulo = titulo;
        livro.autor = autor;
        livro.codigoBarras = codigo;
        livro.preco = preco;
        livro.esgotado = false;

        livroDao.inserirLivro(livro);

        Toast.makeText(this, "Livro cadastrado!", Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable android.content.Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);

        if (result != null && result.getContents() != null) {
            binding.edtCodigoBarras.setText(result.getContents());
        }

        super.onActivityResult(requestCode, resultCode, data);
    }
}
