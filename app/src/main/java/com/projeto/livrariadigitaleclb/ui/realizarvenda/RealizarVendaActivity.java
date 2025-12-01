package com.projeto.livrariadigitaleclb.ui.realizarvenda;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;
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

import java.util.ArrayList;
import java.util.List;

public class RealizarVendaActivity extends AppCompatActivity {

    private ActivityRealizarVendaBinding binding;
    private ProdutoAdapter adapter;
    private LivroDao livroDao;

    // LISTA DO CARRINHO (Ids dos livros selecionados)
    private ArrayList<Integer> carrinhoIds = new ArrayList<>();

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
        atualizarBotaoFinalizar(); // Esconde o botão se o carrinho estiver vazio
    }

    private void configurarLista() {
        List<LivroEntity> livros = livroDao.listarLivrosParaVenda();

        adapter = new ProdutoAdapter(
                livros,
                livro -> {
                    // LÓGICA DE SELEÇÃO (Carrinho)
                    if (carrinhoIds.contains(livro.id)) {
                        carrinhoIds.remove((Integer) livro.id); // Remove se já existe
                        Toast.makeText(this, "Item removido", Toast.LENGTH_SHORT).show();
                    } else {
                        carrinhoIds.add(livro.id); // Adiciona se não existe
                        Toast.makeText(this, "Item adicionado", Toast.LENGTH_SHORT).show();
                    }

                    // Opcional: Aqui você deve atualizar a cor do item no adapter para mostrar que foi selecionado
                    // adapter.notifyDataSetChanged();

                    atualizarBotaoFinalizar();
                }
        );

        binding.recyclerProdutos.setLayoutManager(new GridLayoutManager(this, 3));
        binding.recyclerProdutos.setAdapter(adapter);
    }

    // Mostra o botão de finalizar apenas se tiver itens no carrinho
    private void atualizarBotaoFinalizar() {
        if (carrinhoIds.isEmpty()) {
            binding.btnFinalizar.setVisibility(View.GONE); // Certifique-se de ter esse botão no XML
        } else {
            binding.btnFinalizar.setVisibility(View.VISIBLE);
            binding.btnFinalizar.setText("Finalizar (" + carrinhoIds.size() + ")");
        }
    }

    private void configurarBotoes() {
        binding.btnHome.setOnClickListener(v -> finish());
        binding.btnScan.setOnClickListener(v -> iniciarScanner());

        // Novo botão para ir para a próxima tela
        binding.btnFinalizar.setOnClickListener(v -> {
            Intent intent = new Intent(this, ConcluirVendaActivity.class);
            // Passamos a LISTA de IDs agora
            intent.putIntegerArrayListExtra("listaLivrosIds", carrinhoIds);
            startActivity(intent);
        });
    }

    private void iniciarScanner() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setPrompt("Aponte para o código de barras");
        integrator.setBeepEnabled(true);
        integrator.initiateScan();
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

    // ... (Métodos configurarBusca e iniciarScanner permanecem iguais) ...

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);

        if (result != null && result.getContents() != null) {
            String codigo = result.getContents();
            LivroEntity livro = livroDao.buscarPorCodigo(codigo);

            if (livro != null) {
                // Adiciona direto ao carrinho
                if (!carrinhoIds.contains(livro.id)) {
                    carrinhoIds.add(livro.id);
                    Toast.makeText(this, "Livro adicionado ao carrinho!", Toast.LENGTH_SHORT).show();
                    atualizarBotaoFinalizar();
                } else {
                    Toast.makeText(this, "Este livro já está no carrinho.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Intent intent = new Intent(this, CadastrarLivroActivity.class);
                intent.putExtra("codigo", codigo);
                startActivity(intent);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}