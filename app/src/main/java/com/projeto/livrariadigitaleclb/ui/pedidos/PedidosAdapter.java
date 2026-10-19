package com.projeto.livrariadigitaleclb.ui.pedidos;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.projeto.livrariadigitaleclb.R;
import com.projeto.livrariadigitaleclb.data.local.entity.PedidoEntity;
import com.projeto.livrariadigitaleclb.util.Texto;

import java.util.ArrayList;
import java.util.List;

public class PedidosAdapter extends RecyclerView.Adapter<PedidosAdapter.VH> {

    public interface Acoes {
        void editar(PedidoEntity pedido);
        void excluir(PedidoEntity pedido);
    }

    private final List<PedidoEntity> lista = new ArrayList<>();
    private final Acoes acoes;

    public PedidosAdapter(Acoes acoes) {
        this.acoes = acoes;
    }

    public void atualizarLista(List<PedidoEntity> novaLista) {
        lista.clear();
        lista.addAll(novaLista);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pedido, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        PedidoEntity p = lista.get(position);
        h.quantidade.setText(String.valueOf(p.quantidadeDesejada));
        h.titulo.setText(p.titulo);
        if (Texto.vazio(p.autor)) {
            h.autor.setVisibility(View.GONE);
        } else {
            h.autor.setVisibility(View.VISIBLE);
            h.autor.setText(p.autor);
        }
        h.itemView.setContentDescription(Texto.plural(p.quantidadeDesejada, "exemplar", "exemplares")
                + " de " + p.titulo);
        h.editar.setOnClickListener(v -> acoes.editar(p));
        h.excluir.setOnClickListener(v -> acoes.excluir(p));
    }

    @Override
    public int getItemCount() {
        return lista.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView quantidade, titulo, autor;
        final View editar, excluir;

        VH(View v) {
            super(v);
            quantidade = v.findViewById(R.id.txtQuantidade);
            titulo = v.findViewById(R.id.txtTitulo);
            autor = v.findViewById(R.id.txtAutor);
            editar = v.findViewById(R.id.btnEdit);
            excluir = v.findViewById(R.id.btnDelete);
        }
    }
}
