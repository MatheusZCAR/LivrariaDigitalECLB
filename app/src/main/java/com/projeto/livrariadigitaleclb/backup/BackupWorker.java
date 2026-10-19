package com.projeto.livrariadigitaleclb.backup;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.IOException;

/** Cópia automática para o Google Drive, executada pelo WorkManager. */
public class BackupWorker extends Worker {

    private static final String TAG = "BackupWorker";

    public BackupWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context c = getApplicationContext();
        BackupPrefs prefs = new BackupPrefs(c);
        if (!prefs.driveConectado()) return Result.success();

        try {
            String token = DriveAuth.tokenSemInterface(c);
            BackupServico.enviarParaDrive(c, token);
            return Result.success();
        } catch (DriveAuth.PrecisaReconectarException e) {
            prefs.registrarErro("A cópia automática parou: é preciso conectar ao Google Drive de novo.");
            return Result.failure();
        } catch (DriveClient.ErroDrive e) {
            prefs.registrarErro(e.getMessage());
            return e.precisaReconectar ? Result.failure() : Result.retry();
        } catch (IOException e) {
            Log.w(TAG, "Falha de rede no backup automático", e);
            prefs.registrarErro("Sem internet na última tentativa de cópia automática.");
            return getRunAttemptCount() < 5 ? Result.retry() : Result.failure();
        } catch (Exception e) {
            Log.e(TAG, "Erro no backup automático", e);
            prefs.registrarErro(DriveAuth.explicar(e));
            return getRunAttemptCount() < 3 ? Result.retry() : Result.failure();
        }
    }
}
