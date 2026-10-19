package com.projeto.livrariadigitaleclb.ui.catalogo;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.projeto.livrariadigitaleclb.databinding.ActivityCameraXCaptureBinding;
import com.projeto.livrariadigitaleclb.ui.BaseActivity;
import com.projeto.livrariadigitaleclb.util.Avisos;
import com.projeto.livrariadigitaleclb.util.Capas;

import java.io.File;

/** Câmera para fotografar a capa do livro. Devolve o caminho da foto em EXTRA_IMAGE_PATH. */
public class CameraXCaptureActivity extends BaseActivity {

    private static final String TAG = "CameraXCapture";
    public static final String EXTRA_IMAGE_PATH = "image_path";

    private ActivityCameraXCaptureBinding binding;
    private ImageCapture imageCapture;

    private final ActivityResultLauncher<String> pedirPermissao =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), concedida -> {
                if (concedida) {
                    iniciarCamera();
                } else {
                    new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                            .setTitle("Câmera não liberada")
                            .setMessage("Para tirar a foto da capa, o app precisa usar a câmera.\n\n"
                                    + "Você pode liberar em Configurações > Apps > Livraria > Permissões, "
                                    + "ou escolher uma foto da galeria.")
                            .setPositiveButton(com.projeto.livrariadigitaleclb.R.string.entendi, (d, w) -> finish())
                            .setOnCancelListener(d -> finish())
                            .show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCameraXCaptureBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnCapture.setOnClickListener(v -> tirarFoto());
        binding.btnClose.setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            iniciarCamera();
        } else {
            pedirPermissao.launch(Manifest.permission.CAMERA);
        }
    }

    private void iniciarCamera() {
        ListenableFuture<ProcessCameraProvider> futuro = ProcessCameraProvider.getInstance(this);
        futuro.addListener(() -> {
            try {
                ProcessCameraProvider provedor = futuro.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(binding.previewView.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .setTargetRotation(binding.previewView.getDisplay() != null
                                ? binding.previewView.getDisplay().getRotation()
                                : getWindowManager().getDefaultDisplay().getRotation())
                        .build();

                // Alguns tablets só têm câmera frontal.
                CameraSelector camera = provedor.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)
                        ? CameraSelector.DEFAULT_BACK_CAMERA
                        : CameraSelector.DEFAULT_FRONT_CAMERA;

                provedor.unbindAll();
                provedor.bindToLifecycle(this, camera, preview, imageCapture);
            } catch (Exception e) {
                Log.e(TAG, "Erro ao iniciar câmera", e);
                Avisos.erro(this, "Câmera indisponível",
                        "Não foi possível abrir a câmera. Use \"Escolher da galeria\".");
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void tirarFoto() {
        if (imageCapture == null) {
            Avisos.info(this, "A câmera ainda está abrindo. Aguarde um instante.");
            return;
        }
        File arquivo = Capas.arquivoTemporario(this);
        ImageCapture.OutputFileOptions opcoes = new ImageCapture.OutputFileOptions.Builder(arquivo).build();

        binding.btnCapture.setEnabled(false);
        binding.progresso.setVisibility(View.VISIBLE);

        imageCapture.takePicture(opcoes, ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults resultado) {
                        Intent dados = new Intent();
                        dados.putExtra(EXTRA_IMAGE_PATH, arquivo.getAbsolutePath());
                        setResult(RESULT_OK, dados);
                        finish();
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException e) {
                        Log.e(TAG, "Erro ao salvar foto", e);
                        binding.btnCapture.setEnabled(true);
                        binding.progresso.setVisibility(View.GONE);
                        Avisos.erro(CameraXCaptureActivity.this, "A foto não foi salva",
                                "Tente de novo. Se continuar, use \"Escolher da galeria\".");
                    }
                });
    }
}
