# Mood — auditoria técnica e plano de backend

6 de novembro de 2026 · Auditoria local, sem publicação

## Diagnóstico

É possível continuar a partir do código existente e sair do Lovable sem reconstruir o website. A versão `mood-source (1).zip` compila e funciona como aplicação SSR autónoma com TanStack Start, React 19, TypeScript e Tailwind. A configuração autónoma usa plugins de código aberto; o servidor Node respondeu corretamente às rotas existentes. Não foi necessário login no Lovable, chave de API ou serviço remoto para navegar.

O website é, neste momento, um protótipo de apresentação. O formulário não recebe candidaturas e o painel não é um sistema administrativo privado. Supabase Database, Auth e Storage não estão integrados. Não deve ser lançado para recolha de dados antes da implementação e validação dessas fronteiras.

A Proposta B foi preservada integralmente: nenhum ficheiro de origem do ZIP principal foi alterado. Foram apenas instaladas dependências e gerados resultados locais de auditoria. Não houve deploy, criação de contas, serviços pagos, dados reais ou conteúdo novo atribuído ao Mood.

## 1. Âmbito, fontes e escolha da base

Foram comparados os dois ZIPs. `lovable-project-058f41f0 (2).zip` contém a versão mais limitada, com ligação de configuração ao Lovable. `mood-source (1).zip` contém as páginas institucionais, formulário demonstrativo, painel demonstrativo, módulos tipados, dois ficheiros de testes, fontes locais e `vite.standalone.config.ts`. Esta foi a base executada. Não foi misturado código dos dois exports.

O briefing anexado foi tratado como referência conceptual, não como autorização para executar o backlog. A referência a Next.js nesse documento não substitui a stack TanStack Start pedida na mensagem atual. Não há motivo técnico observado para migrar de framework.

Não existe um checkout Git utilizável com histórico/remoto fornecido nesta workspace; os resultados referem-se aos anexos. As indicações antigas no HANDOFF sobre verificações anteriores não foram tomadas como prova: os testes abaixo foram executados nesta auditoria.

## 2. Verificações executadas

Ambiente: Linux, Node 24.22.1, npm 11.9.0, Bun 1.4.4, Chromium headless. A instalação final foi reconciliada com `bun install --frozen-lockfile`, usando o `bun.lock` fornecido. Uma instalação npm inicial serviu para bootstrap e não é o procedimento recomendado para repetir o projeto. Os comandos abaixo usam executáveis locais instalados.

| Verificação | Resultado |
|---|---|
| Instalação Bun com lockfile congelado | Passou |
| `npm test` | 2 ficheiros, 22 testes passaram |
| `npx --no-install tsc --noEmit` | Passou, sem erros |
| Typecheck explícito de `vite.standalone.config.ts` | Passou; o tsconfig principal não inclui este ficheiro |
| `npm run lint` | 0 erros, 6 avisos de Fast Refresh |
| `npm run build` | Passou; mantém configuração Lovable e gera alvo Cloudflare |
| `npx --no-install vite build --config vite.standalone.config.ts` | Passou; gera alvo SSR Node |
| `node .output/server/index.mjs` após build autónomo | Servidor local funcional |
| Navegação e hidratação Chromium | Sem erros JavaScript capturados |
| Links internos e âncoras das oito páginas principais | Nenhum destino quebrado encontrado |
| Responsividade: 8 páginas × 5 larguras | Um caso de overflow a 320 px |
| axe: home, investigação, candidatura e admin, a 390 e 1440 px | Falhas de contraste em três páginas; admin sem violações automáticas nessas amostras |

Os seis avisos de lint estão em `src/components/ui/{badge,button,form,navigation-menu,sidebar,toggle}.tsx`: exportação de componentes juntamente com constantes/funções. Não impedem o build. O build também avisa que Vite já resolve caminhos de tsconfig nativamente; remover o plugin redundante é manutenção de baixa prioridade.

Os 22 testes cobrem correspondência de rotas, filtros e flags que bloqueiam envio/confirmação. Não exercitam persistência, autorização, submissão real, concorrência ou políticas RLS — essas funcionalidades ainda não existem.

## 3. Rotas

Todas as rotas existentes foram abertas diretamente no servidor de produção autónomo, verificando resposta HTTP e renderização.

| Rotas | Resultado |
|---|---|
| `/`, `/proposta-b`, `/mood` | 200, conteúdo renderizado |
| `/fisiologia`, `/investigacao`, `/atletas` | 200, conteúdo renderizado |
| `/candidatura`, `/candidatura/recebida`, `/contactos`, `/admin` | 200, conteúdo renderizado |
| `/fisiologia/fisiologia-exemplo`, `/fisiologia/recuperacao-exemplo`, `/fisiologia/treino-exemplo` | 200, detalhes de demonstração |
| `/investigacao/artigo-exemplo`, `/investigacao/tese-exemplo`, `/investigacao/preprint-exemplo`, `/investigacao/publicacao-exemplo` | 200, detalhes de demonstração |
| `/nao-existe`, `/fisiologia/invalido`, `/investigacao/invalido` | 404 correto |
| `/admin/login`, `/privacidade`, `/termos` | 404: páginas ainda não implementadas |

São 17 URLs existentes verificadas. As rotas dinâmicas têm todos os sete registos atualmente existentes testados. Os nomes internos com `_` evitam aninhar indevidamente detalhes nas listagens.

As páginas de apoio propostas no briefing não estão todas implementadas: login, páginas legais, histórias individuais de atletas e subáreas administrativas reais faltam. O rodapé apresenta privacidade/termos como texto pendente, não como links funcionais. `/cookies` deve depender das tecnologias efetivamente utilizadas, sem inventar uma política.

## 4. Independência do Lovable

A configuração `vite.standalone.config.ts` importa Vite, TanStack Start, React, Tailwind, tsconfig-paths e Nitro diretamente. A aplicação servida localmente não fez pedidos externos na amostra de navegação capturada, incluindo nenhum pedido ao Lovable. As fontes são empacotadas localmente.

Ainda existem vestígios a remover na próxima fase:

- `vite.config.ts` e os scripts padrão `dev`/`build` usam `@lovable.dev/vite-tanstack-config`.
- O pacote continua em `devDependencies`; o lockfile inclui essa árvore.
- `src/lib/lovable-error-reporting.ts` e `__root.tsx` contêm hooks opcionais para globais do editor. Sem essas globais, não fazem nada; não são necessários para a apresentação.
- Documentação e AGENTS conservam referências ao projeto conectado. O export original inclui `.lovable/project.json`.

Correção proposta: tornar a configuração autónoma a padrão, remover o pacote e hooks Lovable, regenerar o lockfile com Bun e rever documentação/scripts. Confirmar com instalação limpa, build SSR, testes e capturas antes/depois. Não substituir React, TanStack, Tailwind ou os componentes visuais por causa desta remoção.

A demonstração de independência nesta etapa usa a configuração autónoma com as dependências do projeto instaladas; a eliminação física do pacote e nova instalação limpa fazem parte da correção proposta, não de uma mudança já realizada.

## 5. Formulário e administração

### Candidatura

`ApplicationPages.tsx` contém nome, email, telefone opcional, modalidade, equipa opcional e apresentação. Existem labels, autocomplete e validação HTML nativa. Campos vazios e email inválido impediram validação; um exemplo válido apresentou “Nenhuma candidatura foi enviada ou guardada”. O botão de envio está desativado. A página de receção afirma corretamente que a confirmação está indisponível.

Faltam contrato de dados, validação no servidor, limites de tamanho, operação de gravação, tratamento de falhas, proteção contra abuso, idempotência e recibo após commit. A interface não tem estados reais de envio/repetição. O aviso de validação pode permanecer após editar campos; deve ser ligado ao estado efetivo quando o fluxo for implementado.

Os campos definitivos dependem da operação: nível competitivo e localização, por exemplo, foram propostos no briefing mas não estão nesta interface. Não devem ser adicionados automaticamente. Rever a necessidade do telefone. Elegibilidade, menores, responsável pelo tratamento, retenção e texto de privacidade são decisões necessárias antes de ativar recolha. A checkbox atual apenas reconhece uma demonstração; não é um mecanismo final de privacidade ou marketing.

### Administração

`AdminPreview.tsx` é público por desenho. As quatro abas mudam corretamente o título e o conteúdo. Os registos `EXEMPLO-01` etc. estão marcados como exemplos; ações de analisar/editar/gerir estão desativadas e as contagens reais são traços. Não há credenciais embutidas ou acesso a candidaturas reais nesta interface.

Faltam login, recuperação de conta, logout, sessões, MFA, autorização por função, guardas de servidor, consultas privadas, operações de escrita, pesquisa/paginação real, detalhe de candidatura, notas, histórico e módulos editoriais funcionais. Esconder o menu ou proteger só uma rota React não resolve o acesso via API.

## 6. CSS, acessibilidade e responsividade

Foram examinadas capturas a 390 e 1440 px e medido o layout a 320, 390, 768, 1024 e 1440 px nas oito páginas principais. Não foram encontradas imagens quebradas nessas amostras. A 320 px, a home mede 376 px de largura de documento, causada por `.b-media-placeholder` na área do produto. Corrigir a largura mínima/grid do componente e os filhos; não mascarar com `overflow-x:hidden` global.

Axe encontrou contraste insuficiente, reproduzido em mobile e desktop:

| Elemento | Contraste observado | Ação |
|---|---|---|
| `.b-source-pending` no fundo marfim | 3,84:1 | Escurecer texto secundário até ≥4,5:1 |
| `.b-library-caption`, `.b-paper-subject` | 4,49:1 | Dar margem de contraste, evitando valor no limiar |
| Texto secundário de privacidade/termos no rodapé | 4,19:1 | Clarear discretamente no fundo carvão |
| Numerais dos passos da candidatura no bloco verde | 4,16:1 | Clarear o tom de texto |

São ajustes locais de legibilidade dentro da identidade B, sem mudar composição, fotografia, tipografia ou paleta base. O admin não apresentou violações axe nessas amostras; isto não certifica WCAG.

Há pontos positivos: skip link, labels, `aria-expanded`/`aria-controls`, indicação das abas selecionadas, dimensões de imagem, lazy loading abaixo da dobra e regras de redução de movimento. O menu abre, fecha por Escape e navega para `/mood` fechando-se. A pesquisa sem resultados e a limpeza de filtros funcionam.

A revisão manual seguinte deve cobrir foco de inputs/textarea, percurso completo por teclado, zoom 200–400%, leitura da tabela em mobile, anúncios de resultados, estados de erro e leitores de ecrã. A UI de 404/erro ainda está em inglês e usa a aparência genérica; adaptar ao idioma e ao sistema B. Safari/iPhone e Firefox não foram executados.

## 7. SEO, conteúdo e desempenho

### SEO e integridade editorial

Existe SSR com títulos, descriptions e metadados sociais básicos, um H1 por página verificada e `lang="pt"`. Os slugs inválidos devolvem HTTP 404 real.

Faltam canonical, sitemap, `og:url`, imagem de partilha e metadados específicos de artigo/publicação com dados verificados. O helper `pageHead` acrescenta Mood a títulos que já o incluem: a home resulta em “Mood — High Performance — Mood”. `/proposta-b` duplica a home e precisa de política explícita: preservar a referência, acrescentando canonical para `/` e noindex conforme o ambiente, ou futuramente redirecionar após decisão.

`robots.txt` permite tudo. Não há `noindex` no admin, recibo ou exemplos. Enquanto forem protótipos, previews devem ter controlo de acesso e `noindex`; robots não protege dados. Antes da produção, somente conteúdos aprovados devem ser consultáveis e constar do sitemap. Remover exemplos das consultas públicas, em vez de confiar no rótulo “placeholder”. Não gerar schema de organização, autores, resultados ou papers sem informação aprovada. As 404 verificadas ficaram sem título.

As fotografias e os conteúdos estão identificados como ilustrativos/pendentes. Não foi atribuída produção científica real a Thiago nem criados testemunhos. O slogan e “Base científica” existentes também exigem aprovação editorial; a auditoria não valida essas alegações. Não publicar as lacunas como factos.

### Desempenho

O build autónomo produz chunk principal de JS de 325,01 kB (103,33 kB gzip), CSS de 120,82 kB (20,36 kB gzip) e outro CSS de 42,39 kB (19,01 kB gzip). Fotografias: hero 93,14 kB, detalhe 121,91 kB, ciclista 131,02 kB. Há muitos ficheiros de fontes/subsets no output, embora nem todos sejam descarregados numa visita.

Uma navegação local inicial em Chromium a 390×844, sem limitação de CPU/rede, observou 20 recursos e aproximadamente 908 kB transferidos em recursos, LCP 360 ms e CLS 0,0147. Estes números são apenas diagnóstico local; não demonstram Core Web Vitals em dispositivos reais. INP não foi medido. Não foi realizada uma auditoria Lighthouse completa.

Prioridades: medir num preview representativo; reduzir subsets/pesos de fontes mantendo Manrope/Source Sans 3; evitar CSS duplicado ou desnecessário; verificar imports e divisão de componentes/rotas; oferecer imagens responsivas AVIF/WebP preservando recortes. As imagens já têm pesos moderados, dimensões e prioridade no hero. Não é necessário reconstruir o design para otimizar.

Na resposta local observada não havia CSP, HSTS ou outros cabeçalhos de endurecimento. Definir os cabeçalhos no alvo de alojamento, com CSP compatível com os scripts de SSR/hidratação. HSTS depende do domínio HTTPS final; não se deve inferir a configuração de uma futura produção a partir de localhost.

## 8. Backend proposto — ainda não implementado

### Fronteiras técnicas

Manter TanStack Start e criar operações de servidor e uma camada de acesso a dados separada da apresentação. Usar `@supabase/supabase-js` e `@supabase/ssr` com versões fixadas e lockfile. Preparar clientes por pedido no servidor, cookies e renovação de sessão segundo a integração SSR atual; não confiar apenas na sessão apresentada pelo navegador. Não colocar dados privados em loaders públicos, HTML SSR, URLs ou cache partilhada.

Apenas URL e chave publicável podem chegar ao cliente. Chaves secretas/service_role ficam exclusivamente no servidor e nunca em variáveis `VITE_*`. A operação pública de candidatura deve ser estreita e validada; se usar chave privilegiada, essa operação contorna RLS e precisa de validação/limites explícitos, sem aceitar status ou permissões enviados pelo cliente. Não oferecer INSERT público amplo nas tabelas de candidaturas.

Auth da equipa por convite, sem criação pública de contas administrativas. Propor MFA para a equipa, especialmente decisões, publicação e gestão de acessos. A autorização deve consultar pertença ativa/permissões mantidas pelo servidor; não usar `user_metadata` editável. Verificar permissão em cada operação e também na base de dados. Revogação de função deve produzir efeito nas operações sensíveis sem depender de claims antigas.

### Modelo conceptual

| Grupo | Entidades propostas e regras |
|---|---|
| Equipa | `staff_members`, `roles`, `staff_roles`; ligação a `auth.users`, funções e ativação geridas por administrador |
| Candidaturas | `applications`, `application_notes`, `application_events`; dados mínimos, estado, responsável, datas e versão para concorrência |
| Conteúdo | `content_items`, `content_revisions`, `subjects`, `content_subjects`; slug único e revisão publicada separada da revisão em edição |
| Ciência | `publications`, `authors`, `publication_authors`, `evidence_records`; DOI/origem, limitações, revisão e associação comprovada |
| Atletas | `athletes`, `testimonials`, `authorizations`; autorização privada, âmbito, validade e ligação ao material autorizado |
| Institucional | `page_blocks`; blocos predefinidos e versões editoriais |
| Media | `media_assets`; bucket/chave, MIME/tamanho, crédito, alt, direitos e aprovação |
| Auditoria | `audit_events`; autor, ação, alvo e data, sem copiar desnecessariamente dados pessoais |

Definir PKs UUID, FKs, constraints e índices nas FKs e padrões reais de acesso: candidaturas por estado/data/responsável e conteúdos por estado/slug/data. Listagens administrativas paginadas e seleção explícita de colunas. Evitar índices especulativos ou carregar todos os registos no navegador.

### Matriz mínima de acesso

| Ator | Candidaturas/notas | Rascunhos | Publicar | Equipa | Media privada |
|---|---|---|---|---|---|
| Visitante | Sem leitura; envia só pela operação limitada | Sem acesso | Não | Não | Não |
| Gestor de candidaturas | Ver/anotar/decidir dentro do âmbito atribuído | Não | Não | Não | Só documentos necessários à função |
| Editor | Não | Criar/editar | Não | Não | Só media editorial autorizado |
| Revisor/publicador | Não, salvo função acumulada | Rever | Sim, com validações | Não | Material de revisão necessário |
| Administrador | Acesso operacional explicitamente concedido | Gestão | Conforme função | Convites/permissões | Conforme finalidade |

RLS em todas as tabelas expostas, grants mínimos e acesso negado por defeito. `authenticated` não equivale a membro da equipa. Policies devem verificar função e âmbito; UPDATE exige SELECT e `USING`/`WITH CHECK`. Preferir SECURITY INVOKER; views públicas só com campos aprovados e `security_invoker`, ou projeção pública separada sem rascunhos. Testar pelo acesso direto à API, não só pela interface.

### Candidatura e aprovação manual

Fluxo proposto: Recebida → Em análise → Necessita de informação → Selecionada/Não selecionada; Arquivada como destino operacional. A equipa confirma os nomes e transições antes de implementar.

A submissão valida schema/tamanhos no servidor, verifica origem/CSRF onde aplicável, limita frequência, mitiga bots e usa chave de idempotência com unicidade na BD. Não usar só email como chave de idempotência. Persistir candidatura e evento inicial na mesma transação. Mostrar confirmação só após commit; falhas preservam campos e repetição não cria duplicados. Não expor enumeração de emails ou IDs de candidatura. Recibo público deve ser genérico ou protegido por token opaco limitado, sem dados pessoais.

Decisões só podem ser executadas por humano autorizado: registar ator, estado anterior/novo e data na mesma transação, verificar versão para impedir sobreposição de decisões e exigir motivo quando a operação o justificar. Notas são privadas. Mudar o estado não envia email automaticamente no MVP. Logs e observabilidade devem ocultar dados pessoais e tokens.

### Gestão editorial e Storage

Rascunho → Em revisão → Publicado → Arquivado. Alterar um conteúdo publicado cria revisão pendente e preserva a versão pública anterior. Publicar exige função apropriada, fontes/autorizações válidas e revisão científica quando aplicável. Sanitizar conteúdo rico e validar URLs, não aceitar HTML arbitrário. Atualizar cache e sitemap após publicação/arquivo.

Separar media aprovada pública de rascunhos, comprovativos e documentos privados. Buckets privados sujeitos a RLS; downloads privados com acesso autenticado ou URLs assinadas de curta duração. Um bucket público permite leitura a quem tiver o URL: nunca colocar ali documentos de candidatura ou autorizações. Restringir tamanho/MIME, verificar ficheiros no servidor e tratar SVG/HTML e ficheiros ativos de forma segura. Não permitir uploads anónimos no MVP sem necessidade definida.

A retirada/expiração de autorização deve impedir novas publicações e desencadear a retirada do material afetado, incluindo cache/objetos conforme a operação. Registar direitos e revisões sem expor comprovativos ao público.

## 9. Correções prioritárias e plano por fases

| Fase | Trabalho | Critério de conclusão |
|---|---|---|
| 1 — Base autónoma | Promover config standalone, remover pacote/hooks Lovable, uniformizar scripts, fixar Node/Bun, lockfile único e CI de typecheck/lint/test/build | Instalação limpa sem pacote Lovable; SSR e rotas passam; capturas B mantidas |
| 2 — Correções de interface | Overflow 320 px, contrastes, 404/erro em português e B, foco/teclado, noindex de preview e títulos | Sem scroll involuntário; contrastes AA; comparação visual aprovada sem redesenho |
| 3 — Fundação backend | Validar campos/menores/retenção, modelar migrações, Auth por convite, matriz de permissões, RLS e buckets, ambiente separado de teste | Visitante/utilizador sem função não acede a dados; testes por perfil/API/Storage passam |
| 4 — Candidaturas | Endpoint seguro, validação, limites, idempotência, persistência/recibo; painel real com notas e decisão manual transacional | Envio e repetição controlada; falhas preservam dados; apenas gestores autorizados decidem |
| 5 — Editorial e media | Revisões, publicação/arquivo, ciência/evidências, autorizações e media | Público só vê versão aprovada; editor não vê candidatos; alterações não substituem versão publicada |
| 6 — Homologação | Conteúdo real aprovado, páginas legais/contactos, sitemap/canonical/OG, desempenho, testes cross-browser, recuperação e guia operacional | Conteúdo e operação validados; plano de rollback e restauro ensaiado; aprovação separada para produção |

**Bloqueios antes de dados reais:** autenticação/autorização, RLS, Storage privado, submissão segura e decisões de privacidade. **Correções imediatas de qualidade:** overflow, contraste e indexação de exemplos. **Manutenção posterior:** avisos Fast Refresh, simplificação de plugins e redução de assets conforme medições.

Não é necessário esperar por todos os materiais institucionais para consolidar a base e implementar com dados sintéticos isolados. Ativar recolha ou publicar exige critérios, responsáveis e conteúdo aprovados. Configuração de serviços e eventual custo serão apresentados concretamente antes de qualquer contratação; esta auditoria não criou recursos.

## 10. Testes exigidos nas próximas fases

- Visitante, autenticado sem função, editor, gestor, publicador e administrador: tentativas de leitura/escrita por interface e API, incluindo alteração de IDs.
- Rascunhos, notas, autorizações e ficheiros privados inacessíveis sem permissão; URLs assinadas expiram; utilizador desativado perde operações sensíveis.
- Candidatura válida/inválida, limites, falha de rede/BD, retry idempotente, submissões concorrentes, confirmação só após gravação e nenhum estado selecionado pelo cliente.
- Decisões concorrentes e histórico atómico; editor não pode promover a própria permissão; atualização de revisão não altera versão pública antes de aprovação.
- XSS/conteúdo rico, URLs perigosos, tipo/tamanho de upload e revogação de autorização.
- Teclado, leitor de ecrã, zoom, redução de movimento, Safari/iPhone e Firefox; regressão visual B e métricas laboratoriais representativas.

## 11. Evidências e limites

A pasta `evidencias` contém logs de instalação/checks/builds, resultados JSON de navegador, scripts reproduzíveis e capturas. `browser-results.json` regista HTTP, H1/títulos, largura, imagens, axe e interações. `extra-results.json` regista links, validação inválida, pedidos externos e medições locais.

Não foram auditadas políticas de um projeto Supabase existente: não foi fornecido nem conectado um backend desta aplicação. Não houve teste de infraestrutura produtiva, auditoria completa de dependências/CVEs, carga, certificação WCAG ou medição de Core Web Vitals em campo. As conclusões de funcionamento aplicam-se à versão principal dos anexos e ao alvo Node local testado.

Documentação Supabase consultada nesta sessão: [segurança dos produtos](https://supabase.com/docs/guides/security/product-security), [buckets e acesso](https://supabase.com/docs/guides/storage/buckets/fundamentals), [changelog](https://supabase.com/changelog), incluindo a entrada sobre [Postgres 15.19/17.11](https://supabase.com/changelog/postgres-15-19-17-11-breaking-changes). Confirmar as APIs de SSR, versões e permissões na execução da integração; nenhuma migração foi aplicada nesta etapa.
