package com.projeto.livrariadigitaleclb.ui.catalogo;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.projeto.livrariadigitaleclb.data.local.AppDatabase;
import com.projeto.livrariadigitaleclb.data.local.dao.EstoqueDao;
import com.projeto.livrariadigitaleclb.data.local.dao.LivroDao;
import com.projeto.livrariadigitaleclb.data.local.entity.EstoqueEntity;
import com.projeto.livrariadigitaleclb.data.local.entity.LivroEntity;
import com.projeto.livrariadigitaleclb.databinding.ActivityCadastrarLivroBinding;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CadastrarLivroActivity extends AppCompatActivity {

    private static final String TAG = "CadastrarLivro";
    private static final int REQUEST_PICK_IMAGE = 102;
    private static final int REQUEST_CAMERA_X = 201;

    private ActivityCadastrarLivroBinding binding;
    private LivroDao livroDao;
    private EstoqueDao estoqueDao;

    private String currentPhotoPath = null;

    private boolean isEditMode = false;
    private int livroIdEditar = -1;
    private LivroEntity livroExistente = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCadastrarLivroBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        livroDao = AppDatabase.getInstance(this).livroDao();
        estoqueDao = AppDatabase.getInstance(this).estoqueDao();

        binding.btnHome.setOnClickListener(v -> finish());

        livroIdEditar = getIntent().getIntExtra("livro_id", -1);

        if (livroIdEditar != -1) {
            isEditMode = true;
            configurarModoEdicao();
        } else {
            String codigoExtra = getIntent().getStringExtra("codigo");
            if (codigoExtra != null) binding.edtCodigoBarras.setText(codigoExtra);
        }

        binding.btnLerCodigo.setOnClickListener(v -> iniciarScanner());
        binding.btnTirarFoto.setOnClickListener(v -> mostrarOpcoesFoto());
        binding.btnSalvarLivro.setOnClickListener(v -> salvarOuAtualizarLivro());
    }

    private void configurarModoEdicao() {
        binding.textTituloCadastro.setText("Editar Livro");
        binding.btnSalvarLivro.setText("💾 ATUALIZAR");

        livroExistente = livroDao.getLivroById(livroIdEditar);
        EstoqueEntity estoque = estoqueDao.buscarPorLivro(livroIdEditar);

        if (livroExistente == null) {
            Toast.makeText(this, "Erro ao carregar livro", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        binding.edtTitulo.setText(livroExistente.titulo);
        binding.edtAutor.setText(livroExistente.autor);
        binding.edtLocalizacao.setText(livroExistente.localizacao);
        binding.edtCodigoBarras.setText(livroExistente.codigoBarras);
        binding.edtPreco.setText(String.format(Locale.US, "%.2f", livroExistente.preco));

        if (estoque != null)
            binding.edtQuantidadeEstoque.setText(String.valueOf(estoque.quantidadeDisponivel));

        if (livroExistente.imagemPath != null) {
            currentPhotoPath = livroExistente.imagemPath;
            File imgFile = new File(currentPhotoPath);
            if (imgFile.exists())
                binding.imgPreviewCapa.setImageURI(Uri.fromFile(imgFile));
        }
    }

    private void mostrarOpcoesFoto() {
        String[] opcoes = {"Tirar Foto", "Escolher da Galeria"};

        new AlertDialog.Builder(this)
                .setTitle("Adicionar Foto")
                .setItems(opcoes, (dialog, which) -> {
                    if (which == 0) abrirCameraX();
                    else abrirGaleria();
                })
                .show();
    }

    private void abrirCameraX() {
        Intent intent = new Intent(this, CameraXCaptureActivity.class);
        startActivityForResult(intent, REQUEST_CAMERA_X);
    }

    private void abrirGaleria() {
        Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(i, REQUEST_PICK_IMAGE);
    }

    private File criarArquivoImagem() {
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                    .format(new Date());
            File dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

            File f = new File(dir, "LIVRO_" + timeStamp + ".jpg");
            currentPhotoPath = f.getAbsolutePath();
            return f;

        } catch (Exception e) {
            return null;
        }
    }

    private void copiarImagemDaGaleria(Uri srcUri) {
        try {
            File out = criarArquivoImagem();
            InputStream in = getContentResolver().openInputStream(srcUri);
            OutputStream outStream = new FileOutputStream(out);

            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) outStream.write(buf, 0, len);

            outStream.close();
            in.close();

            binding.imgPreviewCapa.setImageURI(Uri.fromFile(out));

        } catch (Exception e) {
            Toast.makeText(this, "Erro ao copiar imagem", Toast.LENGTH_SHORT).show();
        }
    }

    private void iniciarScanner() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setPrompt("Aponte para o código de barras");
        integrator.setBeepEnabled(true);
        integrator.initiateScan();
    }

    private void salvarOuAtualizarLivro() {

        String titulo = binding.edtTitulo.getText().toString().trim();
        String autor = binding.edtAutor.getText().toString().trim();
        String localizacao = binding.edtLocalizacao.getText().toString().trim();
        String codigo = binding.edtCodigoBarras.getText().toString().trim();
        String precoStr = binding.edtPreco.getText().toString().trim();
        String qtdStr = binding.edtQuantidadeEstoque.getText().toString().trim();

        if (titulo.isEmpty() || autor.isEmpty() || precoStr.isEmpty() || qtdStr.isEmpty()) {
            Toast.makeText(this, "Preencha todos os campos obrigatórios", Toast.LENGTH_SHORT).show();
            return;
        }

        double preco = Double.parseDouble(precoStr.replace(",", "."));
        int quantidade = Integer.parseInt(qtdStr);

        if (isEditMode) {
            livroExistente.titulo = titulo;
            livroExistente.autor = autor;
            livroExistente.localizacao = localizacao;
            livroExistente.codigoBarras = codigo;
            livroExistente.preco = preco;

            if (currentPhotoPath != null)
                livroExistente.imagemPath = currentPhotoPath;

            livroDao.atualizarLivro(livroExistente);

            EstoqueEntity estoque = estoqueDao.buscarPorLivro(livroIdEditar);
            if (estoque != null) {
                estoque.quantidadeDisponivel = quantidade;
                estoque.dataAtualizacao = System.currentTimeMillis();
                estoqueDao.atualizarEstoque(estoque);
            }

            Toast.makeText(this, "Livro atualizado!", Toast.LENGTH_SHORT).show();

        } else {
            LivroEntity novo = new LivroEntity();
            novo.titulo = titulo;
            novo.autor = autor;
            novo.localizacao = localizacao;
            novo.codigoBarras = codigo;
            novo.preco = preco;
            novo.imagemPath = currentPhotoPath;

            long newId = livroDao.inserirLivro(novo);

            EstoqueEntity estoque = new EstoqueEntity();
            estoque.livroId = (int) newId;
            estoque.quantidadeDisponivel = quantidade;
            estoque.dataAtualizacao = System.currentTimeMillis();

            estoqueDao.inserirEstoque(estoque);

            Toast.makeText(this, "Livro cadastrado!", Toast.LENGTH_SHORT).show();
        }

        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Código de barras
        if (requestCode == IntentIntegrator.REQUEST_CODE) {
            IntentResult r = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
            if (r != null && r.getContents() != null)
                binding.edtCodigoBarras.setText(r.getContents());
            return;
        }

        // CameraX
        if (requestCode == REQUEST_CAMERA_X && resultCode == RESULT_OK && data != null) {
            String path = data.getStringExtra(CameraXCaptureActivity.EXTRA_IMAGE_PATH);
            if (path != null) {
                currentPhotoPath = path;
                binding.imgPreviewCapa.setImageURI(Uri.fromFile(new File(path)));
            }
            return;
        }

        // Galeria
        if (requestCode == REQUEST_PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            Uri selected = data.getData();
            if (selected != null) copiarImagemDaGaleria(selected);
        }
    }
}
