package com.projeto.livrariadigitaleclb.ui.realizarvenda;

import android.graphics.BitmapFactory;
import android.graphics.Color; // Importante para as cores
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

    // NOVO: Lista para controlar visualmente quem está selecionado
    private List<Integer> idsSelecionados = new ArrayList<>();

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

        // Carregamento da imagem (mantido igual)
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

        // NOVO: Verifica se está selecionado e altera o visual
        boolean isSelected = idsSelecionados.contains(livro.id);

        if (isSelected) {
            // Estilo quando selecionado (Fundo cinza e imagem mais clara)
            holder.itemView.setBackgroundColor(Color.parseColor("#E0E0E0"));
            holder.imagem.setAlpha(0.5f); // 50% de opacidade
        } else {
            // Estilo padrão (Fundo normal e imagem nítida)
            holder.itemView.setBackgroundColor(Color.TRANSPARENT);
            holder.imagem.setAlpha(1.0f);
        }

        // NOVO: Lógica de clique atualizada
        holder.itemView.setOnClickListener(v -> {
            // Atualiza a lista interna de seleção visual
            if (idsSelecionados.contains(livro.id)) {
                idsSelecionados.remove((Integer) livro.id);
            } else {
                idsSelecionados.add(livro.id);
            }

            // Avisa o RecyclerView para redesenhar APENAS este item (performance)
            notifyItemChanged(holder.getAdapterPosition());

            // Chama o listener da Activity para a lógica de negócio
            listener.onProdutoClick(livro);
        });
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

    // Opcional: Método para limpar a seleção se você cancelar a venda
    public void limparSelecao() {
        idsSelecionados.clear();
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