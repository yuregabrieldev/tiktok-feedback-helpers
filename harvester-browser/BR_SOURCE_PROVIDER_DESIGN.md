# Harvester Browser ↔ Vista Stream — Sistema de provedores BR

**Estado:** proposta de integração validada contra a estrutura atual dos dois aplicativos. Este documento NÃO afirma que os provedores de terceiros estejam operacionais ou que exista um novo APK.

## Decisão de arquitetura
- Permanecem DOIS aplicativos independentes.
- O Harvester Browser é o navegador, detetor passivo, inspetor e administrador de fontes. **Reproduzir ou detetar nunca adiciona automaticamente à biblioteca.**
- O Vista Stream é o catálogo, associador de filmes/séries/animes/episódios, guardião das fontes escolhidas e player Media3 + Chromecast.
- Uma camada **ProviderBridge** converte resultados de extensões para um formato próprio; não tenta instalar e executar APKs do CloudStream nem do Aniyomi dentro dos nossos APKs.
- A existência de pasta em um repositório não atesta funcionamento atual de uma fonte.
- Nenhum módulo ignora DRM, paywalls, proteções de acesso, bloqueios anti-automação ou obtém vídeo sem autorização.

## Código upstream analisado (2026-10-24)
- `lawlietbr/lietrepo` (declara MIT no README; não havia ficheiro LICENSE na árvore principal consultada): diretórios AnimeFire, PobreFlix e outros. Classes CloudStream `MainAPI`, `search()`, `load()`, `loadLinks()`; requerem o runtime CloudStream.
- `yuzono/anime-extensions` (Apache-2.0): `src/pt/animefire` contém modelos de episódio, seleção de áudio `dublado`/`legendado` e fonte HTTP própria do Aniyomi. Também requer adaptação do runtime.
- `GuickerZ/guindex` (MIT): normalização de nomes brasileiros, ano/temporada/episódio e ordenação; é um serviço TypeScript de indexação/debrid, **não** um extrator de WebView Android e não deve ser iniciado como crawler automático no dispositivo.
- `saimuelbr/saimuelrepo`: o checkout público analisado inclui um índice de builds, mas não os fontes Kotlin dos provedores correspondentes. Não tratar os APKs binários como código-fonte disponível.
- `movie-web/providers`: estrutura distinta de providers/extractors; referência conceptual somente.

## Contrato próprio mínimo
```kotlin
data class ProviderDescriptor(
  val id: String, val title: String, val version: String,
  val languages: Set<String>, val supportedDomains: Set<String>,
  val mediaTypes: Set<String>, val capabilities: Set<String>
)
data class ProviderMedia(
  val sourceId: String, val pageUrl: String, val title: String,
  val originalTitle: String?, val year: Int?, val type: String,
  val season: Int?, val episode: Int?, val poster: String?,
  val imdbId: String?, val anilistId: Int?
)
data class PlaybackSource(
  val mediaId: String, val uri: String, val mimeType: String?,
  val audioLanguage: String?, val dubbed: Boolean?,
  val quality: Int?, val subtitleTracks: List<SubtitleTrack>,
  val headers: Map<String, String>, val expiresAtMs: Long?
)
interface HarvesterProvider {
  val descriptor: ProviderDescriptor
  suspend fun search(query: String): List<ProviderMedia>
  suspend fun details(id: String): ProviderMedia
  suspend fun episodes(id: String): List<ProviderMedia>
  suspend fun resolve(mediaId: String): List<PlaybackSource>
}
```
**Nota:** isto é o formato proposto, não uma API Android já implementada.

## Motor de execução
1. Um `ProviderRegistry` regista adaptadores **auditados** e desativados por padrão. Na primeira fase, empacotados como código compilado. Fase posterior: manifestos JSON assinados e aprovados pelo utilizador, sem executar Kotlin/Java arbitrário descarregado.
2. Só permite domínios declarados, HTTPS, redirects validados e pedidos feitos pelo `ProviderHttpClient`; rejeita localhost/redes privadas por padrão.
3. Pesquisa apenas após ação expressa. Cache (títulos e detalhes), deduplicação, máximo 1 pedido em voo por host e espera mínima entre pedidos. Degrada/para em 429, 503, 522 e 524; **nunca** faz retries em massa. Sem rastreio periódico de páginas de terceiros.
4. Capacidade `search/details/episodes` independente de `resolve`; metadados podem continuar disponíveis mesmo que reprodução seja restrita.
5. As legendas e variantes são classificadas separadamente: preferir PT-BR dublado, depois PT-BR legendado, depois outros idiomas segundo preferências do utilizador. Não inferir que uma fonte tem dublagem apenas por estar em português.
6. Guardar origem, versão do adaptador, instante e possível expiração da URL; não usar URLs de sessão expirada permanentemente.

## Integração do botão + existente
`Página Web/Provider -> Harvester.MediaItem -> Intent ACTION_IMPORT -> Vista.ImportEditor -> catalog match -> escolha do utilizador -> guardado explícito`

- O Harvester envia título limpo, idioma, episódio/temporada, fonte, referer e legendas quando conhecidos.
- O Vista pesquisa o seu catálogo **Cinemeta/AniList** como já faz (não usar TMDB sem chave válida); classifica variantes PT-BR/EN, ano e episódios.
- Escolhida a correspondência, o Vista guarda **ID de catálogo** (IMDb/AniList/TMDB), temporada, episódio, proveniência e a fonte normalizada.
- No ecrã de um filme/série/episódio, `StreamingRepositoryImpl.resolveServers()` agrega as fontes do catálogo + fontes Browser **confirmadas**. Fontes duplicadas são colapsadas por URL canónica/episódio; prioridade de dublagem/qualidade é uma preferência, não garantia.
- O retorno `RESULT_OK` ao Harvester apenas depois de a gravação no Vista ter sido concluída. Cancelar não grava nenhum vídeo em nenhuma biblioteca.
- O Chromecast existente do Vista é o único controlador Cast, e reproduz o `VideoServer` escolhido quando o endereço é acessível ao receptor.

## Interface futura
- **Harvester:** painel “Fontes” com módulos instalados, ativos, domínio, idioma, ações pesquisar/desativar/remover/atualizar; vídeos encontrados têm botão + explícito.
- **Vista Stream:** “Fontes disponíveis” incluindo “Browser”, filtro por PT-BR/dublado/legendado, biblioteca “Guardados do Browser”, edição e remoção das fontes com confirmação.
- **Diagnóstico:** contar pedidos extra do provider separadamente dos pedidos naturais do WebView; registar erros sem capturar tokens ou cookies privados.

## Sequência de desenvolvimento
**Fase A (contratos e testes):** DTOs/registry/normalização de temporadas e episódios/ordenador PT-BR, testes unitários e adaptador de teste offline.

**Fase B (primeiro provider autorizado):** fonte pública/licenciada de teste ou servidor controlado; pesquisa, metadados e episódios, depois vídeo acessível por Media3. Testar no telemóvel e num receptor Chromecast real.

**Fase C (adaptadores BR):** auditar código e licença módulo a módulo; portar apenas padrões que sejam legais e tecnicamente compatíveis. A utilização de um repositório não implica que todos os domínios dele possam ou devam ser consultados.

**Fase D (gestão de módulos):** configuração individual de fontes, atualizações autenticadas, retries conservadores, importação seletiva e métricas de falha.

## Critérios de aceitação
- Nenhum vídeo assistido entra na biblioteca sem + e confirmação.
- Cada fonte guardada pertence exatamente ao filme ou episódio selecionado.
- Idioma/qualidade/legendas aparecem como atributos verificáveis.
- Desligar um provider para imediatamente novas consultas; erro 522 interrompe sem escalar tráfego.
- Browser e Vista continuam instaláveis independentemente; releases com APKs reais e testes verificáveis.
