package com.projeto.livrariadigitaleclb.ui.backup;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.Formatter;
import android.view.View;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;

import com.google.android.gms.auth.api.identity.AuthorizationResult;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.projeto.livrariadigitaleclb.App;
import com.projeto.livrariadigitaleclb.R;
import com.projeto.livrariadigitaleclb.backup.BackupAgendador;
import com.projeto.livrariadigitaleclb.backup.BackupArquivo;
import com.projeto.livrariadigitaleclb.backup.BackupInvalidoException;
import com.projeto.livrariadigitaleclb.backup.BackupPrefs;
import com.projeto.livrariadigitaleclb.backup.BackupServico;
import com.projeto.livrariadigitaleclb.backup.DriveAuth;
import com.projeto.livrariadigitaleclb.backup.DriveClient;
import com.projeto.livrariadigitaleclb.data.Carrinho;
import com.projeto.livrariadigitaleclb.databinding.ActivityBackupBinding;
import com.projeto.livrariadigitaleclb.ui.BaseActivity;
import com.projeto.livrariadigitaleclb.ui.main.MainActivity;
import com.projeto.livrariadigitaleclb.util.Avisos;
import com.projeto.livrariadigitaleclb.util.Datas;
import com.projeto.livrariadigitaleclb.util.Dialogos;
import com.projeto.livrariadigitaleclb.util.Periodo;
import com.projeto.livrariadigitaleclb.util.Texto;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/** Tela de cópia de segurança: Google Drive e arquivo. */
public class BackupActivity extends BaseActivity {

    public static final String EXTRA_MENSAGEM = "mensagem";

    private interface ComToken {
        void executar(String token);
    }

    private ActivityBackupBinding binding;
    private BackupPrefs prefs;
    private boolean trabalhando = false;
    private ComToken acaoPendente;

    /** Tela do Google para escolher a conta e autorizar o Drive. */
    private final ActivityResultLauncher<IntentSenderRequest> autorizacao =
            registerForActivityResult(new ActivityResultContracts.StartIntentSenderForResult(), r -> {
                ComToken acao = acaoPendente;
                acaoPendente = null;
                if (r.getResultCode() != Activity.RESULT_OK || r.getData() == null) {
                    terminar();
                    Avisos.info(this, "A conexão com o Google Drive foi cancelada.");
                    return;
                }
                try {
                    AuthorizationResult resultado = Identity.getAuthorizationClient(this)
                            .getAuthorizationResultFromIntent(r.getData());
                    if (acao != null) acao.executar(resultado.getAccessToken());
                } catch (Exception e) {
                    terminar();
                    Avisos.erro(this, "Não foi possível conectar", DriveAuth.explicar(e));
                }
            });

    private final ActivityResultLauncher<String> salvarArquivo =
            registerForActivityResult(new ActivityResultContracts.CreateDocument("application/zip"), uri -> {
                if (uri != null) salvarEmArquivo(uri);
            });

    private final ActivityResultLauncher<String[]> abrirArquivo =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) restaurarDeArquivo(uri);
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBackupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        definirTitulo(getString(R.string.backup_tela_titulo));
        prefs = new BackupPrefs(this);

        binding.btnConectar.setOnClickListener(v -> conectar());
        binding.btnBackupAgora.setOnClickListener(v -> fazerBackupNoDrive());
        binding.btnRestaurarDrive.setOnClickListener(v -> escolherCopiaNoDrive());
        binding.btnDesconectar.setOnClickListener(v -> confirmarDesconectar());
        binding.btnSalvarArquivo.setOnClickListener(v ->
                salvarArquivo.launch(BackupArquivo.nomeSugerido(System.currentTimeMillis())));
        binding.btnAbrirArquivo.setOnClickListener(v ->
                abrirArquivo.launch(new String[]{"application/zip", "application/x-zip-compressed",
                        "application/octet-stream"}));
        binding.switchAutomatico.setOnCheckedChangeListener((b, ligado) -> {
            if (ligado == prefs.automatico()) return;
            prefs.setAutomatico(ligado);
            BackupAgendador.atualizar(this);
            Avisos.info(this, ligado ? "Cópia automática ligada." : "Cópia automática desligada.");
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (trabalhando) {
                    Avisos.info(BackupActivity.this, "Aguarde terminar antes de sair desta tela.");
                } else {
                    finish();
                }
            }
        });

        atualizarTela();
    }

    // ---------------------------------------------------------------- tela

    private void atualizarTela() {
        boolean conectado = prefs.driveConectado();
        binding.btnConectar.setVisibility(conectado ? View.GONE : View.VISIBLE);
        binding.btnBackupAgora.setVisibility(conectado ? View.VISIBLE : View.GONE);
        binding.btnRestaurarDrive.setVisibility(conectado ? View.VISIBLE : View.GONE);
        binding.switchAutomatico.setVisibility(conectado ? View.VISIBLE : View.GONE);
        binding.txtDicaAutomatico.setVisibility(conectado ? View.VISIBLE : View.GONE);
        binding.btnDesconectar.setVisibility(conectado ? View.VISIBLE : View.GONE);
        binding.switchAutomatico.setChecked(prefs.automatico());

        StringBuilder detalhe = new StringBuilder();
        String titulo;
        int cor;
        int icone;
        long ultima = prefs.ultimaCopia();
        if (conectado) {
            detalhe.append("Conta Google: ").append(prefs.contaEmail() == null ? "conectada" : prefs.contaEmail());
        } else {
            detalhe.append("Google Drive não conectado.");
        }
        if (prefs.ultimoBackupDrive() > 0) {
            detalhe.append("\nÚltima cópia no Drive: ").append(quando(prefs.ultimoBackupDrive()));
        }
        if (prefs.ultimoBackupArquivo() > 0) {
            detalhe.append("\nÚltima cópia em arquivo: ").append(quando(prefs.ultimoBackupArquivo()));
        }
        String erro = prefs.ultimoErro();
        boolean erroRecente = erro != null && prefs.ultimoErroEm() > ultima;
        if (erroRecente) detalhe.append("\n\nAtenção: ").append(erro);

        if (ultima == 0) {
            titulo = "Ainda não há nenhuma cópia";
            cor = R.color.alerta;
            icone = R.drawable.ic_aviso;
        } else if (erroRecente || Datas.diasDesde(ultima, System.currentTimeMillis(), java.util.TimeZone.getDefault()) > 3) {
            titulo = "Faça uma cópia nova";
            cor = R.color.alerta;
            icone = R.drawable.ic_aviso;
        } else {
            titulo = "Os dados estão protegidos";
            cor = R.color.sucesso;
            icone = R.drawable.ic_nuvem_ok;
        }
        binding.txtStatusTitulo.setText(titulo);
        binding.txtStatusDetalhe.setText(detalhe);
        binding.iconeStatus.setImageResource(icone);
        binding.iconeStatus.setColorFilter(ContextCompat.getColor(this, cor));
        binding.cartaoStatus.setStrokeColor(ContextCompat.getColor(this, cor));
    }

    private String quando(long instante) {
        return Datas.quando(instante);
    }

    private void comecar(String mensagem) {
        trabalhando = true;
        binding.areaProgresso.setVisibility(View.VISIBLE);
        binding.txtProgresso.setText(mensagem);
        habilitarBotoes(false);
    }

    private void terminar() {
        trabalhando = false;
        binding.areaProgresso.setVisibility(View.GONE);
        habilitarBotoes(true);
        atualizarTela();
    }

    private void habilitarBotoes(boolean habilitar) {
        binding.btnConectar.setEnabled(habilitar);
        binding.btnBackupAgora.setEnabled(habilitar);
        binding.btnRestaurarDrive.setEnabled(habilitar);
        binding.btnDesconectar.setEnabled(habilitar);
        binding.btnSalvarArquivo.setEnabled(habilitar);
        binding.btnAbrirArquivo.setEnabled(habilitar);
        binding.switchAutomatico.setEnabled(habilitar);
        binding.barraTopo.btnVoltar.setEnabled(habilitar);
    }

    // ---------------------------------------------------------------- Google Drive

    /** Garante um token de acesso; se preciso, mostra a tela do Google para autorizar. */
    private void obterToken(String mensagem, ComToken acao) {
        comecar(mensagem);
        DriveAuth.autorizar(this)
                .addOnSuccessListener(this, resultado -> {
                    if (resultado.hasResolution() && resultado.getPendingIntent() != null) {
                        acaoPendente = acao;
                        autorizacao.launch(new IntentSenderRequest.Builder(
                                resultado.getPendingIntent().getIntentSender()).build());
                    } else {
                        acao.executar(resultado.getAccessToken());
                    }
                })
                .addOnFailureListener(this, e -> {
                    terminar();
                    Avisos.erro(this, "Não foi possível conectar ao Google Drive", DriveAuth.explicar(e));
                });
    }

    private void conectar() {
        obterToken("Conectando ao Google Drive…", token -> App.SEGUNDO_PLANO.execute(() -> {
            try {
                DriveClient drive = new DriveClient(token);
                String email = drive.emailDaConta();
                drive.obterOuCriarPasta();
                prefs.conectar(email);
                BackupAgendador.atualizar(this);
                runOnUiThread(() -> {
                    terminar();
                    new MaterialAlertDialogBuilder(this)
                            .setTitle("Google Drive conectado!")
                            .setMessage("As cópias serão guardadas na conta " + (email == null ? "escolhida" : email)
                                    + ", na pasta \"" + DriveClient.NOME_PASTA + "\".\n\nQuer fazer a primeira cópia agora?")
                            .setPositiveButton("Sim, fazer agora", (d, w) -> fazerBackupNoDrive())
                            .setNegativeButton("Depois", null)
                            .show();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    terminar();
                    Avisos.erro(this, "Não foi possível conectar", mensagemDeErro(e));
                });
            }
        }));
    }

    private void fazerBackupNoDrive() {
        obterToken("Fazendo a cópia no Google Drive… não feche o app.", token -> App.SEGUNDO_PLANO.execute(() -> {
            try {
                BackupServico.enviarParaDrive(this, token);
                runOnUiThread(() -> {
                    terminar();
                    Avisos.sucesso(this, "Pronto! A cópia foi guardada no Google Drive.");
                });
            } catch (Exception e) {
                prefs.registrarErro(mensagemDeErro(e));
                runOnUiThread(() -> {
                    terminar();
                    Avisos.erro(this, "A cópia não foi feita", mensagemDeErro(e));
                });
            }
        }));
    }

    private void escolherCopiaNoDrive() {
        obterToken("Procurando as cópias no Google Drive…", token -> App.SEGUNDO_PLANO.execute(() -> {
            try {
                DriveClient drive = new DriveClient(token);
                String pasta = drive.obterOuCriarPasta();
                List<DriveClient.ArquivoDrive> copias = drive.listar(pasta);
                runOnUiThread(() -> {
                    terminar();
                    mostrarListaDeCopias(token, copias);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    terminar();
                    Avisos.erro(this, "Não foi possível ver as cópias", mensagemDeErro(e));
                });
            }
        }));
    }

    private void mostrarListaDeCopias(String token, List<DriveClient.ArquivoDrive> copias) {
        if (copias.isEmpty()) {
            Avisos.erro(this, "Nenhuma cópia encontrada",
                    "Esta conta ainda não tem cópias da livraria no Google Drive.");
            return;
        }
        SimpleDateFormat formato = new SimpleDateFormat("EEEE, dd/MM/yyyy 'às' HH:mm", Periodo.BR);
        List<Dialogos.Opcao> opcoes = new ArrayList<>();
        for (int i = 0; i < Math.min(8, copias.size()); i++) {
            DriveClient.ArquivoDrive c = copias.get(i);
            String data = c.criadoEm > 0 ? formato.format(new Date(c.criadoEm)) : c.nome;
            String texto = Character.toUpperCase(data.charAt(0)) + data.substring(1)
                    + (c.tamanho > 0 ? "  (" + Formatter.formatShortFileSize(this, c.tamanho) + ")" : "");
            opcoes.add(new Dialogos.Opcao(texto, R.drawable.ic_historico, () -> baixarCopia(token, c)));
        }
        Dialogos.opcoes(this, "Qual cópia restaurar?",
                "A mais recente está em primeiro lugar.", opcoes.toArray(new Dialogos.Opcao[0]));
    }

    private void baixarCopia(String token, DriveClient.ArquivoDrive copia) {
        comecar("Baixando a cópia…");
        App.SEGUNDO_PLANO.execute(() -> {
            try {
                File destino = arquivoTemporarioDeRestauracao();
                new DriveClient(token).baixar(copia.id, destino);
                BackupArquivo.Resumo resumo = BackupArquivo.ler(destino);
                runOnUiThread(() -> {
                    terminar();
                    confirmarRestauracao(destino, resumo);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    terminar();
                    Avisos.erro(this, "Não foi possível baixar a cópia", mensagemDeErro(e));
                });
            }
        });
    }

    private void confirmarDesconectar() {
        Dialogos.confirmar(this, "Desconectar o Google Drive?",
                "As cópias automáticas vão parar. As cópias que já estão no Drive continuam guardadas lá.",
                "Sim, desconectar", "Não", () -> {
                    prefs.desconectar();
                    BackupAgendador.atualizar(this);
                    atualizarTela();
                    Avisos.info(this, "Google Drive desconectado.");
                });
    }

    // ---------------------------------------------------------------- arquivo

    private void salvarEmArquivo(Uri destino) {
        comecar("Salvando a cópia…");
        App.SEGUNDO_PLANO.execute(() -> {
            try {
                File zip = BackupArquivo.criar(this);
                try (InputStream in = new java.io.FileInputStream(zip);
                     OutputStream out = getContentResolver().openOutputStream(destino)) {
                    if (out == null) throw new IOException("Não foi possível gravar no local escolhido.");
                    BackupArquivo.copiarFluxo(in, out);
                } finally {
                    //noinspection ResultOfMethodCallIgnored
                    zip.delete();
                }
                prefs.registrarSucessoArquivo(System.currentTimeMillis());
                runOnUiThread(() -> {
                    terminar();
                    Avisos.sucesso(this, "Pronto! A cópia foi salva.");
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    terminar();
                    Avisos.erro(this, "A cópia não foi salva", mensagemDeErro(e));
                });
            }
        });
    }

    private void restaurarDeArquivo(Uri origem) {
        comecar("Lendo a cópia…");
        App.SEGUNDO_PLANO.execute(() -> {
            try {
                File destino = arquivoTemporarioDeRestauracao();
                try (InputStream in = getContentResolver().openInputStream(origem);
                     OutputStream out = new FileOutputStream(destino)) {
                    if (in == null) throw new IOException("Não foi possível abrir o arquivo.");
                    BackupArquivo.copiarFluxo(in, out);
                }
                BackupArquivo.Resumo resumo = BackupArquivo.ler(destino);
                runOnUiThread(() -> {
                    terminar();
                    confirmarRestauracao(destino, resumo);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    terminar();
                    Avisos.erro(this, "Não foi possível usar este arquivo", mensagemDeErro(e));
                });
            }
        });
    }

    // ---------------------------------------------------------------- restauração

    private File arquivoTemporarioDeRestauracao() {
        File pasta = new File(getCacheDir(), "restaurar_origem");
        //noinspection ResultOfMethodCallIgnored
        pasta.mkdirs();
        return new File(pasta, "copia.zip");
    }

    private void confirmarRestauracao(File zip, BackupArquivo.Resumo resumo) {
        String data = resumo.criadoEm > 0
                ? new SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Periodo.BR).format(new Date(resumo.criadoEm))
                : "data desconhecida";
        new MaterialAlertDialogBuilder(this)
                .setIcon(R.drawable.ic_aviso)
                .setTitle("Restaurar esta cópia?")
                .setMessage("Cópia de " + data + ":\n"
                        + "• " + Texto.plural(resumo.livros, "livro cadastrado", "livros cadastrados") + "\n"
                        + "• " + Texto.plural(resumo.vendas, "registro de venda", "registros de venda") + "\n\n"
                        + "ATENÇÃO: os dados que estão neste tablet agora serão substituídos por esta cópia.")
                .setPositiveButton("Sim, restaurar", (d, w) -> restaurar(zip))
                .setNegativeButton(R.string.cancelar, (d, w) -> {
                    //noinspection ResultOfMethodCallIgnored
                    zip.delete();
                })
                .show();
    }

    private void restaurar(File zip) {
        comecar("Restaurando… não feche o app.");
        App.SEGUNDO_PLANO.execute(() -> {
            try {
                BackupArquivo.restaurar(this, zip);
                //noinspection ResultOfMethodCallIgnored
                zip.delete();
                Carrinho.get().limpar();
                runOnUiThread(() -> {
                    trabalhando = false;
                    // Reabre o app do começo, já com os dados restaurados.
                    Intent inicio = new Intent(this, MainActivity.class);
                    inicio.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    inicio.putExtra(EXTRA_MENSAGEM, "Cópia restaurada com sucesso!");
                    startActivity(inicio);
                    finish();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    terminar();
                    Avisos.erro(this, "A restauração não foi concluída", mensagemDeErro(e));
                });
            }
        });
    }

    private String mensagemDeErro(Exception e) {
        if (e instanceof BackupInvalidoException || e instanceof DriveClient.ErroDrive) return e.getMessage();
        if (e instanceof java.net.UnknownHostException || e instanceof java.net.SocketTimeoutException
                || e instanceof java.net.ConnectException) {
            return "Sem internet ou conexão muito lenta. Verifique o Wi-Fi e tente de novo.";
        }
        return DriveAuth.explicar(e);
    }
}
