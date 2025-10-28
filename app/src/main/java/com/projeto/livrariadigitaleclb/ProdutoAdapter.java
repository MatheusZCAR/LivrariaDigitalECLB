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

    private List<Produto> lista;
    private OnProdutoClickListener listener;

    public interface OnProdutoClickListener {
        void onProdutoClick(Produto produto);
    }

    public ProdutoAdapter(List<Produto> lista, OnProdutoClickListener listener) {
        this.lista = lista;
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
        Produto produto = lista.get(position);
        holder.txtTitulo.setText(produto.getTitulo());
        holder.txtAutor.setText(produto.getAutor());
        holder.txtPreco.setText(String.format("R$ %.2f", produto.getPreco()));
        holder.imgProduto.setImageResource(produto.getImagemRes());

        holder.itemView.setOnClickListener(v -> listener.onProdutoClick(produto));
    }

    @Override
    public int getItemCount() {
        return lista.size();
    }

    public void atualizarLista(List<Produto> novaLista) {
        this.lista = novaLista;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtTitulo, txtAutor, txtPreco;
        ImageView imgProduto;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtTitulo = itemView.findViewById(R.id.txtTitulo);
            txtAutor = itemView.findViewById(R.id.txtAutor);
            txtPreco = itemView.findViewById(R.id.txtPreco);
            imgProduto = itemView.findViewById(R.id.imgProduto);
        }
    }
}
