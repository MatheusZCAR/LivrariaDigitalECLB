package com.projeto.livrariadigitaleclb.ui.realizarvenda;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.projeto.livrariadigitaleclb.R;
import com.projeto.livrariadigitaleclb.data.local.entity.LivroComEstoque;
import com.projeto.livrariadigitaleclb.data.local.entity.LivroEntity;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ProdutoAdapter extends RecyclerView.Adapter<ProdutoAdapter.ViewHolder> {
    public interface OnProdutoClickListener {
        void onProdutoClick(LivroEntity livro, int estoqueDisponivel);
    }

    private final OnProdutoClickListener listener;
    private final List<LivroComEstoque> livros = new ArrayList<>();

    public ProdutoAdapter(List<LivroComEstoque> livrosIniciais, OnProdutoClickListener listener) {
        if (livrosIniciais != null) {
            this.livros.addAll(livrosIniciais);
        }
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_livro, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LivroComEstoque itemCompleto = livros.get(position);
        LivroEntity livro = itemCompleto.livro;
        int quantidade = itemCompleto.quantidade;

        holder.titulo.setText(livro.titulo);

        if (quantidade <= 0) {
            holder.bannerEsgotado.setVisibility(View.VISIBLE);
            holder.imagem.setAlpha(0.6f);
        } else {
            holder.bannerEsgotado.setVisibility(View.GONE);
            holder.imagem.setAlpha(1.0f);
        }

        if (livro.imagemPath != null && !livro.imagemPath.isEmpty()) {
            File imgFile = new File(livro.imagemPath);
            if (imgFile.exists()) {
                holder.imagem.setImageURI(Uri.fromFile(imgFile));
            } else {
                holder.imagem.setImageResource(R.drawable.ic_launcher_background);
            }
        } else {
            holder.imagem.setImageResource(R.drawable.ic_launcher_background);
        }

        // Passa o livro e a quantidade atual para a Activity validar
        holder.itemView.setOnClickListener(v -> listener.onProdutoClick(livro, quantidade));
    }

    @Override
    public int getItemCount() {
        return livros.size();
    }

    public void updateList(List<LivroComEstoque> novaLista) {
        this.livros.clear();
        if (novaLista != null) {
            this.livros.addAll(novaLista);
        }
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imagem;
        TextView titulo;
        TextView bannerEsgotado;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imagem = itemView.findViewById(R.id.imgCapaLivro);
            titulo = itemView.findViewById(R.id.txtTituloLivro);
            bannerEsgotado = itemView.findViewById(R.id.txtEsgotadoBanner);
        }
    }
}
