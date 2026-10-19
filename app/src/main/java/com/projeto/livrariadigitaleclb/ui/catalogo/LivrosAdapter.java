package com.projeto.livrariadigitaleclb.ui.catalogo;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.projeto.livrariadigitaleclb.R;
import com.projeto.livrariadigitaleclb.data.local.entity.LivroComEstoque;
import com.projeto.livrariadigitaleclb.data.local.entity.LivroEntity;
import com.projeto.livrariadigitaleclb.util.Capas;
import com.projeto.livrariadigitaleclb.util.Moeda;
import com.projeto.livrariadigitaleclb.util.Telas;
import com.projeto.livrariadigitaleclb.util.Texto;

import java.util.ArrayList;
import java.util.List;

/** Grade de livros com capa, preço e estoque. Usada no Catálogo e na tela de Vender. */
public class LivrosAdapter extends RecyclerView.Adapter<LivrosAdapter.VH> {

    public interface AoTocar {
        void tocou(LivroComEstoque item);
    }

    /** Na tela de venda: quantos exemplares deste livro já estão na venda. */
    public interface QuantidadeNaVenda {
        int de(int livroId);
    }

    private final List<LivroComEstoque> itens = new ArrayList<>();
    private final AoTocar aoTocar;
    @Nullable private final QuantidadeNaVenda naVenda;

    public LivrosAdapter(AoTocar aoTocar, @Nullable QuantidadeNaVenda naVenda) {
        this.aoTocar = aoTocar;
        this.naVenda = naVenda;
        setHasStableIds(true);
    }

    public void setItens(List<LivroComEstoque> novos) {
        itens.clear();
        if (novos != null) itens.addAll(novos);
        notifyDataSetChanged();
    }

    public void atualizarLivro(int livroId) {
        for (int i = 0; i < itens.size(); i++) {
            if (itens.get(i).livro.id == livroId) {
                notifyItemChanged(i);
                return;
            }
        }
    }

    @Override
    public long getItemId(int position) {
        return itens.get(position).livro.id;
    }

    @Override
    public int getItemCount() {
        return itens.size();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_livro, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        LivroComEstoque item = itens.get(position);
        LivroEntity livro = item.livro;
        Context c = h.itemView.getContext();
        boolean esgotado = item.quantidade <= 0 || livro.esgotado;

        h.titulo.setText(livro.titulo);
        if (Texto.vazio(livro.autor)) {
            h.autor.setVisibility(View.GONE);
        } else {
            h.autor.setVisibility(View.VISIBLE);
            h.autor.setText(livro.autor);
        }
        h.preco.setText(Moeda.formatar(livro.preco));

        if (esgotado) {
            h.estoque.setText("Sem estoque");
            h.estoque.setTextColor(ContextCompat.getColor(c, R.color.perigo));
            h.banner.setVisibility(View.VISIBLE);
            h.capa.setAlpha(0.55f);
        } else {
            h.estoque.setText(Texto.plural(item.quantidade, "exemplar", "exemplares") + " em estoque");
            h.estoque.setTextColor(ContextCompat.getColor(c, R.color.texto_secundario));
            h.banner.setVisibility(View.GONE);
            h.capa.setAlpha(1f);
        }

        Capas.carregar(h.capa, livro.imagemPath);

        int qtdVenda = naVenda == null ? 0 : naVenda.de(livro.id);
        if (qtdVenda > 0) {
            h.naVenda.setVisibility(View.VISIBLE);
            h.naVenda.setText(qtdVenda + " na venda");
            h.cartao.setStrokeWidth(Telas.dp(c, 4));
        } else {
            h.naVenda.setVisibility(View.GONE);
            h.cartao.setStrokeWidth(0);
        }

        StringBuilder descricao = new StringBuilder(livro.titulo)
                .append(". ").append(Moeda.formatar(livro.preco)).append(". ")
                .append(h.estoque.getText());
        if (qtdVenda > 0) descricao.append(". ").append(qtdVenda).append(" na venda");
        h.cartao.setContentDescription(descricao);

        h.cartao.setOnClickListener(v -> aoTocar.tocou(item));
    }

    static class VH extends RecyclerView.ViewHolder {
        final MaterialCardView cartao;
        final ImageView capa;
        final TextView banner, naVenda, titulo, autor, preco, estoque;

        VH(View v) {
            super(v);
            cartao = v.findViewById(R.id.cartaoLivro);
            capa = v.findViewById(R.id.imgCapaLivro);
            banner = v.findViewById(R.id.txtEsgotadoBanner);
            naVenda = v.findViewById(R.id.txtNaVenda);
            titulo = v.findViewById(R.id.txtTituloLivro);
            autor = v.findViewById(R.id.txtAutorLivro);
            preco = v.findViewById(R.id.txtPrecoLivro);
            estoque = v.findViewById(R.id.txtEstoqueLivro);
        }
    }
}
