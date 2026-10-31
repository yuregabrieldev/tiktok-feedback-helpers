# VistaBrowser — assinatura estável de APK (gratuita)

O Android exige que o APK atualizado tenha **exatamente o mesmo certificado**,
além do mesmo applicationId, para preservar os dados. GitHub Actions gera uma
debug.keystore temporária em cada runner: usar assembleDebug sem uma chave
persistente não garante atualização entre builds.

## Criar a chave uma vez, em dispositivo controlado pelo proprietário

**Recomendado:** gerar no computador com Java 17+ (ou Termux com OpenJDK no
telemóvel). Nunca colar senhas ou a chave em issues, commits, conversas,
artifacts públicos ou ficheiros do repositório.

```bash
keytool -genkeypair -v -keystore vistabrowser-release.jks -storetype PKCS12 -alias vistabrowser -keyalg RSA -keysize 3072 -validity 10950
```

Guardar `vistabrowser-release.jks` offline (2 cópias seguras). Criar quatro
**Repository → Settings → Secrets and variables → Actions → New repository secret**:

- `VB_KEYSTORE_BASE64`: conteúdo do ficheiro em base64 numa única linha,
  por exemplo `base64 -w 0 vistabrowser-release.jks` em Linux/Termux
- `VB_KEYSTORE_PASSWORD`: password do keystore
- `VB_KEY_ALIAS`: `vistabrowser`
- `VB_KEY_PASSWORD`: password da chave privada (quando igual à do store, usar o mesmo valor)

A build 4.3.8 deteta os segredos e configura a assinatura de Debug com essa
chave (continua a distribuir o mesmo applicationId e APK). Os segredos não
estão no código e nenhum APK pode recuperar a chave privada.

**IMPORTANTE:** se a versão instalada tem certificado debug diferente, a
primeira versão assinada com a nova chave NÃO poderá atualizar por cima.
Exporta os dados do browser, verifica o backup e só então reinstala uma
única vez. Daí em diante, conserva sempre a mesma chave. Se a chave antiga
estiver disponível, prefere reutilizá-la para evitar a reinstalação.

## Verificação

No workflow consulta a assinatura e confirma o SHA-256 do certificado.
Confirma o mesmo certificado entre duas builds sequenciais antes de anunciar
"atualização sem perda de dados". Atualizações só podem ser instaladas
pela UI do instalador Android, quando exigido pelo sistema.

## Assistente de reprodução (experimental)

Não existe um motor universal que abra qualquer vídeo pela força. O
VistaPlayerAssistant tenta `HTMLMediaElement.play()` apenas em elementos
visíveis/aces­síveis no contexto atual, com acesso a iframes de mesma origem.
Não clica em anúncios ou controlos de acesso; iframes cross-origin e players
protegidos poderão continuar a exigir intervenção. O diagnóstico não deve
confundir "não foi observada mídia" com "o vídeo está indisponível".
