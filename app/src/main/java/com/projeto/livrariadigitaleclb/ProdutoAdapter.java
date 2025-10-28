package com.projeto.livrariadigitaleclb;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ProdutoAdapter extends RecyclerView.Adapter<ProdutoAdapter.ViewHolder> {

    public interface OnProdutoClickListener {
        void onProdutoClick(Produto produto);
    }

    private List<Produto> produtos;
    private OnProdutoClickListener listener;

    public ProdutoAdapter(List<Produto> produtos, OnProdutoClickListener listener) {
        this.produtos = produtos;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_produto, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Produto produto = produtos.get(position);
        holder.image.setImageResource(produto.getImagem());
        holder.title.setText(produto.getTitulo());

        holder.itemView.setOnClickListener(v -> listener.onProdutoClick(produto));
    }

    @Override
    public int getItemCount() {
        return produtos.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView title;
        ViewHolder(View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.imageProduto);
            title = itemView.findViewById(R.id.textTitulo);
        }
    }
}

