package com.projeto.livrariadigitaleclb.data.local;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.projeto.livrariadigitaleclb.data.local.dao.AutorDao;
import com.projeto.livrariadigitaleclb.data.local.dao.EstoqueDao;
import com.projeto.livrariadigitaleclb.data.local.dao.ItemVendaDao;
import com.projeto.livrariadigitaleclb.data.local.dao.LivroAutorDao;
import com.projeto.livrariadigitaleclb.data.local.dao.LivroDao;
import com.projeto.livrariadigitaleclb.data.local.dao.LocalizacaoDao;
import com.projeto.livrariadigitaleclb.data.local.dao.PedidoDao;
import com.projeto.livrariadigitaleclb.data.local.dao.RelatorioDao;
import com.projeto.livrariadigitaleclb.data.local.dao.RelatorioVendaDao;
import com.projeto.livrariadigitaleclb.data.local.dao.VendaDao;
import com.projeto.livrariadigitaleclb.data.local.entity.AutorEntity;
import com.projeto.livrariadigitaleclb.data.local.entity.EstoqueEntity;
import com.projeto.livrariadigitaleclb.data.local.entity.ItemVendaEntity;
import com.projeto.livrariadigitaleclb.data.local.entity.LivroAutorEntity;
import com.projeto.livrariadigitaleclb.data.local.entity.LivroEntity;
import com.projeto.livrariadigitaleclb.data.local.entity.LocalizacaoEntity;
import com.projeto.livrariadigitaleclb.data.local.entity.PedidoEntity;
import com.projeto.livrariadigitaleclb.data.local.entity.RelatorioEntity;
import com.projeto.livrariadigitaleclb.data.local.entity.RelatorioVendaEntity;
import com.projeto.livrariadigitaleclb.data.local.entity.Venda;

@Database(
        entities = {
                PedidoEntity.class,
                LivroEntity.class,
                Venda.class,
                AutorEntity.class,
                LocalizacaoEntity.class,
                EstoqueEntity.class,
                ItemVendaEntity.class,
                RelatorioEntity.class,
                RelatorioVendaEntity.class,
                LivroAutorEntity.class
        },
        version = AppDatabase.VERSAO,
        exportSchema = true
)
public abstract class AppDatabase extends RoomDatabase {

    public static final String NOME_ARQUIVO = "livraria_db";
    public static final int VERSAO = 16;

    private static volatile AppDatabase instance;

    public abstract PedidoDao pedidoDao();
    public abstract LivroDao livroDao();
    public abstract VendaDao vendaDao();

    public abstract AutorDao autorDao();
    public abstract LocalizacaoDao localizacaoDao();
    public abstract EstoqueDao estoqueDao();
    public abstract ItemVendaDao itemVendaDao();
    public abstract RelatorioDao relatorioDao();
    public abstract RelatorioVendaDao relatorioVendaDao();
    public abstract LivroAutorDao livroAutorDao();

    /**
     * 15 → 16: a venda passa a guardar quantidade, forma de pagamento e um código que
     * agrupa os livros da mesma venda. Nenhum dado é perdido.
     */
    public static final Migration MIGRACAO_15_16 = new Migration(15, 16) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE vendas ADD COLUMN quantidade INTEGER NOT NULL DEFAULT 1");
            db.execSQL("ALTER TABLE vendas ADD COLUMN formaPagamento TEXT");
            db.execSQL("ALTER TABLE vendas ADD COLUMN condicaoPagamento TEXT");
            db.execSQL("ALTER TABLE vendas ADD COLUMN codigoVenda INTEGER NOT NULL DEFAULT 0");
            // Na versão antiga, todos os exemplares de uma mesma venda tinham o mesmo horário.
            db.execSQL("UPDATE vendas SET codigoVenda = dataVenda");
        }
    };

    public static AppDatabase getInstance(Context context) {
        AppDatabase local = instance;
        if (local == null) {
            synchronized (AppDatabase.class) {
                local = instance;
                if (local == null) {
                    local = construir(context.getApplicationContext());
                    instance = local;
                }
            }
        }
        return local;
    }

    private static AppDatabase construir(Context appContext) {
        return Room.databaseBuilder(appContext, AppDatabase.class, NOME_ARQUIVO)
                .addMigrations(MIGRACAO_15_16)
                // Versões 1 a 14 só existiram durante o desenvolvimento do app.
                // A partir da 15 os dados NUNCA são apagados: toda mudança precisa de uma Migration.
                .fallbackToDestructiveMigrationFrom(true, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14)
                // As consultas são pequenas (catálogo de uma livraria); operações pesadas
                // (backup, restauração) rodam em segundo plano.
                .allowMainThreadQueries()
                .build();
    }

    /**
     * Fecha o banco (usado antes de restaurar uma cópia de segurança).
     * A próxima chamada a {@link #getInstance(Context)} abre de novo.
     */
    public static void fechar() {
        synchronized (AppDatabase.class) {
            if (instance != null) {
                instance.close();
                instance = null;
            }
        }
    }
}
