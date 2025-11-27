package com.projeto.livrariadigitaleclb.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.projeto.livrariadigitaleclb.room.PedidoEntity;
import com.projeto.livrariadigitaleclb.room.PedidoDao;
import com.projeto.livrariadigitaleclb.LivroEntity;
import com.projeto.livrariadigitaleclb.LivroDao;
import com.projeto.livrariadigitaleclb.Venda;
import com.projeto.livrariadigitaleclb.VendaDao;

@Database(
        entities = {
                PedidoEntity.class,
                LivroEntity.class,
                Venda.class
        },
        version = 10,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    private static AppDatabase instance;

    public abstract PedidoDao pedidoDao();
    public abstract LivroDao livroDao();
    public abstract VendaDao vendaDao();

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
