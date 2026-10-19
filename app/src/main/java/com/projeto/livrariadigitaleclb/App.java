package com.projeto.livrariadigitaleclb;

import android.app.Application;
import android.util.Log;

import com.projeto.livrariadigitaleclb.backup.BackupAgendador;
import com.projeto.livrariadigitaleclb.backup.BackupPrefs;
import com.projeto.livrariadigitaleclb.util.Capas;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class App extends Application {

    /** Uma única fila para tarefas demoradas (backup, PDFs, fotos). */
    public static final ExecutorService SEGUNDO_PLANO = Executors.newSingleThreadExecutor();

    @Override
    public void onCreate() {
        super.onCreate();
        BackupAgendador.atualizar(this);

        BackupPrefs prefs = new BackupPrefs(this);
        if (!prefs.capasMigradas()) {
            SEGUNDO_PLANO.execute(() -> {
                try {
                    Capas.migrarCapasAntigas(this);
                    prefs.marcarCapasMigradas();
                } catch (Exception e) {
                    Log.e("App", "Falha ao migrar capas antigas", e);
                }
            });
        }
    }
}
