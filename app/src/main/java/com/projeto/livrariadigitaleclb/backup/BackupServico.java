package com.projeto.livrariadigitaleclb.backup;

import android.content.Context;

import java.io.File;
import java.io.IOException;

/** Junta as peças: gera a cópia e envia ao Google Drive. */
public final class BackupServico {

    /** Quantas cópias manter no Drive (as mais antigas são apagadas). */
    public static final int COPIAS_NO_DRIVE = 20;

    private BackupServico() {}

    /** Gera e envia a cópia. Não chame na thread principal. */
    public static void enviarParaDrive(Context c, String token) throws IOException {
        File zip = BackupArquivo.criar(c);
        try {
            DriveClient drive = new DriveClient(token);
            String pasta = drive.obterOuCriarPasta();
            drive.enviar(zip, zip.getName(), pasta);
            new BackupPrefs(c).registrarSucessoDrive(System.currentTimeMillis());
            try {
                drive.apagarAntigas(pasta, COPIAS_NO_DRIVE);
            } catch (IOException ignorada) {
                // não é grave: tenta de novo na próxima cópia
            }
        } finally {
            //noinspection ResultOfMethodCallIgnored
            zip.delete();
        }
    }
}
