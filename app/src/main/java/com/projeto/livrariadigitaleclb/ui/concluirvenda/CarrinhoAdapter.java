package com.projeto.livrariadigitaleclb.ui.concluirvenda;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.projeto.livrariadigitaleclb.R;

import java.util.List;

public class CarrinhoAdapter extends RecyclerView.Adapter<CarrinhoAdapter.ViewHolder> {

    private List<ConcluirVendaActivity.ItemCarrinho> itens;
    private OnQuantidadeChangeListener quantidadeListener;
    private OnRemoverItemListener removerListener;

    public interface OnQuantidadeChangeListener {
        void onQuantidadeChanged(ConcluirVendaActivity.ItemCarrinho item, int novaQuantidade);
    }

    public interface OnRemoverItemListener {
        void onRemover(ConcluirVendaActivity.ItemCarrinho item);
    }

    public CarrinhoAdapter(List<ConcluirVendaActivity.ItemCarrinho> itens,
                           OnQuantidadeChangeListener quantidadeListener,
                           OnRemoverItemListener removerListener) {
        this.itens = itens;
        this.quantidadeListener = quantidadeListener;
        this.removerListener = removerListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_carrinho, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ConcluirVendaActivity.ItemCarrinho item = itens.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return itens.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgCapa;
        TextView txtTitulo;
        TextView txtAutor;
        TextView txtPreco;
        TextView txtQuantidade;
        TextView txtSubtotal;
        ImageButton btnDiminuir;
        ImageButton btnAumentar;
        ImageButton btnRemover;

        ViewHolder(View itemView) {
            super(itemView);
            imgCapa = itemView.findViewById(R.id.imgCapa);
            txtTitulo = itemView.findViewById(R.id.txtTitulo);
            txtAutor = itemView.findViewById(R.id.txtAutor);
            txtPreco = itemView.findViewById(R.id.txtPreco);
            txtQuantidade = itemView.findViewById(R.id.txtQuantidade);
            txtSubtotal = itemView.findViewById(R.id.txtSubtotal);
            btnDiminuir = itemView.findViewById(R.id.btnDiminuir);
            btnAumentar = itemView.findViewById(R.id.btnAumentar);
            btnRemover = itemView.findViewById(R.id.btnRemover);
        }

        void bind(ConcluirVendaActivity.ItemCarrinho item) {
            txtTitulo.setText(item.livro.titulo);
            // txtAutor.setText(item.livro.autor); // Ajuste conforme sua estrutura
            txtPreco.setText(String.format("R$ %.2f", item.livro.preco));
            txtQuantidade.setText(String.valueOf(item.quantidade));
            txtSubtotal.setText(String.format("R$ %.2f", item.getSubtotal()));

            // Configura a capa do livro se disponível
            // imgCapa.setImageResource(...); // Implemente conforme sua lógica

            btnDiminuir.setOnClickListener(v -> {
                int novaQtd = item.quantidade - 1;
                quantidadeListener.onQuantidadeChanged(item, novaQtd);
            });

            btnAumentar.setOnClickListener(v -> {
                int novaQtd = item.quantidade + 1;
                quantidadeListener.onQuantidadeChanged(item, novaQtd);
            });

            btnRemover.setOnClickListener(v -> {
                removerListener.onRemover(item);
            });
        }
    }
}
