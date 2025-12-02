package com.projeto.livrariadigitaleclb.ui.catalogo;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.projeto.livrariadigitaleclb.R;
import com.projeto.livrariadigitaleclb.data.local.AppDatabase;
import com.projeto.livrariadigitaleclb.data.local.dao.PedidoDao;
import com.projeto.livrariadigitaleclb.data.local.entity.LivroComEstoque;
import com.projeto.livrariadigitaleclb.data.local.entity.LivroEntity;
import com.projeto.livrariadigitaleclb.data.local.entity.PedidoEntity;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LivroAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_CADASTRAR = 0;
    private static final int VIEW_TYPE_LIVRO = 1;

    private final Context context;
    private final List<LivroComEstoque> livros = new ArrayList<>();

    public LivroAdapter(Context context, List<LivroComEstoque> inicial) {
        this.context = context;
        if (inicial != null) {
            livros.addAll(inicial);
        }
    }

    @Override
    public int getItemCount() {
        return livros.size() + 1;
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? VIEW_TYPE_CADASTRAR : VIEW_TYPE_LIVRO;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);

        if (viewType == VIEW_TYPE_CADASTRAR) {
            View view = inflater.inflate(R.layout.item_cadastrar_livro, parent, false);
            return new CadastrarViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_livro, parent, false);
            return new LivroViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof CadastrarViewHolder) {
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(context, CadastrarLivroActivity.class);
                context.startActivity(intent);
            });
        } else if (holder instanceof LivroViewHolder) {
            int index = position - 1;
            LivroComEstoque itemCompleto = livros.get(index);
            LivroEntity livro = itemCompleto.livro;
            int quantidade = itemCompleto.quantidade;

            LivroViewHolder vh = (LivroViewHolder) holder;
            vh.titulo.setText(livro.titulo);

            if (quantidade <= 0 || livro.esgotado) {
                vh.bannerEsgotado.setVisibility(View.VISIBLE);
                vh.imagem.setAlpha(0.6f);
            } else {
                vh.bannerEsgotado.setVisibility(View.GONE);
                vh.imagem.setAlpha(1.0f);
            }

            if (livro.imagemPath != null && !livro.imagemPath.isEmpty()) {
                File imgFile = new File(livro.imagemPath);
                if (imgFile.exists()) {
                    vh.imagem.setImageURI(Uri.fromFile(imgFile));
                } else {
                    vh.imagem.setImageResource(R.drawable.ic_launcher_background);
                }
            } else {
                vh.imagem.setImageResource(R.drawable.ic_launcher_background);
            }

            vh.itemView.setOnClickListener(v -> mostrarDetalhes(livro, quantidade));
        }
    }

    private void mostrarDetalhes(LivroEntity livro, int quantidade) {
        String autor = (livro.autor != null && !livro.autor.isEmpty()) ? livro.autor : "Autor não informado";
        String local = (livro.localizacao != null && !livro.localizacao.isEmpty()) ? livro.localizacao : "Não informado";
        String codigo = (livro.codigoBarras != null && !livro.codigoBarras.isEmpty()) ? livro.codigoBarras : "Não cadastrado";

        String mensagem = "Autor: " + autor
                + "\nLocal: " + local
                + "\nCódigo: " + codigo
                + "\nPreço: " + String.format(Locale.getDefault(), "R$ %.2f", livro.preco)
                + "\n\nEstoque: " + quantidade + " unidade(s)";

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(livro.titulo);
        builder.setMessage(mensagem);
        builder.setNeutralButton("✏️ Editar", (dialog, which) -> {
            Intent intent = new Intent(context, CadastrarLivroActivity.class);
            intent.putExtra("livro_id", livro.id); // Passa o ID para carregar os dados
            context.startActivity(intent);
        });

        builder.setPositiveButton("Fechar", null);

        if (quantidade <= 0 || livro.esgotado) {
            builder.setNegativeButton("📝 Pedir Reposição", (dialog, which) -> {
                solicitarReposicao(livro);
            });
        }

        builder.show();
    }

    private void solicitarReposicao(LivroEntity livro) {
        // Cria um diálogo com campo de texto numérico
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Reposição: " + livro.titulo);
        builder.setMessage("Digite a quantidade desejada:");

        final EditText input = new EditText(context);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        builder.setView(input);

        builder.setPositiveButton("Adicionar à Lista", (dialog, which) -> {
            String qtdStr = input.getText().toString();
            if (!qtdStr.isEmpty()) {
                int qtd = Integer.parseInt(qtdStr);
                adicionarAoPedido(livro, qtd);
            } else {
                Toast.makeText(context, "Quantidade inválida", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void adicionarAoPedido(LivroEntity livro, int qtd) {
        PedidoDao pedidoDao = AppDatabase.getInstance(context).pedidoDao();

        // Cria o pedido usando o novo construtor que aceita quantidade
        PedidoEntity pedido = new PedidoEntity(livro.titulo, livro.autor, qtd);
        pedidoDao.inserirPedido(pedido);

        Toast.makeText(context, "Adicionado à Lista de Pedidos!", Toast.LENGTH_SHORT).show();
    }

    public void atualizarLista(List<LivroComEstoque> novosLivros) {
        livros.clear();
        if (novosLivros != null) {
            livros.addAll(novosLivros);
        }
        notifyDataSetChanged();
    }

    static class CadastrarViewHolder extends RecyclerView.ViewHolder {
        public CadastrarViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    static class LivroViewHolder extends RecyclerView.ViewHolder {
        TextView titulo;
        ImageView imagem;
        TextView bannerEsgotado;

        public LivroViewHolder(@NonNull View itemView) {
            super(itemView);
            titulo = itemView.findViewById(R.id.txtTituloLivro);
            imagem = itemView.findViewById(R.id.imgCapaLivro);
            bannerEsgotado = itemView.findViewById(R.id.txtEsgotadoBanner);
        }
    }
}
