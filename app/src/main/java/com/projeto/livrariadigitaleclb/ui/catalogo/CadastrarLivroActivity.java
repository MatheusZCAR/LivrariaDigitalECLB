package com.projeto.livrariadigitaleclb.ui.catalogo;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import com.projeto.livrariadigitaleclb.App;
import com.projeto.livrariadigitaleclb.R;
import com.projeto.livrariadigitaleclb.backup.BackupAgendador;
import com.projeto.livrariadigitaleclb.data.Carrinho;
import com.projeto.livrariadigitaleclb.data.local.AppDatabase;
import com.projeto.livrariadigitaleclb.data.local.entity.EstoqueEntity;
import com.projeto.livrariadigitaleclb.data.local.entity.LivroEntity;
import com.projeto.livrariadigitaleclb.databinding.ActivityCadastrarLivroBinding;
import com.projeto.livrariadigitaleclb.ui.BaseActivity;
import com.projeto.livrariadigitaleclb.util.Avisos;
import com.projeto.livrariadigitaleclb.util.Capas;
import com.projeto.livrariadigitaleclb.util.Dialogos;
import com.projeto.livrariadigitaleclb.util.Moeda;
import com.projeto.livrariadigitaleclb.util.Texto;

import java.io.File;

public class CadastrarLivroActivity extends BaseActivity {

    public static final String EXTRA_LIVRO_ID = "livro_id";
    public static final String EXTRA_CODIGO = "codigo";
    public static final String EXTRA_MENSAGEM = "mensagem";

    private static final String ESTADO_CAPA = "capa_atual";

    private ActivityCadastrarLivroBinding binding;
    private AppDatabase db;

    private LivroEntity livroExistente;
    /** Caminho da capa que aparece na tela agora (pode ainda não estar salvo no banco). */
    @Nullable private String capaAtual;
    private String estadoInicial = "";

    private final ActivityResultLauncher<ScanOptions> leitorCodigo =
            registerForActivityResult(new ScanContract(), resultado -> {
                if (resultado.getContents() != null) {
                    binding.edtCodigoBarras.setText(resultado.getContents().trim());
                    binding.campoCodigo.setError(null);
                }
            });

    private final ActivityResultLauncher<Intent> camera =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), resultado -> {
                if (resultado.getResultCode() == RESULT_OK && resultado.getData() != null) {
                    String caminho = resultado.getData().getStringExtra(CameraXCaptureActivity.EXTRA_IMAGE_PATH);
                    if (caminho != null) processarFoto(new File(caminho), true);
                }
            });

    private final ActivityResultLauncher<PickVisualMediaRequest> galeria =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) processarFoto(uri);
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCadastrarLivroBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        db = AppDatabase.getInstance(this);

        int livroId = getIntent().getIntExtra(EXTRA_LIVRO_ID, -1);
        if (livroId != -1) {
            livroExistente = db.livroDao().getLivroById(livroId);
            if (livroExistente == null) {
                Avisos.erro(this, "Livro não encontrado", "Este livro não existe mais no catálogo.");
                finish();
                return;
            }
        }

        if (livroExistente != null) {
            definirTitulo(getString(R.string.editar_livro));
            if (savedInstanceState == null) preencherCampos();
            binding.btnExcluirLivro.setVisibility(View.VISIBLE);
        } else {
            definirTitulo(getString(R.string.cadastrar_livro));
            if (savedInstanceState == null) {
                String codigo = getIntent().getStringExtra(EXTRA_CODIGO);
                if (codigo != null) binding.edtCodigoBarras.setText(codigo);
                binding.edtQuantidadeEstoque.setText("1");
            }
        }

        capaAtual = savedInstanceState != null
                ? savedInstanceState.getString(ESTADO_CAPA)
                : (livroExistente != null ? livroExistente.imagemPath : null);
        mostrarCapa();
        estadoInicial = savedInstanceState != null ? savedInstanceState.getString("estado_inicial", "") : estadoAtual();

        boolean temCamera = getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY);
        binding.btnTirarFoto.setVisibility(temCamera ? View.VISIBLE : View.GONE);
        binding.btnLerCodigo.setVisibility(temCamera ? View.VISIBLE : View.GONE);

        binding.btnLerCodigo.setOnClickListener(v -> lerCodigo());
        binding.btnTirarFoto.setOnClickListener(v -> camera.launch(new Intent(this, CameraXCaptureActivity.class)));
        binding.btnGaleria.setOnClickListener(v -> galeria.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build()));
        binding.btnRemoverFoto.setOnClickListener(v -> {
            descartarCapaNaoSalva();
            capaAtual = null;
            mostrarCapa();
        });
        binding.btnMaisEstoque.setOnClickListener(v -> mudarEstoque(+1));
        binding.btnMenosEstoque.setOnClickListener(v -> mudarEstoque(-1));
        binding.btnSalvarLivro.setOnClickListener(v -> validarESalvar());
        binding.btnExcluirLivro.setOnClickListener(v -> confirmarExclusao());

        limparErroAoDigitar(binding.campoTitulo);
        limparErroAoDigitar(binding.campoPreco);
        limparErroAoDigitar(binding.campoQuantidade);
        limparErroAoDigitar(binding.campoCodigo);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                sairComConfirmacao();
            }
        });
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ESTADO_CAPA, capaAtual);
        outState.putString("estado_inicial", estadoInicial);
    }

    private void preencherCampos() {
        binding.edtTitulo.setText(livroExistente.titulo);
        binding.edtAutor.setText(livroExistente.autor);
        binding.edtLocalizacao.setText(livroExistente.localizacao);
        binding.edtCodigoBarras.setText(livroExistente.codigoBarras);
        binding.edtPreco.setText(Moeda.formatarSemSimbolo(livroExistente.preco));
        EstoqueEntity estoque = db.estoqueDao().buscarPorLivro(livroExistente.id);
        binding.edtQuantidadeEstoque.setText(String.valueOf(estoque == null ? 0 : estoque.quantidadeDisponivel));
    }

    private String texto(com.google.android.material.textfield.TextInputEditText campo) {
        return campo.getText() == null ? "" : campo.getText().toString().trim();
    }

    private String estadoAtual() {
        return texto(binding.edtTitulo) + "|" + texto(binding.edtAutor) + "|" + texto(binding.edtPreco)
                + "|" + texto(binding.edtQuantidadeEstoque) + "|" + texto(binding.edtCodigoBarras)
                + "|" + texto(binding.edtLocalizacao) + "|" + capaAtual;
    }

    private void limparErroAoDigitar(TextInputLayout campo) {
        if (campo.getEditText() == null) return;
        campo.getEditText().addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { campo.setError(null); }
            @Override public void afterTextChanged(Editable s) { }
        });
    }

    private void mudarEstoque(int delta) {
        Integer atual = Texto.inteiro(texto(binding.edtQuantidadeEstoque));
        int novo = Math.max(0, Math.min(99999, (atual == null ? 0 : atual) + delta));
        binding.edtQuantidadeEstoque.setText(String.valueOf(novo));
    }

    private void lerCodigo() {
        ScanOptions opcoes = new ScanOptions();
        opcoes.setPrompt("Aponte a câmera para o código de barras do livro");
        opcoes.setBeepEnabled(true);
        opcoes.setOrientationLocked(false);
        leitorCodigo.launch(opcoes);
    }

    // ---------------------------------------------------------------- foto

    private void processarFoto(Uri uri) {
        mostrarProcessandoFoto(true);
        App.SEGUNDO_PLANO.execute(() -> {
            File otimizada = Capas.salvarOtimizada(this, uri);
            runOnUiThread(() -> fotoPronta(otimizada));
        });
    }

    private void processarFoto(File original, boolean apagarOriginal) {
        mostrarProcessandoFoto(true);
        App.SEGUNDO_PLANO.execute(() -> {
            File otimizada = Capas.salvarOtimizada(this, original);
            if (apagarOriginal) Capas.apagar(original.getAbsolutePath());
            runOnUiThread(() -> fotoPronta(otimizada));
        });
    }

    private void mostrarProcessandoFoto(boolean processando) {
        binding.btnTirarFoto.setEnabled(!processando);
        binding.btnGaleria.setEnabled(!processando);
        binding.btnTirarFoto.setText(processando ? "Preparando foto…" : getString(R.string.tirar_foto));
    }

    private void fotoPronta(@Nullable File otimizada) {
        if (isDestroyed()) {
            if (otimizada != null) Capas.apagar(otimizada.getAbsolutePath());
            return;
        }
        mostrarProcessandoFoto(false);
        if (otimizada == null) {
            Avisos.erro(this, "Não deu certo", "Não foi possível usar esta foto. Tente tirar outra.");
            return;
        }
        descartarCapaNaoSalva();
        capaAtual = otimizada.getAbsolutePath();
        mostrarCapa();
    }

    private void mostrarCapa() {
        Capas.carregar(binding.imgPreviewCapa, capaAtual);
        boolean temFoto = capaAtual != null && new File(capaAtual).exists();
        binding.btnRemoverFoto.setVisibility(temFoto ? View.VISIBLE : View.GONE);
        binding.btnTirarFoto.setText(temFoto ? "Tirar outra foto" : getString(R.string.tirar_foto));
    }

    /** Apaga a foto que foi tirada agora mas ainda não foi salva no livro. */
    private void descartarCapaNaoSalva() {
        String salva = livroExistente != null ? livroExistente.imagemPath : null;
        if (capaAtual != null && !capaAtual.equals(salva)) {
            Capas.apagar(capaAtual);
        }
    }

    // ---------------------------------------------------------------- salvar

    private void validarESalvar() {
        String titulo = texto(binding.edtTitulo);
        String codigo = texto(binding.edtCodigoBarras);
        Double preco = Moeda.interpretar(texto(binding.edtPreco));
        Integer quantidade = Texto.inteiro(texto(binding.edtQuantidadeEstoque));

        View primeiroErro = null;
        if (titulo.isEmpty()) {
            binding.campoTitulo.setError("Escreva o nome do livro.");
            primeiroErro = binding.campoTitulo;
        }
        if (preco == null) {
            binding.campoPreco.setError(texto(binding.edtPreco).isEmpty()
                    ? "Escreva o preço."
                    : "Preço inválido. Use só números, por exemplo 25,90.");
            if (primeiroErro == null) primeiroErro = binding.campoPreco;
        }
        if (quantidade == null) {
            binding.campoQuantidade.setError("Escreva quantos exemplares há (pode ser 0).");
            if (primeiroErro == null) primeiroErro = binding.campoQuantidade;
        }
        if (primeiroErro != null) {
            final View alvo = primeiroErro;
            binding.rolagem.post(() -> binding.rolagem.smoothScrollTo(0, Math.max(0, alvo.getTop() - 40)));
            alvo.requestFocus();
            return;
        }

        if (!codigo.isEmpty()) {
            LivroEntity outro = db.livroDao().buscarPorCodigoExceto(codigo,
                    livroExistente != null ? livroExistente.id : -1);
            if (outro != null) {
                binding.campoCodigo.setError("Este código já pertence a outro livro.");
                new MaterialAlertDialogBuilder(this)
                        .setTitle("Código já cadastrado")
                        .setMessage("O código " + codigo + " já pertence ao livro \"" + outro.titulo
                                + "\".\n\nCada livro precisa ter um código diferente.")
                        .setPositiveButton("Abrir esse livro", (d, w) -> {
                            Intent i = new Intent(this, CadastrarLivroActivity.class);
                            i.putExtra(EXTRA_LIVRO_ID, outro.id);
                            startActivity(i);
                            descartarCapaNaoSalva();
                            finish();
                        })
                        .setNegativeButton(R.string.voltar, null)
                        .show();
                return;
            }
        }

        if (preco == 0) {
            Dialogos.confirmar(this, "Preço zerado",
                    "O preço está em R$ 0,00. O livro será vendido de graça. Está certo?",
                    "Sim, está certo", "Corrigir o preço",
                    () -> salvar(titulo, codigo, preco, quantidade));
            return;
        }
        salvar(titulo, codigo, preco, quantidade);
    }

    private void salvar(String titulo, String codigo, double preco, int quantidade) {
        long agora = System.currentTimeMillis();
        LivroEntity livro = livroExistente != null ? livroExistente : new LivroEntity();
        String capaAnterior = livroExistente != null ? livroExistente.imagemPath : null;

        livro.titulo = titulo;
        livro.autor = texto(binding.edtAutor);
        livro.localizacao = texto(binding.edtLocalizacao);
        livro.codigoBarras = codigo.isEmpty() ? null : codigo;
        livro.preco = preco;
        livro.imagemPath = capaAtual;

        int id = db.runInTransaction(() -> {
            int livroId;
            if (livroExistente != null) {
                db.livroDao().atualizarLivro(livro);
                livroId = livro.id;
            } else {
                livroId = (int) db.livroDao().inserirLivro(livro);
            }
            EstoqueEntity estoque = new EstoqueEntity();
            estoque.livroId = livroId;
            estoque.quantidadeDisponivel = quantidade;
            estoque.dataAtualizacao = agora;
            db.estoqueDao().salvarEstoque(estoque);
            return livroId;
        });

        // Foto antiga substituída: apaga o arquivo para não ocupar espaço.
        if (capaAnterior != null && !capaAnterior.equals(capaAtual)) Capas.apagar(capaAnterior);

        BackupAgendador.dadosMudaram(this);

        Intent resultado = new Intent();
        resultado.putExtra(EXTRA_LIVRO_ID, id);
        resultado.putExtra(EXTRA_MENSAGEM, livroExistente != null
                ? "Alterações salvas em \"" + titulo + "\"."
                : "\"" + titulo + "\" foi cadastrado.");
        setResult(RESULT_OK, resultado);
        finish();
    }

    // ---------------------------------------------------------------- excluir / sair

    private void confirmarExclusao() {
        if (livroExistente == null) return;
        Dialogos.confirmar(this, "Excluir livro?",
                "\"" + livroExistente.titulo + "\" vai sair do catálogo.\n\n"
                        + "As vendas já feitas continuam nos relatórios.",
                "Sim, excluir", "Não excluir", () -> {
                    db.runInTransaction(() -> {
                        db.estoqueDao().excluirDoLivro(livroExistente.id);
                        db.livroDao().excluirLivro(livroExistente);
                    });
                    descartarCapaNaoSalva();
                    Capas.apagar(livroExistente.imagemPath);
                    Carrinho.get().remover(livroExistente.id);
                    BackupAgendador.dadosMudaram(this);
                    Intent resultado = new Intent();
                    resultado.putExtra(EXTRA_MENSAGEM, "\"" + livroExistente.titulo + "\" foi excluído.");
                    setResult(RESULT_OK, resultado);
                    finish();
                });
    }

    private void sairComConfirmacao() {
        if (estadoAtual().equals(estadoInicial)) {
            finish();
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle("Sair sem salvar?")
                .setMessage("Você fez mudanças que ainda não foram salvas.")
                .setPositiveButton("Salvar agora", (d, w) -> validarESalvar())
                .setNegativeButton("Sair sem salvar", (d, w) -> {
                    descartarCapaNaoSalva();
                    finish();
                })
                .setNeutralButton("Continuar editando", null)
                .show();
    }
}
