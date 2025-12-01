package com.projeto.livrariadigitaleclb.ui.catalogo;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.projeto.livrariadigitaleclb.R;
import com.projeto.livrariadigitaleclb.data.local.entity.LivroEntity;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class LivroAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_CADASTRAR = 0;
    private static final int VIEW_TYPE_LIVRO = 1;

    private final Context context;
    private final List<LivroEntity> livros = new ArrayList<>();

    public LivroAdapter(Context context, List<LivroEntity> inicial) {
        this.context = context;
        if (inicial != null) {
            livros.addAll(inicial);
        }
    }

    @Override
    public int getItemCount() {
        return livros.size() + 1;
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? VIEW_TYPE_CADASTRAR : VIEW_TYPE_LIVRO;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);

        if (viewType == VIEW_TYPE_CADASTRAR) {
            View view = inflater.inflate(R.layout.item_cadastrar_livro, parent, false);
            return new CadastrarViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_livro, parent, false);
            return new LivroViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof CadastrarViewHolder) {
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(context, CadastrarLivroActivity.class);
                context.startActivity(intent);
            });
        } else if (holder instanceof LivroViewHolder) {
            int index = position - 1;
            LivroEntity livro = livros.get(index);

            LivroViewHolder vh = (LivroViewHolder) holder;
            vh.titulo.setText(livro.titulo);

            // Carregar a imagem se existir
            if (livro.imagemPath != null && !livro.imagemPath.isEmpty()) {
                File imgFile = new File(livro.imagemPath);
                if (imgFile.exists()) {
                    Uri photoUri = Uri.fromFile(imgFile);
                    vh.imagem.setImageURI(photoUri);
                } else {
                    vh.imagem.setImageResource(R.drawable.ic_launcher_background);
                }
            } else {
                vh.imagem.setImageResource(R.drawable.ic_launcher_background);
            }
        }
    }

    public void atualizarLista(List<LivroEntity> novosLivros) {
        livros.clear();
        if (novosLivros != null) {
            livros.addAll(novosLivros);
        }
        notifyDataSetChanged();
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