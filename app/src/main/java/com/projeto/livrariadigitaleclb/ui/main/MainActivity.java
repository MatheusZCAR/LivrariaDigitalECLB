package com.projeto.livrariadigitaleclb.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.projeto.livrariadigitaleclb.BuildConfig;
import com.projeto.livrariadigitaleclb.R;
import com.projeto.livrariadigitaleclb.backup.BackupPrefs;
import com.projeto.livrariadigitaleclb.databinding.ActivityMainBinding;
import com.projeto.livrariadigitaleclb.databinding.ItemAtalhoBinding;
import com.projeto.livrariadigitaleclb.ui.BaseActivity;
import com.projeto.livrariadigitaleclb.ui.backup.BackupActivity;
import com.projeto.livrariadigitaleclb.ui.catalogo.CatalogoActivity;
import com.projeto.livrariadigitaleclb.ui.pedidos.PedidosActivity;
import com.projeto.livrariadigitaleclb.ui.realizarvenda.RealizarVendaActivity;
import com.projeto.livrariadigitaleclb.ui.relatorios.RelatoriosActivity;
import com.projeto.livrariadigitaleclb.util.Avisos;
import com.projeto.livrariadigitaleclb.util.Datas;
import com.projeto.livrariadigitaleclb.util.Periodo;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class MainActivity extends BaseActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.barraTopo.btnVoltar.setVisibility(View.GONE);
        definirTitulo(getString(R.string.inicio_titulo));

        configurarAtalho(binding.atalhoVender, R.drawable.ic_venda, R.string.vender,
                R.string.vender_descricao, RealizarVendaActivity.class);
        configurarAtalho(binding.atalhoCatalogo, R.drawable.ic_catalogo, R.string.catalogo,
                R.string.catalogo_descricao, CatalogoActivity.class);
        configurarAtalho(binding.atalhoPedidos, R.drawable.ic_pedidos, R.string.pedidos,
                R.string.pedidos_descricao, PedidosActivity.class);
        configurarAtalho(binding.atalhoRelatorios, R.drawable.ic_relatorios, R.string.relatorios,
                R.string.relatorios_descricao, RelatoriosActivity.class);

        View.OnClickListener abrirBackup = v -> startActivity(new Intent(this, BackupActivity.class));
        binding.cartaoBackup.setOnClickListener(abrirBackup);
        binding.btnAbrirBackup.setOnClickListener(abrirBackup);

        binding.txtVersao.setText("Versão " + BuildConfig.VERSION_NAME);

        String mensagem = getIntent().getStringExtra(BackupActivity.EXTRA_MENSAGEM);
        if (mensagem != null && savedInstanceState == null) {
            Avisos.sucesso(this, mensagem);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        atualizarSaudacao();
        atualizarStatusBackup();
    }

    private void configurarAtalho(ItemAtalhoBinding atalho, int icone, int rotulo, int descricao,
                                  Class<?> destino) {
        atalho.iconeAtalho.setImageResource(icone);
        atalho.rotuloAtalho.setText(rotulo);
        atalho.descricaoAtalho.setText(descricao);
        atalho.getRoot().setContentDescription(getString(rotulo) + ". " + getString(descricao));
        atalho.getRoot().setOnClickListener(v -> startActivity(new Intent(this, destino)));
    }

    private void atualizarSaudacao() {
        int hora = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String saudacao = hora < 12 ? "Bom dia!" : (hora < 18 ? "Boa tarde!" : "Boa noite!");
        binding.txtSaudacao.setText(saudacao);
        String data = new SimpleDateFormat("EEEE, d 'de' MMMM 'de' yyyy", Periodo.BR).format(new Date());
        binding.txtData.setText("Hoje é " + data);
    }

    private void atualizarStatusBackup() {
        BackupPrefs prefs = new BackupPrefs(this);
        long ultima = prefs.ultimaCopia();
        long agora = System.currentTimeMillis();
        String erro = prefs.ultimoErro();

        String texto;
        int cor;
        int icone;
        if (erro != null && prefs.ultimoErroEm() > ultima) {
            texto = erro;
            cor = R.color.alerta;
            icone = R.drawable.ic_aviso;
        } else if (ultima == 0) {
            texto = prefs.driveConectado()
                    ? "Ainda não foi feita nenhuma cópia. Toque para fazer agora."
                    : "Ainda não configurada. Toque aqui para proteger os dados da livraria.";
            cor = R.color.alerta;
            icone = R.drawable.ic_aviso;
        } else {
            String quando = Datas.quando(ultima);
            String onde = prefs.ultimoBackupDrive() >= prefs.ultimoBackupArquivo()
                    ? "no Google Drive" : "em arquivo";
            if (Datas.diasDesde(ultima, agora, java.util.TimeZone.getDefault()) > 3) {
                texto = "A última cópia " + onde + " é antiga (" + quando + "). Toque para fazer uma nova.";
                cor = R.color.alerta;
                icone = R.drawable.ic_aviso;
            } else {
                texto = "Última cópia " + onde + ": " + quando;
                cor = R.color.sucesso;
                icone = R.drawable.ic_nuvem_ok;
            }
        }
        binding.txtStatusBackup.setText(texto);
        binding.iconeBackup.setImageResource(icone);
        binding.iconeBackup.setColorFilter(ContextCompat.getColor(this, cor));
        binding.cartaoBackup.setStrokeColor(ContextCompat.getColor(this, cor == R.color.sucesso ? R.color.borda : cor));
    }
}
