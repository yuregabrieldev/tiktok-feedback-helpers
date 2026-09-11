# PRD — PULSO

**Versão:** MVP 0.1  
**Estado:** pronto para implementação  
**Plataforma:** web mobile-first, distribuída por link (WhatsApp)

## 1. Resumo

PULSO é uma comunidade de descoberta entre criadores de TikTok. Um utilizador conhece o perfil de outro criador no TikTok, retorna ao PULSO e envia feedback útil. Cada feedback válido concede pontos; os pontos permitem lançar a própria campanha e receber feedbacks de outras pessoas.

O produto não recompensa, exige, afirma verificar ou contabiliza follows, likes, comentários ou visualizações no TikTok. Na linguagem interna deste produto, uma **avaliação** significa: abrir o perfil TikTok indicado e retornar para enviar feedback no PULSO.

## 2. Objetivo do MVP

Criar um ciclo simples e seguro:

```text
avaliar criador → ganhar ponto → publicar campanha → receber avaliações
```

O primeiro acesso tem uma missão-tutorial sem recompensa. Em seguida, a pessoa encontra uma campanha normal na For You, ganha o primeiro ponto com uma avaliação válida e usa esse ponto para publicar o próprio perfil.

## 3. Princípios de produto

- Mobile-first: o uso esperado é pelo navegador aberto a partir do WhatsApp.
- Uma ação principal por tela; nunca depender de hover.
- O backend decide saldos, elegibilidade, missões e recompensas; o frontend apenas apresenta estados.
- Feedback útil vale mais do que cliques. Abrir um link nunca gera ponto por si só.
- Perfis TikTok são adicionados manualmente; não há Login com TikTok no MVP.
- O produto promove descoberta e feedback, não crescimento artificial de métricas do TikTok.

## 4. Papéis

### Utilizador

Cria perfil, completa o tutorial, avalia campanhas, acumula/gasta pontos, publica campanhas e recebe feedbacks.

### Administrador

Modera conteúdo e feedbacks. Configura a conta do tutorial, campanhas-semente e destaques formados apenas por contas que administra.

## 5. Fluxos essenciais

### 5.1 Cadastro e criação de perfil

1. Utilizador entra com e-mail e código de acesso/magic link.
2. Cria o perfil: foto, nome de exibição, `@username`, link de perfil TikTok e nicho.
3. O link precisa ser um URL válido de perfil TikTok; URLs arbitrárias são rejeitadas.
4. A conta começa com saldo `0` e estado `tutorial pendente`.

Não utilizar senha no MVP. E-mail com código reduz atrito no celular e a superfície de risco de senhas.

### 5.2 Tutorial obrigatório — conta admin #1

Antes de acessar a For You, publicar ou usar pontos, o utilizador completa uma única missão-tutorial.

```text
Criar perfil
→ missão-tutorial obrigatória
→ abrir perfil TikTok do admin
→ retornar e enviar feedback válido
→ conta ativada; saldo continua 0
→ For You
```

Características:

- O tutorial não concede pontos.
- É apresentado uma única vez por utilizador.
- O botão único é `Conhecer e avaliar`.
- O perfil TikTok abre em nova aba/página. Ao retornar ao PULSO, surge a avaliação daquela missão.
- Existe uma janela de elegibilidade discreta no backend antes de aceitar o feedback. O produto não afirma que verifica follow, visualização ou interação no TikTok.
- Após um feedback válido, `tutorial_completed_at` é preenchido e o utilizador é redirecionado para a For You.

### 5.3 For You normal e primeiro ponto

A For You é um feed infinito de **campanhas**, não uma lista crua de perfis. Cada campanha pertence a um criador e contém uma pergunta concreta.

Uma campanha-semente do admin (#2) pode entrar como campanha normal na For You dos utilizadores recém-ativados. Ela oferece a mesma recompensa de qualquer campanha normal: `+1 ponto` após feedback válido.

```text
For You
→ conhecer e avaliar uma campanha normal
→ +1 ponto
→ publicar própria campanha por 1 ponto
```

### 5.4 Avaliar uma campanha

1. No card, o utilizador toca `Conhecer e avaliar`.
2. O backend verifica elegibilidade e cria uma missão temporária, exclusiva e com validade definida.
3. O PULSO abre o link TikTok de perfil em nova aba/página.
4. Ao retorno, a missão pendente mostra a folha de avaliação.
5. O utilizador responde uma pergunta estruturada e deixa uma sugestão curta.
6. O backend valida a submissão. Se aceita, registra o feedback, credita a recompensa, atualiza a campanha e remove aquele card da For You daquele utilizador.

Abrir/fechar o TikTok sem concluir a avaliação não concede saldo. Se a missão expirar, não há recompensa.

### 5.5 Gastar pontos e publicar uma campanha

1. Na área `Publicar`, o utilizador escolhe o próprio perfil e escreve o pedido de feedback.
2. Seleciona a quantidade de feedbacks desejada: `1`, `3` ou `5`.
3. Cada feedback custa `1 ponto`.
4. O utilizador vê a prévia do card e confirma `Lançar campanha`.
5. O backend debita o valor para uma reserva da campanha e cria a campanha ativa.
6. Cada feedback válido transfere `1 ponto` da reserva ao avaliador.
7. Ao cumprir a quantidade comprada, a campanha encerra automaticamente.

Exemplo: uma campanha de 3 feedbacks reserva 3 pontos e entrega 1 ponto a cada um dos três avaliadores aprovados. Pontos não distribuídos devem voltar ao saldo quando a campanha expirar.

No MVP, um utilizador pode ter no máximo uma campanha normal ativa.

### 5.6 Destaques do administrador

Além da campanha-tutorial e de campanhas-semente normais, o admin pode lançar uma **missão em destaque** usando uma de suas contas.

- O card fica fixado no topo da For You.
- Recompensa: `+2 pontos` por feedback válido.
- O utilizador recebe a recompensa uma vez por versão de destaque.
- Ao publicar uma nova versão com outro perfil TikTok, ela aparece novamente para pessoas que ainda não a concluíram.
- Alterar somente texto, imagem ou pergunta não cria uma nova recompensa; o admin precisa publicar um novo destaque.
- Destaques possuem orçamento e podem ser pausados pelo admin.

Pontos de tutorial não existem. Pontos de campanhas-semente e destaques são créditos concedidos pelo orçamento da plataforma/admin.

## 6. Estrutura da For You

### Card de campanha

```text
PULSO #041 · RECEITAS

[foto] @marianaem15
       Receitas práticas para quem não tem tempo

PEDIDO DA VEZ
“Minha bio explica claramente o que eu posto?”

[ Conhecer e avaliar ]
```

O card representa uma campanha com um perfil real: foto enviada, `@username`, nicho, descrição curta e pergunta. Não há botão separado de “ver perfil”; o único CTA abre o TikTok e inicia a missão.

### Regras de seleção

O servidor retorna campanhas que:

- não pertencem ao próprio utilizador;
- não foram concluídas por ele;
- não estão bloqueadas/denunciadas;
- ainda possuem reserva e vagas de feedback;
- respeitam idioma, nicho e diversidade quando esses dados estiverem disponíveis.

Uma campanha destacada ativa ocupa o primeiro slot. Campanhas já concluídas não voltam ao feed daquele utilizador. A listagem usa paginação por cursor; o cliente não decide ranking nem elegibilidade.

## 7. Perfil

### Público

- Foto de perfil processada;
- nome e `@username`;
- link para TikTok;
- nicho e biografia curta;
- número de feedbacks recebidos;
- número de campanhas concluídas;
- campanha ativa, se existir.

### Privado — Minha conta

- saldo atual;
- histórico de entradas, reservas, gastos, estornos e recompensas;
- avaliações enviadas e recebidas;
- estado do tutorial;
- edição do perfil e da foto;
- botão para denunciar/bloquear utilizadores.

E-mail, IP, saldo detalhado e dados antiabuso nunca são públicos.

## 8. Sistema de pontos

| Evento | Saldo |
|---|---:|
| Completar tutorial | 0 |
| Feedback válido de campanha normal | +1 |
| Feedback válido de destaque admin | +2 |
| Lançar campanha com N feedbacks | −N, em reserva |
| Feedback válido recebido pela campanha | +1 ao avaliador, saindo da reserva |
| Expiração sem feedback distribuído | estorno do saldo reservado |

Todo evento é escrito em um livro-caixa imutável (`point_ledger`). O saldo exibido é derivado ou atualizado apenas por função transacional de servidor.

## 9. Fotos e links de perfil

### Upload de foto

Aceitar somente `JPG/JPEG`, `PNG`, `WebP` e `HEIC/HEIF` (fotos de iPhone). Rejeitar GIF, SVG, vídeo, PDF, ZIP e formatos desconhecidos.

Pipeline obrigatório:

```text
upload autenticado
→ bucket privado de quarentena
→ validação por assinatura real do arquivo, tamanho e dimensões
→ decodificação segura e moderação de imagem
→ remoção de EXIF
→ conversão para imagem normalizada (WebP/JPEG)
→ bucket público apenas para a versão processada
```

O original nunca é público. Limites iniciais: até 8 MB, mínimo 160 × 160 e máximo 4.096 × 4.096 pixels. Fotos reprovadas por moderação não aparecem publicamente.

### Link TikTok

O backend aceita apenas URLs HTTPS de perfil TikTok, normaliza o endereço e nunca busca URLs arbitrárias fornecidas pelo utilizador. O site não coleta senha TikTok nem executa scraping de perfil.

## 10. Segurança e prevenção de fraude

- Supabase Auth por e-mail; confirmação antes de publicar ou ganhar saldo.
- Row Level Security em todas as tabelas expostas.
- Chave `service_role` somente no servidor/Edge Function; jamais no navegador.
- Funções transacionais de servidor para: iniciar missão, enviar feedback, creditar/reverter pontos, criar campanhas e lançar destaques.
- Chaves de idempotência e restrições únicas para impedir crédito duplicado.
- Limites por conta, IP e dispositivo para cadastro, upload, início de missão e feedback.
- CAPTCHA/Turnstile em cadastro, publicação, upload e comportamento suspeito.
- Filtro de texto contra repetição, spam e feedback vazio; tamanho mínimo e campos estruturados.
- Uma avaliação por utilizador e campanha. Bloqueio de autoavaliação.
- Registro de auditoria para saldo, moderação, alterações administrativas e denúncias.
- Rate limit, cabeçalhos de segurança, CSRF para ações sensíveis e validação de payload no servidor.
- Painel admin para remover campanhas/fotos, bloquear contas, congelar saldo e analisar denúncias.

O tempo de permanência entre abrir e retornar é apenas um sinal antiabuso; não prova que o utilizador viu conteúdo, seguiu ou interagiu no TikTok.

## 11. Telas do MVP

1. Entrada por e-mail/código.
2. Criar/editar perfil.
3. Missão-tutorial bloqueadora.
4. For You infinita.
5. Folha de retorno e avaliação.
6. Publicar campanha e prévia.
7. Perfil público.
8. Minha conta / carteira de pontos.
9. Histórico de feedbacks.
10. Painel admin: tutorial, campanhas-semente, destaques, orçamento, moderação e denúncias.

## 12. Direção visual

O produto herda a linguagem visual do projeto **King Of The Internet**, sem copiar o tema de realeza:

- fundo de papel claro, tinta quase preta e linhas técnicas;
- tipografia display forte para nomes, campanhas e estados importantes;
- tipografia mono para etiquetas, pontos, estados e dados operacionais;
- uma cor intensa de acento, usada para atividade e poder de ação;
- vocabulário de transmissão/sinal/sistema ao vivo;
- cards de campanha com foco em uma pessoa e uma pergunta;
- navegação inferior com `For You`, `Publicar` e `Minha conta`;
- controles com pelo menos 44 × 44 px, suporte a área segura de iPhone e `prefers-reduced-motion`.

Evitar: clone visual do TikTok, feed de vídeo reproduzindo automaticamente, cards SaaS genéricos, paleta ciano/rosa do TikTok e excesso de elementos decorativos.

## 13. Métricas iniciais

- Conclusão do tutorial;
- percentagem de utilizadores que realiza a primeira avaliação normal;
- tempo até o primeiro ponto;
- percentagem que publica a primeira campanha;
- taxa de feedback concluído por campanha;
- taxa de denúncia/reprovação de foto ou feedback;
- saldo médio e taxa de estorno de campanhas expiradas.

## 14. Fora do escopo do MVP

- Login, coleta de senha ou scraping do TikTok;
- verificação de follow, likes, comentários ou visualizações;
- recompensa por follow, like, comentário ou clique;
- reprodução/embedding automático de vídeos TikTok;
- uploads de vídeo, GIF, SVG ou documentos;
- pagamentos em dinheiro;
- múltiplas campanhas normais simultâneas por utilizador;
- recomendação avançada baseada em machine learning.

## 15. Critérios de aceite

- Um utilizador não ativado não acessa For You nem Publicar; ao concluir o tutorial, passa a acessar ambos com saldo 0.
- Uma campanha normal aprovada concede exatamente 1 ponto, uma única vez, ao avaliador elegível.
- Uma campanha de N feedbacks bloqueia N pontos e não distribui mais de N recompensas.
- O mesmo utilizador nunca recebe duas recompensas pela mesma campanha ou versão de destaque.
- Uma nova versão de destaque criada pelo admin aparece no topo para utilizadores que ainda não a concluíram e concede 2 pontos no máximo uma vez.
- Cliques ou requisições repetidas não alteram saldo sem feedback válido.
- Arquivos fora dos formatos aceitos, com assinatura inválida, tamanho/dimensões excessivos ou reprovação de moderação não ficam públicos.
- Nenhuma rota do navegador possui permissão direta para editar saldo, aprovar feedback ou configurar campanhas administrativas.
