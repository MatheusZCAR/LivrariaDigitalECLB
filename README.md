# 📚 Livraria Digital ECLB

Aplicativo Android (Java) para a livraria da igreja: catálogo de livros, vendas, lista de pedidos, relatórios e **cópia de segurança no Google Drive**.
Feito para **tablets na horizontal** e pensado para **voluntários idosos**: letras grandes, botões grandes, confirmações claras e opção de desfazer.
Também funciona bem em **celular** (em pé ou deitado): todas as telas usam ConstraintLayout e têm versão para tela estreita.

---

## O que o app faz

| Tela | Para que serve |
|---|---|
| **Vender** | Toque nos livros (ou leia o código de barras) para montar a venda. Aviso com **Desfazer** a cada toque. Mostra quantos de cada livro já estão na venda. |
| **Finalizar venda** | Ajusta quantidades com botões − / +, escolhe **Dinheiro, Pix, Débito ou Crédito**, calcula o **troco** (atalhos “Valor exato”, R$ 20, R$ 50, R$ 100) e as parcelas no crédito. Recibo opcional (imprimir ou enviar). |
| **Livros** | Catálogo com capa, preço e estoque, busca que ignora acentos (“biblia” acha “Bíblia”), cadastro/edição/exclusão de livros, foto da capa (câmera ou galeria), código de barras e “Pedir mais” para a lista de pedidos. |
| **Pedidos** | Lista do que comprar. Pedir o mesmo livro de novo soma a quantidade. Imprimir ou enviar a lista em PDF. |
| **Relatórios** | Hoje / esta semana / este mês / datas escolhidas: total vendido, número de vendas, livros vendidos, por forma de pagamento, mais vendidos e a lista de vendas — com **Cancelar venda** (os livros voltam ao estoque). PDF para imprimir ou enviar. |
| **Cópia de segurança** | Backup no Google Drive (manual, diário e ~30 min depois de cada venda) e restauração. Também salva/restaura de um arquivo. |

---

## ☁️ Configurar o backup no Google Drive (uma vez só)

O app usa a API oficial do Google Drive com a permissão **`drive.file`**: ele só enxerga a pasta que ele mesmo cria
(**“Livraria ECLB - Cópias de segurança”**) — nada mais do Drive da conta.

Para o Google permitir o login, o app precisa ser registrado num projeto do Google Cloud:

1. Acesse <https://console.cloud.google.com/> e crie um projeto (ex.: `livraria-eclb`).
2. **APIs e serviços → Biblioteca →** procure **Google Drive API** → **Ativar**.
3. **APIs e serviços → Tela de consentimento OAuth** (Google Auth Platform):
   - Tipo de usuário: **Externo**; nome do app: *Livraria ECLB*; e-mail de suporte: o seu.
   - Em **Escopos/Acesso a dados**, adicione `.../auth/drive.file` (é um escopo *não sensível*, não precisa de verificação do Google).
   - Em **Público-alvo**, clique em **Publicar app** (ou adicione as contas da livraria como *usuários de teste*; em modo de teste a autorização expira a cada 7 dias, por isso é melhor publicar).
4. **Credenciais → Criar credenciais → ID do cliente OAuth → Android**:
   - Nome do pacote: `com.projeto.livrariadigitaleclb`
   - Impressão digital SHA-1 da chave que assina o APK instalado no tablet. Para ver:
     ```bash
     ./gradlew signingReport
     ```
     (use o SHA-1 da variante que você instala: `debug` ou `release`).
5. Pronto. No tablet: **Início → Cópia de segurança → Conectar ao Google Drive**, escolha a conta da livraria e autorize.

> **Importante:** se a chave de assinatura mudar, o SHA-1 muda e o login falha com “erro de configuração (código 10)”.
> Cadastre um ID de cliente Android para cada chave que você usar (pode ter vários no mesmo projeto).

### APK pelo GitHub Actions com assinatura fixa

Cada execução do GitHub Actions gera uma chave *debug* nova, então o APK baixado de lá não bate com o SHA-1 cadastrado.
Para resolver, guarde a sua chave debug como segredo do repositório:

```bash
base64 -w0 ~/.android/debug.keystore   # copie a saída
```

GitHub → **Settings → Secrets and variables → Actions → New repository secret** → nome `DEBUG_KEYSTORE_BASE64`.
O passo “Assinatura do APK” do workflow mostra o SHA-1 usado.

### Como o backup funciona

- O arquivo é um `.zip` com o banco de dados, as fotos das capas e um `manifesto.json` (data, versão, nº de livros e vendas).
- O Drive mantém as **20 cópias mais recentes**; as mais antigas são apagadas automaticamente.
- A cópia automática roda pelo **WorkManager** quando há internet e bateria (diária + após vendas/cadastros).
- Ao **restaurar**, o app guarda antes uma cópia do estado atual em `files/seguranca/antes_de_restaurar.zip`.
- Sem Google Drive configurado, use **Salvar cópia em arquivo** (o seletor do Android permite escolher o próprio Drive, um pendrive etc.).
- O backup automático do Android (`allowBackup`) também está configurado como proteção extra.

---

## Para desenvolvedores

### Rodar
1. Android Studio recente (AGP 8.13, Gradle 8.13), JDK 17.
2. Abra o projeto, sincronize o Gradle e rode num tablet ou emulador de tablet.

Para gerar release assinado, crie `keystore.properties` na raiz (já está no `.gitignore`):
```properties
storeFile=caminho/para/livraria.jks
storePassword=...
keyAlias=...
keyPassword=...
```

### Estrutura
```
com.projeto.livrariadigitaleclb
├── App.java                     agenda backups e migra capas antigas
├── backup/                      BackupArquivo (zip), DriveClient (REST v3), DriveAuth,
│                                BackupWorker, BackupAgendador, BackupPrefs, BackupServico
├── data/
│   ├── Carrinho, VendaService, PedidosService, RelatorioVendas
│   └── local/ (Room: AppDatabase, dao/, entity/)
├── ui/  BaseActivity + main, catalogo, realizarvenda, concluirvenda, pedidos, relatorios, backup
└── util/ Moeda, Texto, Periodo, Capas, PdfEscritor, Documentos, Avisos, Dialogos, SeletorQuantidade, Telas
```

### Telas: celular e tablet
- Todas as telas usam **ConstraintLayout**.
- `res/layout/` tem a versão para **telas estreitas** (celular, tablet pequeno em pé).
  `res/layout-w840dp-h480dp/` tem a versão para **telas largas** (tablet deitado): painéis lado a lado.
  As duas versões precisam ter **os mesmos IDs** (o lint avisa se não tiverem).
- Tamanhos de letra e botões: `values/dimens.xml` (celular) e `values-sw600dp/dimens.xml` (tablet).
  Textos mais longos para tela larga ficam em `values-w840dp-h480dp/strings.xml`.
- No tablet a tela fica travada na horizontal; no celular segue o aparelho (veja `BaseActivity`).
  A partir do Android 16 o sistema pode ignorar essa trava em tablets, por isso as telas se adaptam à largura.

### Banco de dados (Room) — regras
- Versão atual: **16**. O esquema de cada versão é exportado em `app/schemas/`.
- **Nunca** use `fallbackToDestructiveMigration()`: ele apagava todos os dados a cada mudança.
  Toda mudança de entidade precisa de uma `Migration` nova (veja `AppDatabase.MIGRACAO_15_16`) e de um teste em `DadosSegurosTest`.
- Tabelas `autores`, `localizacoes`, `itens_venda`, `relatorios`, `relatorio_venda` e `livro_autor` existem mas ainda não são usadas.

### Testes
- `./gradlew testDebugUnitTest` — regras de dinheiro, busca, períodos, carrinho e relatórios.
- `./gradlew connectedDebugAndroidTest` — abre todas as telas num tablet, faz uma venda completa, testa a migração 15→16 e o ciclo backup → restauração.
- O GitHub Actions roda tudo isso a cada push (tablet com Android 9 e 15, celular com Android 15), guarda as screenshots de cada tela e gera o APK como artefato.
