package com.projeto.livrariadigitaleclb;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.List;

public class ProdutoAdapter extends RecyclerView.Adapter<ProdutoAdapter.ViewHolder> {

    public interface OnProdutoClickListener {
        void onProdutoClick(LivroEntity livro);
    }

    private List<LivroEntity> livros;
    private final OnProdutoClickListener listener;
    private final Context context;

    public ProdutoAdapter(List<LivroEntity> livros, OnProdutoClickListener listener, Context context) {
        this.livros = livros;
        this.listener = listener;
        this.context = context;
    }

    @NonNull
    @Override
    public ProdutoAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_produto, parent, false);
        return new ProdutoAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProdutoAdapter.ViewHolder holder, int position) {
        LivroEntity livro = livros.get(position);

        holder.titulo.setText(livro.titulo);

        if (livro.imagemPath != null && !livro.imagemPath.isEmpty()) {
            File imgFile = new File(livro.imagemPath);
            if (imgFile.exists()) {
                holder.imagem.setImageBitmap(BitmapFactory.decodeFile(imgFile.getPath()));
            } else {
                holder.imagem.setImageResource(R.drawable.caminho_luz);
            }
        } else {
            holder.imagem.setImageResource(R.drawable.caminho_luz);
        }

        holder.itemView.setOnClickListener(v -> listener.onProdutoClick(livro));
    }

    @Override
    public int getItemCount() {
        return livros.size();
    }

    public void updateList(List<LivroEntity> novaLista) {
        this.livros = novaLista;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imagem;
        TextView titulo;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imagem = itemView.findViewById(R.id.imageProduto);
            titulo = itemView.findViewById(R.id.textTitulo);
        }
    }
}
