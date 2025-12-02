package com.projeto.livrariadigitaleclb.ui.catalogo;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CadastrarLivroActivity extends AppCompatActivity {

    private static final String TAG = "CadastrarLivro";
    private static final int REQUEST_CAMERA_PERMISSION = 100;
    private static final int REQUEST_IMAGE_CAPTURE = 101;
    private static final int REQUEST_PICK_IMAGE = 102;

    private ActivityCadastrarLivroBinding binding;
    private LivroDao livroDao;
    private EstoqueDao estoqueDao;

    private String currentPhotoPath;
    private Uri photoUri;
    private int livroIdEditar = -1;
    private LivroEntity livroExistente = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCadastrarLivroBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Log.d(TAG, "onCreate chamado");
        binding.btnHome.setOnClickListener(v -> finish());
        AppDatabase db = AppDatabase.getInstance(this);
        livroDao = db.livroDao();
        estoqueDao = db.estoqueDao();
        livroIdEditar = getIntent().getIntExtra("livro_id", -1);

        if (livroIdEditar != -1) {
            configurarModoEdicao();
        } else {
            String codigoExtra = getIntent().getStringExtra("codigo");
            if (codigoExtra != null) {
                binding.edtCodigoBarras.setText(codigoExtra);
            }
        }

        binding.btnLerCodigo.setOnClickListener(v -> iniciarScanner());
        binding.btnTirarFoto.setOnClickListener(v -> {
            Log.d(TAG, "Botão Tirar Foto clicado");
            mostrarOpcoesFoto();
        });

        binding.btnSalvarLivro.setOnClickListener(v -> salvarOuAtualizarLivro());
    }

    private void configurarModoEdicao() {
        binding.textTituloCadastro.setText("Editar Livro");
        binding.btnSalvarLivro.setText("💾 ATUALIZAR LIVRO");

        // Carrega os dados do livro e do estoque
        livroExistente = livroDao.getLivroById(livroIdEditar);
        EstoqueEntity estoque = estoqueDao.buscarPorLivro(livroIdEditar);

        if (livroExistente != null) {
            binding.edtTitulo.setText(livroExistente.titulo);
            binding.edtAutor.setText(livroExistente.autor);

            // Carrega localização (se existir)
            if (livroExistente.localizacao != null && !livroExistente.localizacao.equals("Não informado")) {
                binding.edtLocalizacao.setText(livroExistente.localizacao);
            }

            binding.edtCodigoBarras.setText(livroExistente.codigoBarras);
            binding.edtPreco.setText(String.format(Locale.US, "%.2f", livroExistente.preco));

            if (estoque != null) {
                binding.edtQuantidadeEstoque.setText(String.valueOf(estoque.quantidadeDisponivel));
            }

            if (livroExistente.imagemPath != null) {
                currentPhotoPath = livroExistente.imagemPath;
                File imgFile = new File(currentPhotoPath);
                if (imgFile.exists()) {
                    binding.imgPreviewCapa.setImageURI(Uri.fromFile(imgFile));
                }
            }
        }
    }

    private void mostrarOpcoesFoto() {
        String[] opcoes = {"Tirar Foto", "Escolher da Galeria"};

        new AlertDialog.Builder(this)
                .setTitle("Adicionar Foto da Capa")
                .setItems(opcoes, (dialog, which) -> {
                    if (which == 0) {
                        verificarPermissaoCamera();
                    } else {
                        abrirGaleria();
                    }
                })
                .show();
    }

    private void verificarPermissaoCamera() {
        Log.d(TAG, "Verificando permissão da câmera");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Permissão não concedida, solicitando...");
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA},
                        REQUEST_CAMERA_PERMISSION);
            } else {
                Log.d(TAG, "Permissão já concedida");
                abrirCamera();
            }
        } else {
            abrirCamera();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        Log.d(TAG, "onRequestPermissionsResult - requestCode: " + requestCode);

        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Permissão concedida pelo usuário");
                abrirCamera();
            } else {
                Log.d(TAG, "Permissão negada pelo usuário");
                Toast.makeText(this, "Permissão de câmera negada", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void abrirCamera() {
        Log.d(TAG, "Tentando abrir câmera");

        try {
            File photoFile = criarArquivoImagem();

            if (photoFile == null) {
                Log.e(TAG, "Erro ao criar arquivo de imagem");
                Toast.makeText(this, "Erro ao criar arquivo de imagem", Toast.LENGTH_SHORT).show();
                return;
            }

            Log.d(TAG, "Arquivo criado: " + photoFile.getAbsolutePath());

            photoUri = FileProvider.getUriForFile(this,
                    "com.projeto.livrariadigitaleclb.fileprovider",
                    photoFile);

            Log.d(TAG, "URI criada: " + photoUri.toString());

            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            takePictureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            takePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                Log.d(TAG, "Iniciando activity da câmera");
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            } else {
                Log.e(TAG, "Nenhum app de câmera encontrado");
                Toast.makeText(this, "Nenhum app de câmera disponível. Tente escolher da galeria.",
                        Toast.LENGTH_LONG).show();
            }

        } catch (Exception e) {
            Log.e(TAG, "Erro ao abrir câmera: " + e.getMessage(), e);
            Toast.makeText(this, "Erro: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void abrirGaleria() {
        Log.d(TAG, "Abrindo galeria");
        Intent pickPhoto = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(pickPhoto, REQUEST_PICK_IMAGE);
    }

    private File criarArquivoImagem() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss",
                Locale.getDefault()).format(new Date());
        String imageFileName = "LIVRO_" + timeStamp;

        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        if (storageDir == null) {
            Log.e(TAG, "Diretório de armazenamento é null");
            return null;
        }

        if (!storageDir.exists()) {
            boolean created = storageDir.mkdirs();
            Log.d(TAG, "Diretório criado: " + created);
        }

        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        currentPhotoPath = image.getAbsolutePath(); // Atualiza o caminho atual
        Log.d(TAG, "Caminho da foto: " + currentPhotoPath);

        return image;
    }

    private void copiarImagemDaGaleria(Uri sourceUri) {
        try {
            File photoFile = criarArquivoImagem();
            if (photoFile == null) {
                Toast.makeText(this, "Erro ao criar arquivo", Toast.LENGTH_SHORT).show();
                return;
            }

            InputStream inputStream = getContentResolver().openInputStream(sourceUri);
            OutputStream outputStream = new FileOutputStream(photoFile);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            inputStream.close();
            outputStream.close();

            Log.d(TAG, "Imagem copiada para: " + currentPhotoPath);
            binding.imgPreviewCapa.setImageURI(Uri.fromFile(photoFile));
            Toast.makeText(this, "Foto selecionada!", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Log.e(TAG, "Erro ao copiar imagem: " + e.getMessage(), e);
            Toast.makeText(this, "Erro ao processar imagem", Toast.LENGTH_SHORT).show();
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
        String quantidadeStr = binding.edtQuantidadeEstoque.getText().toString().trim();

        if (TextUtils.isEmpty(titulo) || TextUtils.isEmpty(autor) ||
                TextUtils.isEmpty(precoStr) || TextUtils.isEmpty(quantidadeStr)) {
            Toast.makeText(this, "Preencha todos os campos obrigatórios", Toast.LENGTH_SHORT).show();
            return;
        }

        double preco;
        int quantidade;
        try {
            preco = Double.parseDouble(precoStr.replace(",", "."));
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Preço inválido", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            quantidade = Integer.parseInt(quantidadeStr);
            if (quantidade < 0) {
                Toast.makeText(this, "Quantidade não pode ser negativa", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Quantidade inválida", Toast.LENGTH_SHORT).show();
            return;
        }

        if (livroIdEditar != -1 && livroExistente != null) {
            // ATUALIZAR (UPDATE)
            livroExistente.titulo = titulo;
            livroExistente.autor = autor;
            livroExistente.localizacao = localizacao.isEmpty() ? "Não informado" : localizacao;
            livroExistente.codigoBarras = codigo;
            livroExistente.preco = preco;

            // Só atualiza a imagem se o usuário tiver tirado/selecionado uma nova
            // Se currentPhotoPath mudou em relação ao original, atualiza
            if (currentPhotoPath != null && !currentPhotoPath.equals(livroExistente.imagemPath)) {
                livroExistente.imagemPath = currentPhotoPath;
            }

            // Se a imagemPath for nula no objeto mas tivermos um path atual, atribui
            if (livroExistente.imagemPath == null && currentPhotoPath != null) {
                livroExistente.imagemPath = currentPhotoPath;
            }

            livroDao.atualizarLivro(livroExistente);

            // Atualiza Estoque
            EstoqueEntity estoque = estoqueDao.buscarPorLivro(livroIdEditar);
            if (estoque != null) {
                estoque.quantidadeDisponivel = quantidade;
                estoque.dataAtualizacao = System.currentTimeMillis();
                estoqueDao.atualizarEstoque(estoque);
            } else {
                // Caso raro onde o estoque não existia
                EstoqueEntity novoEstoque = new EstoqueEntity();
                novoEstoque.livroId = livroIdEditar;
                novoEstoque.quantidadeDisponivel = quantidade;
                novoEstoque.dataAtualizacao = System.currentTimeMillis();
                estoqueDao.inserirEstoque(novoEstoque);
            }

            Toast.makeText(this, "Livro atualizado com sucesso!", Toast.LENGTH_SHORT).show();

        } else {
            // CADASTRAR (INSERT)
            LivroEntity novoLivro = new LivroEntity();
            novoLivro.titulo = titulo;
            novoLivro.autor = autor;
            novoLivro.preco = preco;
            novoLivro.codigoBarras = codigo;
            novoLivro.localizacao = localizacao.isEmpty() ? "Não informado" : localizacao;
            novoLivro.imagemPath = currentPhotoPath;

            long livroIdGerado = livroDao.inserirLivro(novoLivro);

            EstoqueEntity estoque = new EstoqueEntity();
            estoque.livroId = (int) livroIdGerado;
            estoque.quantidadeDisponivel = quantidade;
            estoque.dataAtualizacao = System.currentTimeMillis();
            estoqueDao.inserirEstoque(estoque);

            Toast.makeText(this, "Livro cadastrado com sucesso!", Toast.LENGTH_SHORT).show();
        }

        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.d(TAG, "onActivityResult - requestCode: " + requestCode + ", resultCode: " + resultCode);

        // Scanner de código de barras
        if (requestCode == IntentIntegrator.REQUEST_CODE) {
            IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
            if (result != null && result.getContents() != null) {
                binding.edtCodigoBarras.setText(result.getContents());
                Log.d(TAG, "Código lido: " + result.getContents());
            }
        }

        // Câmera
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Log.d(TAG, "Foto capturada!");

            if (currentPhotoPath != null) {
                File imgFile = new File(currentPhotoPath);
                if (imgFile.exists()) {
                    Log.d(TAG, "Arquivo existe: " + imgFile.length() + " bytes");
                    binding.imgPreviewCapa.setImageURI(Uri.fromFile(imgFile));
                    Toast.makeText(this, "Foto capturada!", Toast.LENGTH_SHORT).show();
                } else {
                    Log.e(TAG, "Arquivo não existe");
                }
            }
        }

        // Galeria
        if (requestCode == REQUEST_PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            Uri selectedImage = data.getData();
            if (selectedImage != null) {
                Log.d(TAG, "Imagem selecionada da galeria: " + selectedImage);
                copiarImagemDaGaleria(selectedImage);
            }
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (currentPhotoPath != null) {
            outState.putString("photo_path", currentPhotoPath);
        }
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState.containsKey("photo_path")) {
            currentPhotoPath = savedInstanceState.getString("photo_path");
            if (currentPhotoPath != null) {
                File imgFile = new File(currentPhotoPath);
                if (imgFile.exists()) {
                    binding.imgPreviewCapa.setImageURI(Uri.fromFile(imgFile));
                }
            }
        }
    }
}
