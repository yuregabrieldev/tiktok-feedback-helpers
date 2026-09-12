# Segurança e moderação de imagens

## Dependências

O projeto usa um override de `postcss` (`8.5.28`) e `sharp` (`0.35.4`).
Verifique com:

```bash
npm audit --omit=dev --audit-level=high
```

## Uploads

`/api/profile/avatar` autentica o utilizador, valida o tipo e dimensões da imagem,
descarta EXIF, converte para WebP e guarda no bucket privado `avatars-clean`.
Mídias de campanhas só podem ser alteradas por administradores.

## Moderação de conteúdo

O endpoint aceita um classificador externo através de `IMAGE_MODERATION_ENDPOINT`.
Ele deve receber JSON `{ imageBase64, mimeType }` e responder `{ "allowed": true }`
ou `{ "allowed": false }`. Configure a URL e as credenciais do provedor apenas na
Vercel (nunca no código ou em variáveis `NEXT_PUBLIC_*`). Sem esse endpoint, a
validação técnica continua ativa, mas não existe detecção automática de nudez.
