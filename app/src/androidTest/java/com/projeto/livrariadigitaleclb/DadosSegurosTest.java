package com.projeto.livrariadigitaleclb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.room.Room;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.projeto.livrariadigitaleclb.backup.BackupArquivo;
import com.projeto.livrariadigitaleclb.data.RelatorioVendas;
import com.projeto.livrariadigitaleclb.data.local.AppDatabase;
import com.projeto.livrariadigitaleclb.data.local.entity.LivroComEstoque;
import com.projeto.livrariadigitaleclb.data.local.entity.LivroEntity;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.List;

/** Garante que os dados da livraria não se perdem: migração do banco e backup/restauração. */
@RunWith(AndroidJUnit4.class)
public class DadosSegurosTest {

    private static final String BANCO_TESTE = "teste_migracao_15";

    /**
     * Cria um banco exatamente como a versão 15 do app (a que está instalada na igreja)
     * e confere que, ao abrir com a versão nova, nada se perde.
     */
    @Test
    public void migracao15Para16MantemOsDados() {
        Context c = Dados.contexto();
        c.deleteDatabase(BANCO_TESTE);
        File arquivo = c.getDatabasePath(BANCO_TESTE);
        //noinspection ResultOfMethodCallIgnored
        arquivo.getParentFile().mkdirs();

        SQLiteDatabase antigo = SQLiteDatabase.openOrCreateDatabase(arquivo, null);
        String[] tabelas = {
                "CREATE TABLE IF NOT EXISTS `pedidos` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `titulo` TEXT, `autor` TEXT, `quantidadeDesejada` INTEGER NOT NULL)",
                "CREATE TABLE IF NOT EXISTS `livros` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `titulo` TEXT, `autor` TEXT, `codigoBarras` TEXT, `preco` REAL NOT NULL, `esgotado` INTEGER NOT NULL, `imagemPath` TEXT, `localizacao` TEXT)",
                "CREATE TABLE IF NOT EXISTS `vendas` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `livroId` INTEGER NOT NULL, `titulo` TEXT, `preco` REAL NOT NULL, `dataVenda` INTEGER NOT NULL)",
                "CREATE TABLE IF NOT EXISTS `autores` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `nome` TEXT)",
                "CREATE TABLE IF NOT EXISTS `localizacoes` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `descricao` TEXT)",
                "CREATE TABLE IF NOT EXISTS `estoque` (`livroId` INTEGER NOT NULL, `quantidadeDisponivel` INTEGER NOT NULL, `dataAtualizacao` INTEGER NOT NULL, PRIMARY KEY(`livroId`), FOREIGN KEY(`livroId`) REFERENCES `livros`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
                "CREATE TABLE IF NOT EXISTS `itens_venda` (`vendaId` INTEGER NOT NULL, `livroId` INTEGER NOT NULL, `quantidadeVendida` INTEGER NOT NULL, `precoUnitario` REAL NOT NULL, PRIMARY KEY(`vendaId`, `livroId`), FOREIGN KEY(`vendaId`) REFERENCES `vendas`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`livroId`) REFERENCES `livros`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
                "CREATE TABLE IF NOT EXISTS `relatorios` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `tipoRelatorio` TEXT, `localizacaoRelatorio` TEXT)",
                "CREATE TABLE IF NOT EXISTS `relatorio_venda` (`relatorioId` INTEGER NOT NULL, `vendaId` INTEGER NOT NULL, PRIMARY KEY(`relatorioId`, `vendaId`), FOREIGN KEY(`relatorioId`) REFERENCES `relatorios`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`vendaId`) REFERENCES `vendas`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
                "CREATE TABLE IF NOT EXISTS `livro_autor` (`livroId` INTEGER NOT NULL, `autorId` INTEGER NOT NULL, PRIMARY KEY(`livroId`, `autorId`), FOREIGN KEY(`livroId`) REFERENCES `livros`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`autorId`) REFERENCES `autores`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        };
        for (String sql : tabelas) antigo.execSQL(sql);
        antigo.execSQL("INSERT INTO livros (id, titulo, autor, codigoBarras, preco, esgotado, imagemPath, localizacao) VALUES (1, 'Hinário', 'IECLB', '123', 40.0, 0, NULL, 'A1')");
        antigo.execSQL("INSERT INTO estoque VALUES (1, 7, 0)");
        antigo.execSQL("INSERT INTO pedidos (titulo, autor, quantidadeDesejada) VALUES ('Bíblia', 'SBB', 3)");
        // Versão antiga: uma linha por exemplar, mesmo horário para a mesma venda.
        antigo.execSQL("INSERT INTO vendas (livroId, titulo, preco, dataVenda) VALUES (1, 'Hinário', 40.0, 1700000000000)");
        antigo.execSQL("INSERT INTO vendas (livroId, titulo, preco, dataVenda) VALUES (1, 'Hinário', 40.0, 1700000000000)");
        antigo.execSQL("INSERT INTO vendas (livroId, titulo, preco, dataVenda) VALUES (1, 'Hinário', 40.0, 1700000999000)");
        antigo.setVersion(15);
        antigo.close();

        AppDatabase db = Room.databaseBuilder(c, AppDatabase.class, BANCO_TESTE)
                .addMigrations(AppDatabase.MIGRACAO_15_16)
                .allowMainThreadQueries()
                .build();
        try {
            List<LivroComEstoque> livros = db.livroDao().listarLivrosComEstoque();
            assertEquals(1, livros.size());
            assertEquals("Hinário", livros.get(0).livro.titulo);
            assertEquals(7, livros.get(0).quantidade);
            assertEquals(1, db.pedidoDao().getPedidos().size());

            RelatorioVendas r = RelatorioVendas.calcular(db.vendaDao().listarVendas());
            assertEquals(2, r.vendas.size());            // duas vendas
            assertEquals(3, r.unidadesVendidas);         // três exemplares
            assertEquals(120.0, r.totalGeral, 0.001);
        } finally {
            db.close();
            c.deleteDatabase(BANCO_TESTE);
        }
    }

    /** Faz a cópia, apaga dados, restaura e confere que tudo voltou (inclusive as capas). */
    @Test
    public void backupERestauracaoDevolvemTudo() throws Exception {
        Context c = Dados.contexto();
        Dados.recriarBanco();
        AppDatabase db = AppDatabase.getInstance(c);
        int livrosAntes = db.livroDao().contar();
        int pedidosAntes = db.pedidoDao().getPedidos().size();

        File zip = BackupArquivo.criar(c);
        assertTrue(zip.length() > 0);
        BackupArquivo.Resumo resumo = BackupArquivo.ler(zip);
        assertEquals(livrosAntes, resumo.livros);

        // Estraga os dados
        for (LivroEntity l : db.livroDao().listarTodos()) {
            db.estoqueDao().excluirDoLivro(l.id);
            db.livroDao().excluirLivro(l);
        }
        db.pedidoDao().deletarTodos();
        assertEquals(0, db.livroDao().contar());

        File copia = new File(c.getCacheDir(), "copia_teste.zip");
        BackupArquivo.copiar(zip, copia);
        BackupArquivo.restaurar(c, copia);

        AppDatabase restaurado = AppDatabase.getInstance(c);
        assertEquals(livrosAntes, restaurado.livroDao().contar());
        assertEquals(pedidosAntes, restaurado.pedidoDao().getPedidos().size());
        int comCapa = 0;
        for (LivroEntity l : restaurado.livroDao().listarTodos()) {
            if (l.imagemPath != null) {
                assertTrue("capa deveria existir: " + l.imagemPath, new File(l.imagemPath).exists());
                comCapa++;
            }
        }
        assertEquals(3, comCapa);
        assertNotNull(new File(c.getFilesDir(), "seguranca/antes_de_restaurar.zip").exists() ? "ok" : null);
    }

    @Test
    public void arquivoQualquerNaoERestaurado() throws Exception {
        Context c = Dados.contexto();
        File falso = new File(c.getCacheDir(), "falso.zip");
        java.io.FileOutputStream out = new java.io.FileOutputStream(falso);
        out.write("isto não é um zip".getBytes());
        out.close();
        try {
            BackupArquivo.ler(falso);
            throw new AssertionError("deveria recusar");
        } catch (com.projeto.livrariadigitaleclb.backup.BackupInvalidoException esperado) {
            // ok
        }
    }

    @SuppressWarnings("unused")
    private static int contar(SQLiteDatabase db, String tabela) {
        try (Cursor cur = db.rawQuery("SELECT COUNT(*) FROM " + tabela, null)) {
            cur.moveToFirst();
            return cur.getInt(0);
        }
    }
}
