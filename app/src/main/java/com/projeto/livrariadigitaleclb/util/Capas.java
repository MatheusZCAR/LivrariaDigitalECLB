package com.projeto.livrariadigitaleclb.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.exifinterface.media.ExifInterface;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.signature.ObjectKey;
import com.projeto.livrariadigitaleclb.data.local.AppDatabase;
import com.projeto.livrariadigitaleclb.data.local.entity.LivroEntity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Fotos das capas.
 *
 * As fotos da câmera têm vários megabytes; carregar dezenas delas em tamanho cheio numa grade
 * travava a tela e podia fechar o app por falta de memória. Agora toda capa é reduzida
 * (máx. 1000 px, JPEG 85%) e guardada na pasta interna "capas", que também entra no backup.
 */
public final class Capas {

    private static final String TAG = "Capas";
    private static final int LADO_MAXIMO = 1000;

    private Capas() {}

    public interface FonteImagem {
        InputStream abrir() throws IOException;
    }

    public static File pasta(Context c) {
        File dir = new File(c.getFilesDir(), "capas");
        if (!dir.exists() && !dir.mkdirs()) {
            Log.w(TAG, "Não foi possível criar a pasta de capas");
        }
        return dir;
    }

    /** Arquivo temporário para a câmera gravar a foto original. */
    public static File arquivoTemporario(Context c) {
        File dir = new File(c.getCacheDir(), "fotos");
        //noinspection ResultOfMethodCallIgnored
        dir.mkdirs();
        return new File(dir, "foto_" + System.currentTimeMillis() + ".jpg");
    }

    public static boolean pertenceAPasta(Context c, String caminho) {
        if (caminho == null) return false;
        return new File(caminho).getAbsoluteFile().getParentFile() != null
                && new File(caminho).getAbsoluteFile().getParentFile().equals(pasta(c).getAbsoluteFile());
    }

    @Nullable
    public static File salvarOtimizada(Context c, Uri uri) {
        return salvarOtimizada(c, () -> c.getContentResolver().openInputStream(uri));
    }

    @Nullable
    public static File salvarOtimizada(Context c, File origem) {
        return salvarOtimizada(c, () -> new FileInputStream(origem));
    }

    /** Reduz, corrige a rotação e grava na pasta de capas. Devolve null se falhar. */
    @Nullable
    public static File salvarOtimizada(Context c, FonteImagem fonte) {
        try {
            // 1) Descobre o tamanho sem carregar a imagem
            BitmapFactory.Options limites = new BitmapFactory.Options();
            limites.inJustDecodeBounds = true;
            try (InputStream in = fonte.abrir()) {
                if (in == null) return null;
                BitmapFactory.decodeStream(in, null, limites);
            }
            if (limites.outWidth <= 0 || limites.outHeight <= 0) return null;

            // 2) Carrega já reduzida
            BitmapFactory.Options opcoes = new BitmapFactory.Options();
            opcoes.inSampleSize = calcularReducao(limites.outWidth, limites.outHeight, LADO_MAXIMO);
            Bitmap bitmap;
            try (InputStream in = fonte.abrir()) {
                bitmap = BitmapFactory.decodeStream(in, null, opcoes);
            }
            if (bitmap == null) return null;

            // 3) Corrige a rotação (fotos tiradas com o tablet "de lado")
            int orientacao = ExifInterface.ORIENTATION_NORMAL;
            try (InputStream in = fonte.abrir()) {
                if (in != null) {
                    orientacao = new ExifInterface(in).getAttributeInt(
                            ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                }
            } catch (Exception ignorada) {
                // sem EXIF: mantém como está
            }
            Bitmap final_ = ajustar(bitmap, orientacao, LADO_MAXIMO);

            // 4) Grava em JPEG
            File destino = new File(pasta(c), "capa_" + System.currentTimeMillis() + ".jpg");
            try (FileOutputStream out = new FileOutputStream(destino)) {
                final_.compress(Bitmap.CompressFormat.JPEG, 85, out);
            }
            if (final_ != bitmap) bitmap.recycle();
            final_.recycle();
            return destino;
        } catch (Exception | OutOfMemoryError e) {
            Log.e(TAG, "Erro ao otimizar imagem", e);
            return null;
        }
    }

    static int calcularReducao(int largura, int altura, int ladoMaximo) {
        int reducao = 1;
        while (largura / (reducao * 2) >= ladoMaximo && altura / (reducao * 2) >= ladoMaximo / 2) {
            reducao *= 2;
        }
        return reducao;
    }

    private static Bitmap ajustar(Bitmap origem, int orientacao, int ladoMaximo) {
        Matrix m = new Matrix();
        switch (orientacao) {
            case ExifInterface.ORIENTATION_ROTATE_90: m.postRotate(90); break;
            case ExifInterface.ORIENTATION_ROTATE_180: m.postRotate(180); break;
            case ExifInterface.ORIENTATION_ROTATE_270: m.postRotate(270); break;
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL: m.postScale(-1, 1); break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL: m.postScale(1, -1); break;
            default: break;
        }
        int maior = Math.max(origem.getWidth(), origem.getHeight());
        if (maior > ladoMaximo) {
            float escala = (float) ladoMaximo / maior;
            m.postScale(escala, escala);
        }
        if (m.isIdentity()) return origem;
        return Bitmap.createBitmap(origem, 0, 0, origem.getWidth(), origem.getHeight(), m, true);
    }

    /**
     * Mostra a capa (se existir). Quando não há foto, a imagem fica escondida e aparece o
     * desenho padrão que está por baixo dela no layout.
     */
    public static void carregar(ImageView destino, @Nullable String caminho) {
        File arquivo = caminho == null || caminho.isEmpty() ? null : new File(caminho);
        if (arquivo == null || !arquivo.exists()) {
            Glide.with(destino).clear(destino);
            destino.setImageDrawable(null);
            destino.setVisibility(View.INVISIBLE);
            return;
        }
        destino.setVisibility(View.VISIBLE);
        Glide.with(destino)
                .load(arquivo)
                .signature(new ObjectKey(arquivo.lastModified()))
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                .centerCrop()
                .into(destino);
    }

    public static void apagar(@Nullable String caminho) {
        if (caminho == null) return;
        File f = new File(caminho);
        if (f.exists() && !f.delete()) Log.w(TAG, "Não foi possível apagar " + caminho);
    }

    /**
     * Versões antigas do app guardavam a foto original (grande) na pasta externa.
     * Esta rotina, executada uma vez em segundo plano, reduz essas fotos e as move
     * para a pasta interna de capas.
     */
    public static void migrarCapasAntigas(Context c) {
        AppDatabase db = AppDatabase.getInstance(c);
        List<LivroEntity> livros = db.livroDao().listarTodos();
        for (LivroEntity l : livros) {
            if (l.imagemPath == null || l.imagemPath.isEmpty()) continue;
            if (pertenceAPasta(c, l.imagemPath)) continue;
            File antiga = new File(l.imagemPath);
            if (!antiga.exists()) continue;
            File nova = salvarOtimizada(c, antiga);
            if (nova != null) {
                db.livroDao().atualizarImagem(l.id, nova.getAbsolutePath());
                apagar(antiga.getAbsolutePath());
            }
        }
    }
}
