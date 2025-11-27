package com.projeto.livrariadigitaleclb.ui.realizarvenda;

import android.graphics.BitmapFactory;
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

public class ProdutoAdapter extends RecyclerView.Adapter<ProdutoAdapter.ViewHolder> {

    public interface OnProdutoClickListener {
        void onProdutoClick(LivroEntity livro);
    }

    private final OnProdutoClickListener listener;
    private List<LivroEntity> livros = new ArrayList<>();

    public ProdutoAdapter(List<LivroEntity> livros, OnProdutoClickListener listener) {
        if (livros != null) {
            this.livros.addAll(livros);
        }
        this.listener = listener;
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
        this.livros.clear();
        if (novaLista != null) {
            this.livros.addAll(novaLista);
        }
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
