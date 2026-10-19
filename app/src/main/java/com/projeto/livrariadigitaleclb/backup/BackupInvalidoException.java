package com.projeto.livrariadigitaleclb.backup;

/** O arquivo escolhido não é uma cópia de segurança válida deste app. */
public class BackupInvalidoException extends Exception {
    public BackupInvalidoException(String mensagem) {
        super(mensagem);
    }
}
