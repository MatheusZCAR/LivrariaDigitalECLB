package com.projeto.livrariadigitaleclb.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

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
        version = 11,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    private static AppDatabase instance;

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

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "livraria_db"
                    )
                    .fallbackToDestructiveMigration()
                    .allowMainThreadQueries()
                    .build();
        }
        return instance;
    }
}
