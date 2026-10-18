# WiFi TV Hub v1.1.0

Aplicativo Android para celular e Android TV, com descoberta automática mDNS/DNS-SD `_wifitvhub._tcp.`. A televisão anuncia o serviço na LAN enquanto o app está aberto; o celular encontra, seleciona e pede PIN, sem IP manual. Também identifica dispositivos anunciados por `_googlecast._tcp.` como dispositivos **Cast**, mas o Google Cast não autoriza gestão de APKs, comandos remotos universais, nem transferência de arquivos genérica.

O botão Bluetooth lista apenas dispositivos previamente pareados, **não implementa controle remoto via Bluetooth**.

Para instalar APKs: instalar e abrir o WiFi TV Hub na Android TV, selecionar a TV encontrada no celular, informar PIN e enviar arquivo. A instalação requer confirmação do Android. Manter celular e TV na mesma rede local. Se o modo TV não for reconhecido automaticamente, usar o botão "Esta é a televisão? Abrir modo TV".

**Limitações**: Bluetooth não transmite comandos nem arquivos; Google Cast apenas identificado, não controla a TV; funcionamento via servidor HTTP local e PIN/Token, sem TLS; sem execução permanente em segundo plano; não testado em hardware físico. APK debug (pode exigir desinstalação da versão anterior devido a chave debug por runner).

Download em Releases: https://github.com/yuregabrieldev/tiktok-feedback-helpers/releases/tag/wifi-tv-hub-v1.1.0
