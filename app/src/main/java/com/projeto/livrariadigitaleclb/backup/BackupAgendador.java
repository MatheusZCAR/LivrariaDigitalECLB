package com.projeto.livrariadigitaleclb.backup;

import android.content.Context;

import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

/** Agenda as cópias automáticas. */
public final class BackupAgendador {

    private static final String DIARIO = "backup-diario";
    private static final String APOS_MUDANCAS = "backup-apos-mudancas";

    private BackupAgendador() {}

    private static Constraints restricoes() {
        return new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build();
    }

    /** Liga ou desliga a cópia diária conforme a configuração. */
    public static void atualizar(Context c) {
        BackupPrefs prefs = new BackupPrefs(c);
        WorkManager wm = WorkManager.getInstance(c);
        if (prefs.driveConectado() && prefs.automatico()) {
            PeriodicWorkRequest diario = new PeriodicWorkRequest.Builder(BackupWorker.class, 24, TimeUnit.HOURS)
                    .setConstraints(restricoes())
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
                    .build();
            wm.enqueueUniquePeriodicWork(DIARIO, ExistingPeriodicWorkPolicy.KEEP, diario);
        } else {
            wm.cancelUniqueWork(DIARIO);
            wm.cancelUniqueWork(APOS_MUDANCAS);
        }
    }

    /**
     * Chamado depois de uma venda ou cadastro: agenda uma cópia para daqui a 30 minutos.
     * Várias mudanças seguidas geram uma única cópia.
     */
    public static void dadosMudaram(Context c) {
        BackupPrefs prefs = new BackupPrefs(c);
        if (!prefs.driveConectado() || !prefs.automatico()) return;
        OneTimeWorkRequest pedido = new OneTimeWorkRequest.Builder(BackupWorker.class)
                .setConstraints(restricoes())
                .setInitialDelay(30, TimeUnit.MINUTES)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
                .build();
        WorkManager.getInstance(c).enqueueUniqueWork(APOS_MUDANCAS, ExistingWorkPolicy.KEEP, pedido);
    }
}
