package com.projeto.livrariadigitaleclb;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.projeto.livrariadigitaleclb.data.AppDatabase;


public class ConcluirVendaActivity extends AppCompatActivity {

    private LivroDao livroDao;
    private VendaDao vendaDao;
    private LivroEntity livro;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_concluir_venda);

        livroDao = AppDatabase.getInstance(this).livroDao();
        vendaDao = AppDatabase.getInstance(this).vendaDao();

        int livroId = getIntent().getIntExtra("livroId", -1);
        livro = livroDao.getLivroById(livroId);

        preencherDados();

        findViewById(R.id.btnCancelar).setOnClickListener(v -> finish());

        findViewById(R.id.btnConfirmar).setOnClickListener(v -> registrarVenda());
    }

    private void preencherDados() {
        TextView info = findViewById(R.id.textInfoLivro);
        info.setText(
                "Título: " + livro.titulo +
                        "\nAutor: " + livro.autor +
                        "\nPreço: R$ " + livro.preco
        );
    }

    private void registrarVenda() {
        Venda venda = new Venda();
        venda.livroId = livro.id;
        venda.titulo = livro.titulo;
        venda.preco = livro.preco;
        venda.dataVenda = System.currentTimeMillis();

        vendaDao.registrarVenda(venda);

        Toast.makeText(this, "Venda registrada!", Toast.LENGTH_SHORT).show();
        finish();
    }
}


