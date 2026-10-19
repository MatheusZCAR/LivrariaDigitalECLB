package com.projeto.livrariadigitaleclb.backup;

import android.content.Context;
import android.content.SharedPreferences;

/** Guarda a situação das cópias de segurança. */
public final class BackupPrefs {

    private static final String ARQUIVO = "backup";

    private final SharedPreferences p;

    public BackupPrefs(Context c) {
        p = c.getApplicationContext().getSharedPreferences(ARQUIVO, Context.MODE_PRIVATE);
    }

    public boolean driveConectado() { return p.getBoolean("drive_conectado", false); }
    public String contaEmail() { return p.getString("conta_email", null); }
    public boolean automatico() { return p.getBoolean("automatico", true); }
    public long ultimoBackupDrive() { return p.getLong("ultimo_drive", 0); }
    public long ultimoBackupArquivo() { return p.getLong("ultimo_arquivo", 0); }
    public String ultimoErro() { return p.getString("ultimo_erro", null); }
    public long ultimoErroEm() { return p.getLong("ultimo_erro_em", 0); }
    public boolean capasMigradas() { return p.getBoolean("capas_migradas", false); }

    public void conectar(String email) {
        p.edit().putBoolean("drive_conectado", true).putString("conta_email", email).apply();
    }

    public void desconectar() {
        p.edit().putBoolean("drive_conectado", false).remove("conta_email").apply();
    }

    public void setAutomatico(boolean ligado) {
        p.edit().putBoolean("automatico", ligado).apply();
    }

    public void registrarSucessoDrive(long quando) {
        p.edit().putLong("ultimo_drive", quando).remove("ultimo_erro").remove("ultimo_erro_em").apply();
    }

    public void registrarSucessoArquivo(long quando) {
        p.edit().putLong("ultimo_arquivo", quando).apply();
    }

    public void registrarErro(String mensagem) {
        p.edit().putString("ultimo_erro", mensagem).putLong("ultimo_erro_em", System.currentTimeMillis()).apply();
    }

    public void marcarCapasMigradas() {
        p.edit().putBoolean("capas_migradas", true).apply();
    }

    /** Data da cópia mais recente (Drive ou arquivo). */
    public long ultimaCopia() {
        return Math.max(ultimoBackupDrive(), ultimoBackupArquivo());
    }
}
