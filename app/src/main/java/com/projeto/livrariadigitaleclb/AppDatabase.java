package com.projeto.livrariadigitaleclb;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {LivroEntity.class, Venda.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {

    private static AppDatabase instance;

    public abstract LivroDao livroDao();
    public abstract VendaDao vendaDao();

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "livraria_db"
                    )
                    .allowMainThreadQueries() // depois trocamos para async
                    .build();
        }
        return instance;
    }
}

