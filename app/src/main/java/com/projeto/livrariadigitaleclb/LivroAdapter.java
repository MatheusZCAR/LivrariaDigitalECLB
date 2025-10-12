package com.projeto.livrariadigitaleclb;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class LivroAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<Livro> lista;
    private final Context context;
    private static final int VIEW_TYPE_CADASTRAR = 0;
    private static final int VIEW_TYPE_LIVRO = 1;

    public LivroAdapter(List<Livro> lista, Context context) {
        this.lista = lista;
        this.context = context;
    }

    @Override
    public int getItemViewType(int position) {
        return lista.get(position).isBotaoCadastrar ? VIEW_TYPE_CADASTRAR : VIEW_TYPE_LIVRO;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_CADASTRAR) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_cadastrar_livro, parent, false);
            return new CadastrarViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_livro, parent, false);
            return new LivroViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Livro item = lista.get(position);
        if (holder instanceof CadastrarViewHolder) {
            // ação de cadastrar livro
            holder.itemView.setOnClickListener(v -> {
                // TODO: abrir tela de cadastro
            });
        } else if (holder instanceof LivroViewHolder) {
            ((LivroViewHolder) holder).titulo.setText(item.titulo);
            // TODO: carregar imagem se tiver
        }
    }

    @Override
    public int getItemCount() {
        return lista.size();
    }

    static class CadastrarViewHolder extends RecyclerView.ViewHolder {
        public CadastrarViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    static class LivroViewHolder extends RecyclerView.ViewHolder {
        TextView titulo;
        ImageView imagem;

        public LivroViewHolder(@NonNull View itemView) {
            super(itemView);
            titulo = itemView.findViewById(R.id.txtTituloLivro);
            imagem = itemView.findViewById(R.id.imgCapaLivro);
        }
    }
}
