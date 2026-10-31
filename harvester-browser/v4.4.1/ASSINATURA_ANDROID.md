# Assinatura permanente do VistaBrowser (Android, grátis)

## Porquê
O Android só permite atualizar uma instalação se o APK novo tiver o mesmo package ID
e certificado digital do APK já instalado. A versão VistaBrowser 4.4.0 foi
compilada com **debug signing temporário**, pelo que o certificado não pode
ser reutilizado de forma fiável. A migração para chave permanente pode exigir
um backup e uma única reinstalação. Não remova a aplicação antes de verificar
que o backup da biblioteca, favoritos, histórico e fontes foi guardado.

A partir da v4.4.1, a CI não publica nenhum APK sem os quatro segredos.
O APK é verificado com apksigner e o hash do certificado deve coincidir com
a chave fornecida antes da publicação. A release não é marcada como prerelease
para poder ser detetada pelo atualizador interno do VistaBrowser.

## Passos diretamente no Android, sem PC

1. Instale Termux de uma fonte legítima, de preferência F-Droid ou GitHub
   oficial de Termux; evite misturar assinaturas de fontes diferentes.
2. No Termux:

```sh
pkg update
pkg install openjdk-17 coreutils gh
mkdir -p ~/vistabrowser-signing
cd ~/vistabrowser-signing
keytool -genkeypair -keystore vistabrowser-release.jks -storetype PKCS12 \
  -alias vistabrowser -keyalg RSA -keysize 3072 -validity 10950
```

Escolha uma password longa e única; o keytool pedirá informações do
certificado. Não use o certificado do VistaStream. Para PKCS12 a password
da chave privada é normalmente igual à do keystore.

3. Autorize GitHub CLI localmente:

```sh
gh auth login --web --scopes repo,workflow
gh auth status
```

Se o CLI solicitar browser/device code, autorize a conta que administra
`yuregabrieldev/tiktok-feedback-helpers`. NÃO envie token para o ChatGPT.

4. Grave os segredos encriptados no repositório sem copiar o ficheiro para
   uma conversa ou commit:

```sh
gh secret set VB_KEYSTORE_BASE64 -R yuregabrieldev/tiktok-feedback-helpers \
  --body "$(base64 -w 0 ~/vistabrowser-signing/vistabrowser-release.jks)"
gh secret set VB_KEY_ALIAS -R yuregabrieldev/tiktok-feedback-helpers \
  --body "vistabrowser"

read -rsp "Password da assinatura: " VB_PASSWORD
printf '\n'
printf '%s' "$VB_PASSWORD" | gh secret set VB_KEYSTORE_PASSWORD \
  -R yuregabrieldev/tiktok-feedback-helpers
printf '%s' "$VB_PASSWORD" | gh secret set VB_KEY_PASSWORD \
  -R yuregabrieldev/tiktok-feedback-helpers
unset VB_PASSWORD
gh secret list -R yuregabrieldev/tiktok-feedback-helpers
```

Os nomes dos segredos podem ser listados, mas os seus valores nunca devem
aparecer em logs, prints, issues, pull requests, chat ou commits.

5. Faça pelo menos uma cópia segura do ficheiro .jks numa localização
   privada (preferencialmente cifrada). A perda desta chave pode impedir
   futuras atualizações. O ficheiro .jks nunca deve ser publicado no GitHub
   nem partilhado com terceiros.

6. Execute o workflow `vistabrowser-v441-release.yml` na branch
   `feature/vistabrowser-v441-permanent-signing`. A compilação produzirá
   a release normal `vistabrowser-v4.4.1`, que o próprio VistaBrowser
   detetará nas pesquisas de atualização.

## Verificação da assinatura
A CI testa:
- que os quatro segredos estão presentes;
- que o keystore é válido e o alias existe;
- que o APK tem um certificado verificável;
- que SHA-256 desse certificado corresponde ao do keystore;
- que o versionCode é superior ao da versão anterior.

## Limite do primeiro update
Um certificado **novo** não é o certificado usado pelo APK debug da versão
4.4.0; o Android recusará uma instalação por cima. Faça backup, verifique
que é recuperável, e siga uma reinstalação única, se necessário. As versões
seguintes 4.4.2, 4.4.3, ... usarão a mesma chave e poderão atualizar
diretamente, com a confirmação do instalador Android.
