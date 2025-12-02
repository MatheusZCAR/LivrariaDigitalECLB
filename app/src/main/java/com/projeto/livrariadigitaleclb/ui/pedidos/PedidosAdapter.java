package com.projeto.livrariadigitaleclb.ui.pedidos;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.projeto.livrariadigitaleclb.R;
import com.projeto.livrariadigitaleclb.data.local.entity.PedidoEntity;

import java.util.ArrayList;
import java.util.List;

public class PedidosAdapter extends BaseAdapter {

    private final Context context;
    private List<PedidoEntity> lista;
    private final PedidosActivity activity;

    public PedidosAdapter(Context context, List<PedidoEntity> lista, PedidosActivity activity) {
        this.context = context;
        this.lista = new ArrayList<>(lista);
        this.activity = activity;
    }

    @Override
    public int getCount() {
        return lista.size();
    }

    @Override
    public PedidoEntity getItem(int position) {
        return lista.get(position);
    }

    @Override
    public long getItemId(int position) {
        return lista.get(position).id;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = LayoutInflater.from(context)
                    .inflate(R.layout.item_pedido, parent, false);

            holder = new ViewHolder();
            holder.txtInfo = convertView.findViewById(R.id.txtInfo);
            holder.btnEdit = convertView.findViewById(R.id.btnEdit);
            holder.btnDelete = convertView.findViewById(R.id.btnDelete);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        PedidoEntity pedido = lista.get(position);

        String texto = pedido.titulo + "\n" + pedido.autor + " (Qtd: " + pedido.quantidadeDesejada + ")";
        holder.txtInfo.setText(texto);

        holder.btnEdit.setOnClickListener(v ->
                activity.abrirDialogEditarLivro(pedido)
        );

        holder.btnDelete.setOnClickListener(v ->
                activity.confirmarExclusao(pedido)
        );

        return convertView;
    }

    public void atualizarLista(List<PedidoEntity> novaLista) {
        this.lista = new ArrayList<>(novaLista);
        notifyDataSetChanged();
    }

    private static class ViewHolder {
        TextView txtInfo;
        ImageView btnEdit;
        ImageView btnDelete;
    }
}
