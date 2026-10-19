package com.projeto.livrariadigitaleclb.backup;

import android.content.Context;

import com.google.android.gms.auth.api.identity.AuthorizationRequest;
import com.google.android.gms.auth.api.identity.AuthorizationResult;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Autorização de acesso ao Google Drive (Google Identity Services).
 * Pede só o escopo "drive.file": o app vê apenas os arquivos que ele mesmo criou.
 */
public final class DriveAuth {

    public static final String ESCOPO_DRIVE_FILE = "https://www.googleapis.com/auth/drive.file";

    private DriveAuth() {}

    /** A pessoa precisa abrir o app e tocar em "Conectar" de novo. */
    public static class PrecisaReconectarException extends Exception {
        public PrecisaReconectarException() {
            super("É preciso conectar ao Google Drive de novo.");
        }
    }

    public static AuthorizationRequest pedido() {
        return AuthorizationRequest.builder()
                .setRequestedScopes(Collections.singletonList(new Scope(ESCOPO_DRIVE_FILE)))
                .build();
    }

    /** Usado pela tela: pode devolver um PendingIntent para a pessoa escolher a conta. */
    public static Task<AuthorizationResult> autorizar(Context c) {
        return Identity.getAuthorizationClient(c).authorize(pedido());
    }

    /**
     * Usado em segundo plano (backup automático): só funciona se a pessoa já autorizou antes.
     * Não chame na thread principal.
     */
    public static String tokenSemInterface(Context c) throws Exception {
        try {
            AuthorizationResult r = Tasks.await(autorizar(c.getApplicationContext()), 60, TimeUnit.SECONDS);
            if (r.hasResolution() || r.getAccessToken() == null) {
                throw new PrecisaReconectarException();
            }
            return r.getAccessToken();
        } catch (ExecutionException e) {
            Throwable causa = e.getCause();
            if (causa instanceof Exception) throw (Exception) causa;
            throw e;
        }
    }

    /** Mensagem amigável para falhas da autorização. */
    public static String explicar(Exception e) {
        if (e instanceof ApiException) {
            int codigo = ((ApiException) e).getStatusCode();
            switch (codigo) {
                case CommonStatusCodes.NETWORK_ERROR:
                    return "Sem internet. Verifique o Wi-Fi e tente de novo.";
                case CommonStatusCodes.CANCELED:
                    return "A conexão foi cancelada.";
                case CommonStatusCodes.DEVELOPER_ERROR:
                    return "O app ainda não foi registrado no Google Cloud para usar o Drive "
                            + "(erro de configuração, código 10). Peça ajuda ao responsável pelo app.";
                default:
                    return "Não foi possível conectar ao Google (código " + codigo + ").";
            }
        }
        if (e instanceof PrecisaReconectarException) return e.getMessage();
        String msg = e.getMessage();
        return msg == null ? "Não foi possível conectar ao Google." : msg;
    }
}
