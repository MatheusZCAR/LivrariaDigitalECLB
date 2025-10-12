# 📚 Livraria Digital ECLB

Aplicativo Android para tablets, projetado para gerenciamento e vendas de livros. Construído em Java com layout responsivo usando ConstraintLayout, suporte a ViewBinding e arquitetura modular, o projeto tem como foco uma UI simples, intuitiva e totalmente horizontal.

---

## 📌 Funcionalidades já implementadas

### 🏠 MainActivity
Tela inicial com 4 botões de acesso rápido:

- **Catálogo**: abre a tela de gerenciamento de livros
- **Realizar venda**: (placeholder)
- **Pedidos**: (placeholder)
- **Relatórios**: (placeholder)

✅ Interface visual idêntica ao mockup  
✅ Ícones com textos centralizados abaixo  
✅ Navegação funcional para `CatalogoActivity`  

---

### 📘 CatalogoActivity

Tela que exibe os livros disponíveis e o botão de "Cadastrar Livro".

**Componentes:**
- Barra superior com título "catálogo" e ícone da casa (home) para voltar
- **Botão “Cadastrar Livro”** com ícone de "+" e texto “cadastrar livro”
- `RecyclerView` com `GridLayoutManager` (colunas: 3)
- Visual pronto para listar livros como na segunda print fornecida

**Navegação:**
- Botão catálogo da tela principal abre essa tela
- Botão "home" no topo retorna para a `MainActivity`

---

## ⚙️ Tecnologias & Arquitetura

| Tecnologia | Detalhes |
|------------|----------|
| 👨‍💻 Java | Linguagem principal |
| 🧱 ConstraintLayout | Usado em todas as telas e itens |
| 🧼 ViewBinding | Ativado no projeto (`ActivityMainBinding`, `ActivityCatalogoBinding`, etc) |
| 🧭 Navegação | Por `Intent` entre Activities |
| 📦 RecyclerView | Implementado com ViewHolder e múltiplos layouts (botão + livros) |
| 💅 Design responsivo | Foco em tablets, app travado em **modo paisagem (landscape)** |
| 🎨 Ícones `.png` | Armazenados em `/res/drawable/` com nomes organizados |

---

## 📁 Estrutura de Pastas

```
com.projeto.livrariadigitaleclb/
├── CatalogoActivity.java
├── FirstFragment.java
├── Livro.java
├── LivroAdapter.java
├── MainActivity.java
├── SecondFragment.java

com.projeto.livrariadigitaleclb (androidTest)/
com.projeto.livrariadigitaleclb (test)/

res/
├── drawable/
│   ├── borda_pontilhada.xml
│   ├── ic_cadastrar_livro.png
│   ├── ic_catalogo.png
│   ├── ic_home.png
│   ├── ic_launcher_background.xml
│   ├── ic_launcher_foreground.xml
│   ├── ic_pedidos.png
│   ├── ic_relatorios.png
│   └── ic_venda.png
│
├── layout/
│   ├── activity_catalogo.xml
│   ├── activity_main.xml
│   ├── content_catalogo.xml
│   ├── fragment_first.xml
│   ├── fragment_second.xml
│   ├── item_cadastrar_livro.xml
│   └── item_livro.xml

```


