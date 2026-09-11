# Segurança operacional — PULSO

## Segredos

- `NEXT_PUBLIC_SUPABASE_URL` e `NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY` podem ser usados no navegador.
- `SUPABASE_SERVICE_ROLE_KEY` é exclusivamente de servidor e nunca entra no Git, cliente ou logs.
- Como a chave de serviço foi partilhada durante a configuração inicial, ela deve ser rotacionada no painel Supabase antes de produção.

## Imagens

O endpoint de avatar aceita JPEG, PNG, WebP e HEIC/HEIF, valida o arquivo por decodificação, remove EXIF e recria uma imagem WebP. O arquivo original nunca é público.

Antes de disponibilizar uploads ao público, configure `IMAGE_MODERATION_ENDPOINT` com um serviço de moderação de imagem. Ele deve receber a imagem processada e responder JSON no formato:

```json
{ "allowed": true }
```

ou

```json
{ "allowed": false }
```

Sem uma moderação configurada, mantenha uploads de perfil desativados em produção. A validação de formato não detecta nudez ou material gráfico.
