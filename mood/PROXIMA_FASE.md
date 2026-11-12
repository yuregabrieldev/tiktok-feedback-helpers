# Próxima fase — Supabase, autenticação e permissões

Preparação documental; nenhuma integração ou infraestrutura foi criada nesta etapa.

1. Confirmar campos mínimos da candidatura, elegibilidade/menores, entidade responsável, retenção, contactos e permissões da equipa. Confirmar região, titularidade e custos antes de configurar serviços.
2. Modelar migrações versionadas para membros/funções, candidaturas/notas/eventos, conteúdos/revisões, publicações/fontes, media e autorizações. Preservar separação entre revisão em edição e versão pública aprovada.
3. Integrar Auth SSR por convite, sessões verificadas no servidor, MFA e autorização em cada operação. Perfis: administrador, gestor de candidaturas, editor e revisor/publicador. Um editor não deve ler candidaturas; estar autenticado não concede acesso à equipa.
4. Aplicar grants mínimos e RLS nas tabelas expostas. Nunca enviar chaves privilegiadas ao browser; não basear permissões em metadata editável pelo utilizador. Buckets privados para rascunhos e comprovativos, media pública apenas aprovada.
5. Implementar submissão limitada no servidor com validação, antiabuso, idempotência, gravação transacional e confirmação apenas após commit. Seleção exige ação humana identificada e histórico; lidar com decisões concorrentes.
6. Testar por interface e API: visitante, autenticado sem função, gestor, editor, publicador e administrador. Incluir IDOR, acesso a rascunhos e ficheiros privados, revogação de função, retries e falhas. Confirmar que nenhum dado privado entra no SSR/cache público/logs.

Não ativar o formulário com dados reais até a fronteira de acesso e os textos aplicáveis estarem validados. Homologação e eventual produção são passos separados. A auditoria original detalha o modelo conceptual e os critérios de aceitação.
