package com.projeto.livrariadigitaleclb.ui.concluirvenda;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.projeto.livrariadigitaleclb.data.local.AppDatabase;
import com.projeto.livrariadigitaleclb.data.local.dao.LivroDao;
import com.projeto.livrariadigitaleclb.data.local.dao.VendaDao;
import com.projeto.livrariadigitaleclb.data.local.entity.LivroEntity;
import com.projeto.livrariadigitaleclb.data.local.entity.Venda;
import com.projeto.livrariadigitaleclb.databinding.ActivityConcluirVendaBinding;

import java.util.ArrayList;
import java.util.List;

public class ConcluirVendaActivity extends AppCompatActivity {

    private ActivityConcluirVendaBinding binding;
    private LivroDao livroDao;
    private VendaDao vendaDao;

    // Agora trabalhamos com uma LISTA de livros
    private List<LivroEntity> livrosSelecionados = new ArrayList<>();
    private double valorTotal = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityConcluirVendaBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        AppDatabase db = AppDatabase.getInstance(this);
        livroDao = db.livroDao();
        vendaDao = db.vendaDao();

        // Recebe a lista de IDs
        ArrayList<Integer> ids = getIntent().getIntegerArrayListExtra("listaLivrosIds");

        if (ids == null || ids.isEmpty()) {
            Toast.makeText(this, "Nenhum livro selecionado.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        carregarLivros(ids);
        preencherDados();

        binding.btnCancelar.setOnClickListener(v -> finish());
        binding.btnConfirmar.setOnClickListener(v -> registrarVenda());
    }

    private void carregarLivros(ArrayList<Integer> ids) {
        // Se você criou o método no DAO conforme a dica 1:
        // livrosSelecionados = livroDao.obterLivrosPorId(ids);

        // Se NÃO criou o método no DAO, use este loop (menos eficiente, mas funciona agora):
        for (int id : ids) {
            LivroEntity l = livroDao.getLivroById(id);
            if (l != null) {
                livrosSelecionados.add(l);
            }
        }
    }

    private void preencherDados() {
        StringBuilder resumo = new StringBuilder();
        valorTotal = 0.0;

        for (LivroEntity livro : livrosSelecionados) {
            resumo.append("- ").append(livro.titulo).append(" (R$ ").append(livro.preco).append(")\n");
            valorTotal += livro.preco; // Certifique-se que preço é double ou float
        }

        binding.textInfoLivro.setText(resumo.toString());

        // Se tiver um TextView para o total, atualize ele aqui também:
        // binding.textValorTotal.setText("Total: R$ " + valorTotal);
    }

    private void registrarVenda() {
        if (livrosSelecionados.isEmpty()) return;

        long dataAtual = System.currentTimeMillis();

        // Salva uma venda para cada livro (mantendo compatibilidade com seu banco atual)
        for (LivroEntity livro : livrosSelecionados) {
            Venda venda = new Venda();
            venda.livroId = livro.id;
            venda.titulo = livro.titulo;
            venda.preco = livro.preco;
            venda.dataVenda = dataAtual;

            vendaDao.registrarVenda(venda);
        }

        Toast.makeText(this, "Venda de " + livrosSelecionados.size() + " itens realizada!", Toast.LENGTH_SHORT).show();

        // Retorna para a tela principal e limpa a pilha
        // Intent intent = new Intent(this, MainActivity.class);
        // intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        // startActivity(intent);
        finish();
    }
}