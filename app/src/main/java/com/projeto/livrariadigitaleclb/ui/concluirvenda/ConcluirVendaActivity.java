package com.projeto.livrariadigitaleclb.ui.concluirvenda;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.projeto.livrariadigitaleclb.data.local.AppDatabase;
import com.projeto.livrariadigitaleclb.data.local.dao.LivroDao;
import com.projeto.livrariadigitaleclb.data.local.dao.VendaDao;
import com.projeto.livrariadigitaleclb.data.local.entity.LivroEntity;
import com.projeto.livrariadigitaleclb.data.local.entity.Venda;
import com.projeto.livrariadigitaleclb.databinding.ActivityConcluirVendaBinding;

public class ConcluirVendaActivity extends AppCompatActivity {

    private ActivityConcluirVendaBinding binding;

    private LivroDao livroDao;
    private VendaDao vendaDao;
    @Nullable
    private LivroEntity livro;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityConcluirVendaBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        AppDatabase db = AppDatabase.getInstance(this);
        livroDao = db.livroDao();
        vendaDao = db.vendaDao();

        int livroId = getIntent().getIntExtra("livroId", -1);
        if (livroId == -1) {
            Toast.makeText(this, "Livro inválido.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        livro = livroDao.getLivroById(livroId);
        if (livro == null) {
            Toast.makeText(this, "Livro não encontrado.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        preencherDados();

        binding.btnCancelar.setOnClickListener(v -> finish());
        binding.btnConfirmar.setOnClickListener(v -> registrarVenda());
    }

    private void preencherDados() {
        if (livro == null) return;

        String info = "Título: " + livro.titulo +
                "\nAutor: " + livro.autor +
                "\nPreço: R$ " + livro.preco;

        binding.textInfoLivro.setText(info);
    }

    private void registrarVenda() {
        if (livro == null) {
            Toast.makeText(this, "Erro ao registrar venda.", Toast.LENGTH_SHORT).show();
            return;
        }

        Venda venda = new Venda();
        venda.livroId = livro.id;
        venda.titulo = livro.titulo;
        venda.preco = livro.preco;
        venda.dataVenda = System.currentTimeMillis();

        vendaDao.registrarVenda(venda);

        // Futuro: aqui dá pra atualizar Estoque e criar ItemVendaEntity
        // AppDatabase.getInstance(this).estoqueDao()...

        Toast.makeText(this, "Venda registrada!", Toast.LENGTH_SHORT).show();
        finish();
    }
}
