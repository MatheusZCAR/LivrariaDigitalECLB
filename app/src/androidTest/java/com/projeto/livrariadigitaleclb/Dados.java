package com.projeto.livrariadigitaleclb;

import android.app.Instrumentation;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.os.SystemClock;

import androidx.test.platform.app.InstrumentationRegistry;

import com.projeto.livrariadigitaleclb.backup.BackupPrefs;
import com.projeto.livrariadigitaleclb.data.Carrinho;
import com.projeto.livrariadigitaleclb.data.local.AppDatabase;
import com.projeto.livrariadigitaleclb.data.local.entity.EstoqueEntity;
import com.projeto.livrariadigitaleclb.data.local.entity.LivroEntity;
import com.projeto.livrariadigitaleclb.data.local.entity.PedidoEntity;
import com.projeto.livrariadigitaleclb.util.Capas;

import java.io.File;
import java.io.FileOutputStream;

/** Dados de exemplo e utilidades dos testes. */
final class Dados {

    private Dados() {}

    static Context contexto() {
        return InstrumentationRegistry.getInstrumentation().getTargetContext();
    }

    /** Apaga tudo e cria um catálogo de exemplo. */
    static void recriarBanco() {
        Context c = contexto();
        AppDatabase.fechar();
        c.deleteDatabase(AppDatabase.NOME_ARQUIVO);
        File[] capas = Capas.pasta(c).listFiles();
        if (capas != null) for (File f : capas) //noinspection ResultOfMethodCallIgnored
            f.delete();
        c.getSharedPreferences("backup", Context.MODE_PRIVATE).edit().clear().commit();
        Carrinho.get().limpar();

        AppDatabase db = AppDatabase.getInstance(c);
        livro(db, "Bíblia Sagrada — Nova Almeida Atualizada", "Sociedade Bíblica do Brasil", 89.9, 5, "7891234567895",
                capa(c, "Bíblia", Color.rgb(120, 30, 30)));
        livro(db, "Hinos do Povo de Deus", "IECLB", 45, 3, null, capa(c, "Hinos", Color.rgb(20, 80, 120)));
        livro(db, "Catecismo Menor", "Martinho Lutero", 12.5, 0, null, null);
        livro(db, "Castelo Forte — Meditações Diárias", "Editora Sinodal", 35, 8, null, null);
        livro(db, "Livro de Culto", "", 60, 2, null, capa(c, "Culto", Color.rgb(40, 110, 60)));
        livro(db, "Orações para cada dia", "Vários autores", 25, 10, null, null);

        db.pedidoDao().inserirPedido(new PedidoEntity("Catecismo Menor", "Martinho Lutero", 10));
        db.pedidoDao().inserirPedido(new PedidoEntity("Bíblia de Estudo Lutero", "SBB", 2));
    }

    static int livro(AppDatabase db, String titulo, String autor, double preco, int estoque,
                     String codigo, String capa) {
        LivroEntity l = new LivroEntity();
        l.titulo = titulo;
        l.autor = autor;
        l.preco = preco;
        l.codigoBarras = codigo;
        l.imagemPath = capa;
        l.localizacao = "Estante A";
        int id = (int) db.livroDao().inserirLivro(l);
        EstoqueEntity e = new EstoqueEntity();
        e.livroId = id;
        e.quantidadeDisponivel = estoque;
        e.dataAtualizacao = System.currentTimeMillis();
        db.estoqueDao().inserirEstoque(e);
        return id;
    }

    /** Gera uma "capa" colorida para testar o carregamento de imagens. */
    static String capa(Context c, String texto, int cor) {
        Bitmap b = Bitmap.createBitmap(600, 800, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(b);
        Paint fundo = new Paint();
        fundo.setShader(new LinearGradient(0, 0, 0, 800, cor, Color.BLACK, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, 600, 800, fundo);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(Color.WHITE);
        p.setTextSize(90);
        p.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(texto, 300, 420, p);
        File f = new File(Capas.pasta(c), "capa_teste_" + texto + ".jpg");
        try (FileOutputStream out = new FileOutputStream(f)) {
            b.compress(Bitmap.CompressFormat.JPEG, 85, out);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return f.getAbsolutePath();
    }

    static void marcarBackupFeito() {
        new BackupPrefs(contexto()).conectar("livraria.eclb@gmail.com");
        new BackupPrefs(contexto()).registrarSucessoDrive(System.currentTimeMillis() - 2 * 60 * 60 * 1000L);
    }

    /** Salva uma imagem da tela em files/screenshots (o CI copia essas imagens). */
    static void foto(String nome) {
        Instrumentation inst = InstrumentationRegistry.getInstrumentation();
        inst.waitForIdleSync();
        SystemClock.sleep(1500);
        inst.waitForIdleSync();
        Bitmap b = inst.getUiAutomation().takeScreenshot();
        if (b == null) return;
        File dir = contexto().getExternalFilesDir("screenshots");
        if (dir == null) return;
        //noinspection ResultOfMethodCallIgnored
        dir.mkdirs();
        try (FileOutputStream out = new FileOutputStream(new File(dir, nome + ".jpg"))) {
            b.compress(Bitmap.CompressFormat.JPEG, 80, out);
        } catch (Exception ignorada) {
            // screenshot é só para conferência visual
        }
    }
}
