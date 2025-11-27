package com.projeto.livrariadigitaleclb;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import com.projeto.livrariadigitaleclb.room.PedidoEntity;

import java.util.List;

public class PedidosAdapter extends BaseAdapter {

    private Context context;
    private List<PedidoEntity> lista;
    private PedidosActivity activity;

    public PedidosAdapter(Context context, List<PedidoEntity> lista, PedidosActivity activity) {
        this.context = context;
        this.lista = lista;
        this.activity = activity;
    }

    @Override
    public int getCount() {
        return lista.size();
    }

    @Override
    public Object getItem(int position) {
        return lista.get(position);
    }

    @Override
    public long getItemId(int position) {
        return lista.get(position).id;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {

        if (view == null) {
            view = LayoutInflater.from(context)
                    .inflate(R.layout.item_pedido, parent, false);
        }

        TextView txtInfo = view.findViewById(R.id.txtInfo);
        ImageView btnEdit = view.findViewById(R.id.btnEdit);
        ImageView btnDelete = view.findViewById(R.id.btnDelete);

        PedidoEntity pedido = lista.get(position);

        txtInfo.setText(pedido.titulo + " – " + pedido.autor);

        btnEdit.setOnClickListener(v -> activity.abrirDialogEditarLivro(pedido));

        btnDelete.setOnClickListener(v ->
                activity.confirmarExclusao(pedido)
        );

        return view;
    }
}
