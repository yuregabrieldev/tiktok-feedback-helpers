# Mood — entrega das fases 1 e 2

6 de novembro de 2026. Base: `mood-source (1).zip` e `AUDITORIA_MOOD.md`. Código alterado no projeto existente; Proposta B preservada, sem migração de framework.

## Alterações

- Vite padrão autónomo com TanStack Start/React/Tailwind/Nitro para Node SSR. Removidos pacote Lovable, hooks de telemetria do editor, configuração duplicada e plugin de aliases redundante. Lockfile Bun atualizado e instalação limpa verificada.
- Scripts `dev`, `build`, `start`, `typecheck`, `check` e `test:browser`; versões Node/Bun documentadas e `.env.example` sem segredos.
- Overflow da Home a 320 px corrigido pela largura intrínseca do placeholder e dos itens da grelha, sem esconder overflow globalmente.
- Contrastes secundários corrigidos na biblioteca, rodapé e passos da candidatura. Foco visível em campos e controlos, foco escuro na biblioteca marfim, skip link com destino focável e menu móvel com altura limitada/scroll próprio.
- Títulos de listagem e estados vazios sem saltar níveis; tabelas com caption/headers; anúncios de filtros; mensagem de validação demonstrativa limpa ao editar campos.
- Metadados por rota, títulos sem marca duplicada, descrições que identificam o estado demonstrativo, Open Graph e Twitter com a imagem ilustrativa existente, canonical e canonical da comparação apontando à Home.
- Rotas de servidor `/robots.txt` e `/sitemap.xml`; noindex em HTML e cabeçalho HTTP. Indexação desativada por defeito; exemplos/admin/recibo permanecem excluídos mesmo quando a indexação institucional for ativada.
- Erros/404 em português com paleta e linguagem visual B. Fontes locais limitadas ao subset Latin mantendo famílias e pesos. CSS de fontes principal adicional caiu de 42,39 kB para 1,82 kB; o ficheiro de estilos fica aproximadamente em 120,52 kB. Isto não é uma medição de Core Web Vitals.
- Seis avisos ESLint resolvidos separando variantes/contextos/hooks em módulos próprios; nenhuma regra foi desativada. Removidos apenas imports/constantes e estilos móveis antigos sem utilização; componentes reutilizáveis e a referência A foram mantidos.

## Verificação

| Verificação | Resultado |
|---|---|
| Instalação em pasta limpa com `bun install --frozen-lockfile` | Passou, sem pacote Lovable |
| `npm run build` | Passou, SSR Node |
| `npm run typecheck` | Passou |
| `npm run lint` | Passou, zero erros e zero avisos |
| `npm test` | 27 testes, 3 ficheiros; os 22 existentes preservados |
| `npm run dev -- --host 0.0.0.0 --port 3001` | Arrancou e Home respondeu 200 |
| `npm start` | Servidor de produção local em `http://localhost:3000/` |
| Regressão Chromium | 20 URLs de página + sitemap/robots; estados 200/404 esperados |
| Layouts e axe | 8 páginas × 320/390/768/1440; sem overflow/imagens quebradas/violações nas regras WCAG testadas |
| Interações | Skip link, Enter/Escape, foco de campos, validação HTML, limpeza de validação antiga, filtros/estado vazio e abas do admin |

As 20 URLs incluem as 17 existentes e três casos de 404. Login e páginas legais continuam ausentes, conforme o âmbito. O script de auditoria adicional também verificou essas ausências. Os resultados não equivalem a certificação WCAG; Safari/iPhone, Firefox, leitores de ecrã, zoom extensivo e desempenho em dispositivos reais permanecem para homologação.

## Configuração SEO ainda necessária

Não foi inventado um domínio Mood. Sem `VITE_SITE_URL`, canonical e imagens sociais usam referências relativas para funcionar localmente. Configurar a origem aprovada e reconstruir para obter URLs absolutas de partilha. Manter `VITE_ALLOW_INDEXING=false` até o conteúdo público ser aprovado. Nesse modo o sitemap é válido e vazio, deliberadamente. A ativação futura inclui apenas os seis caminhos institucionais/listagens definidos em `src/lib/seo.ts`; não inclui dados de exemplo.

## Próxima fase

`PROXIMA_FASE.md` prepara decisões, modelo, autenticação, permissões, RLS, armazenamento privado e testes do backend. Não foram instalados clientes Supabase, criadas contas, autenticação, bases de dados, serviços pagos ou publicação. O formulário continua sem envio e o admin continua uma demonstração pública sem dados reais.

Evidências desta sessão: `/workspace/mood-phase12-evidence/` e `artifacts/browser/results.json`. Os comandos Bun equivalentes aos usados por npm executam os mesmos scripts e binários locais.
