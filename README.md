# 📚 Livraria Digital ECLB
### Aplicativo Android para gestão da livraria

Aplicativo Android desenvolvido em Java com foco em digitalizar o fluxo de vendas, estoque e pedidos da livraria de uma igreja (ECLB). O app é voltado para **tablets em landscape**, usa **Room** como banco local e ViewBinding para acessar os layouts.

---

## 🔧 Funcionalidades Implementadas

### 📖 Catálogo de Livros
- Exibe todos os livros cadastrados no banco local
- Mostra capa (se houver imagem salva) ou uma imagem padrão
- Botão "Cadastrar Livro" exibido como item especial no grid
- Integração direta com o banco via Room

### ➕ Cadastro de Livros
Tela para cadastrar:
- Título
- Autor
- Código de barras
- Preço
- Leitura de código de barras via biblioteca **ZXing**
- Validação básica dos campos (não permitir campos vazios, preço válido)
- Salvamento como LivroEntity no banco Room

### 🛒 Realizar Venda
- Lista de livros disponíveis para venda em um RecyclerView de 3 colunas
- Busca dinâmica por título ou autor
- Scanner de código de barras:
  - Se o livro existir → abre a tela de conclusão de venda
  - Se não existir → encaminha para a tela de cadastro de livro, já preenchendo o código lido
- Ao tocar em um item, abre a tela **ConcluirVendaActivity** para finalizar a venda

### ✔️ Concluir Venda
- Recebe o livroId via Intent
- Busca o livro correspondente na base (LivroDao.getLivroById)
- Exibe:
  - Título
  - Autor
  - Preço
- Ao confirmar:
  - Cria um registro de Venda com id do livro, título, preço e data/hora da venda (timestamp)
  - Salva no banco via VendaDao
  - Exibe mensagem de sucesso

### 📋 Lista de Pedidos (Reposição)
Tela para controlar livros que precisam ser comprados/repostos.

**Funcionalidades:**
- Adicionar novo pedido (título + autor) via diálogo
- Editar pedido existente
- Remover pedido
- A lista é alimentada a partir da tabela PedidoEntity via PedidoDao
- Geração de **PDF** com todos os itens para impressão:
  - Criação via PdfDocument
  - Salvo em diretório interno da aplicação (`/files/pdfs/lista_pedidos.pdf`)
  - Abre automaticamente usando FileProvider e Intent.ACTION_VIEW

### 📑 Relatórios (estrutura de tela)
Tela RelatoriosActivity com:
- Seleção entre Relatório Diário, Semanal ou Mensal (RadioGroup)
- Botão "Imprimir"

**Estado atual:**
- Exibe uma mensagem (Toast) indicando qual tipo de relatório foi selecionado
- Lógica de geração de relatório ainda não implementada, mas a tela está pronta para isso

---

## 🗄️ Banco de Dados (Room)

Banco local unificado, nome: **livraria_db**

### Configuração principal (AppDatabase)
- Anotação @Database com todas as entidades
- version = 10
- exportSchema = false
- `.fallbackToDestructiveMigration()` habilitado (útil em desenvolvimento)
- `.allowMainThreadQueries()` temporariamente habilitado (para simplicidade)

### Entidades principais em `data.local.entity`
- AutorEntity
- EstoqueEntity
- ItemVendaEntity
- LivroAutorEntity
- LivroEntity
- LocalizacaoEntity
- PedidoEntity
- RelatorioEntity
- RelatorioVendaEntity
- Venda

### DAOs em `data.local.dao`
- AutorDao
- EstoqueDao
- ItemVendaDao
- LivroAutorDao
- LivroDao
- LocalizacaoDao
- PedidoDao
- RelatorioDao
- RelatorioVendaDao
- VendaDao

### Uso atual no app
- **LivroDao** → Catálogo, Cadastro, Realizar Venda, Concluir Venda
- **VendaDao** → Registro das vendas
- **PedidoDao** → Tela de Pedidos
- As demais entidades e DAOs já estão modeladas e prontas para evolução futura do banco, mesmo que ainda não sejam usadas em todas as telas

---

## 📁 Estrutura Atual do Projeto

**Pacote raiz:** `com.projeto.livrariadigitaleclb`

```
com.projeto.livrariadigitaleclb
├── data
│   └── local
│       ├── dao
│       │   ├── AutorDao
│       │   ├── EstoqueDao
│       │   ├── ItemVendaDao
│       │   ├── LivroAutorDao
│       │   ├── LivroDao
│       │   ├── LocalizacaoDao
│       │   ├── PedidoDao
│       │   ├── RelatorioDao
│       │   ├── RelatorioVendaDao
│       │   └── VendaDao
│       ├── entity
│       │   ├── AutorEntity
│       │   ├── EstoqueEntity
│       │   ├── ItemVendaEntity
│       │   ├── LivroAutorEntity
│       │   ├── LivroEntity
│       │   ├── LocalizacaoEntity
│       │   ├── PedidoEntity
│       │   ├── RelatorioEntity
│       │   ├── RelatorioVendaEntity
│       │   └── Venda
│       └── AppDatabase
└── ui
    ├── catalogo
    │   ├── CadastrarLivroActivity
    │   ├── CatalogoActivity
    │   └── LivroAdapter
    ├── concluirvenda
    │   └── ConcluirVendaActivity
    ├── main
    │   └── MainActivity
    ├── pedidos
    │   ├── PedidosActivity
    │   └── PedidosAdapter
    ├── realizarvenda
    │   ├── ProdutoAdapter
    │   └── RealizarVendaActivity
    └── relatorios
        └── RelatoriosActivity
```

---

## 🧪 Dependências Principais

- **AndroidX** (AppCompat, Core KTX, ConstraintLayout, etc.)
- **Room** (runtime + compiler)
- **RecyclerView**
- **ViewBinding**
- **ZXing** (leitura de código de barras)
- **PdfDocument** (API padrão Android para geração de PDF)
- **FileProvider** (para abrir PDF em apps externos)

---

## ▶️ Como Rodar o Projeto

1. **Clonar o repositório:**
   ```bash
   git clone https://github.com/MatheusZCAR/LivrariaDigitalECLB
   ```

2. **Abrir no Android Studio**

3. **Garantir:**
   - JDK configurado (Java 17, conforme projeto)
   - SDK Android atualizado (API 34 ou superior recomendada)

4. **Sincronizar o projeto com o Gradle**

5. **Executar em um emulador ou dispositivo tablet** (orientação landscape)

6. O banco `livraria_db` será criado automaticamente na primeira execução

---

## 🎯 Estado Atual

✔ Aplicativo compila e roda sem erros  
✔ Banco de dados Room unificado e funcionando  
✔ Fluxo completo: Catálogo → Cadastro → Realizar Venda → Concluir Venda  
✔ Lista de Pedidos totalmente funcional + geração de PDF  
✔ Tela de Relatórios pronta visualmente  
✔ Estrutura de pacotes organizada (data.local + ui por funcionalidade)

---

Este README descreve o estado atual do projeto, com todas as funcionalidades já implementadas.
