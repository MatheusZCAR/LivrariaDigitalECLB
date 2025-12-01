package com.projeto.livrariadigitaleclb.ui.concluirvenda;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.projeto.livrariadigitaleclb.R;

import java.io.File;
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
            txtAutor.setText(item.livro.autor != null ? item.livro.autor : "Autor desconhecido");
            txtPreco.setText(String.format("R$ %.2f", item.livro.preco));
            txtQuantidade.setText(String.valueOf(item.quantidade));
            txtSubtotal.setText(String.format("R$ %.2f", item.getSubtotal()));

            // Carrega a imagem do livro
            carregarImagemLivro(item);

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

        private void carregarImagemLivro(ConcluirVendaActivity.ItemCarrinho item) {
            // Tenta carregar a imagem se o livro tiver uma foto cadastrada
            if (item.livro.imagemPath != null && !item.livro.imagemPath.isEmpty()) {
                File imgFile = new File(item.livro.imagemPath);

                if (imgFile.exists()) {
                    try {
                        // Carrega a imagem do arquivo
                        Uri imageUri = Uri.fromFile(imgFile);
                        imgCapa.setImageURI(imageUri);

                        // Força o refresh da imageview
                        imgCapa.invalidate();
                        return; // Sucesso, sai do método

                    } catch (Exception e) {
                        e.printStackTrace();
                        // Se der erro, continua para usar placeholder
                    }
                }
            }

            // Se não tiver imagem ou erro ao carregar, usa placeholder
            usarPlaceholder(item);
        }

        private void usarPlaceholder(ConcluirVendaActivity.ItemCarrinho item) {
            // Lista de placeholders disponíveis
            int[] placeholders = {
                    R.drawable.caminho_luz,
                    R.drawable.anoitecer_aline,
                    R.drawable.encontrei_voce
            };

            // Usa um placeholder baseado no ID do livro
            int placeholderIndex = Math.abs(item.livro.id % placeholders.length);
            imgCapa.setImageResource(placeholders[placeholderIndex]);
        }
    }
}