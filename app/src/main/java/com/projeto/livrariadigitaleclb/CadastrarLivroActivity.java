package com.projeto.livrariadigitaleclb;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;


public class CadastrarLivroActivity extends AppCompatActivity {

    private EditText edtTitulo, edtAutor, edtPreco, edtCodigo;
    private LivroDao livroDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cadastrar_livro);

        livroDao = AppDatabase.getInstance(this).livroDao();

        edtTitulo = findViewById(R.id.edtTitulo);
        edtAutor = findViewById(R.id.edtAutor);
        edtPreco = findViewById(R.id.edtPreco);
        edtCodigo = findViewById(R.id.edtCodigoBarras);

        Button btnSalvar = findViewById(R.id.btnSalvarLivro);
        Button btnScanner = findViewById(R.id.btnLerCodigo);

        // Ler o código pela câmera
        btnScanner.setOnClickListener(v -> iniciarScanner());

        // Salvar livro no banco
        btnSalvar.setOnClickListener(v -> salvarLivro());
    }

    private void iniciarScanner() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setPrompt("Aponte para o código de barras");
        integrator.setBeepEnabled(true);
        integrator.initiateScan();
    }

    private void salvarLivro() {
        String titulo = edtTitulo.getText().toString();
        String autor = edtAutor.getText().toString();
        String codigo = edtCodigo.getText().toString();
        String precoStr = edtPreco.getText().toString();

        if (titulo.isEmpty() || autor.isEmpty() || codigo.isEmpty() || precoStr.isEmpty()) {
            Toast.makeText(this, "Preencha todos os campos!", Toast.LENGTH_SHORT).show();
            return;
        }

        double preco = Double.parseDouble(precoStr);

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
    protected void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);

        if (result != null && result.getContents() != null) {
            edtCodigo.setText(result.getContents());
        }

        super.onActivityResult(requestCode, resultCode, data);
    }
}
