# MuzMod - Minecraft 1.8.9 Forge Mod

Minecraft sunucularında otomatik görev yönetimi için tasarlanmış bir mod.

## Özellikler

### State (Durum) Sistemi
- **Idle**: Bekleme modu - hiçbir şey yapmaz
- **Mining**: Otomatik maden kazma
- **AFK**: AFK bölgesine warp ve bekleme

### Mining (Maden) Özellikleri
- Nether Quartz ore'larını otomatik bulma ve client-side işaretleme
- Gerçekçi kazma simülasyonu (paket göndermek yerine tuş basılı tutma)
- 45 derece aşağı bakarak kazma
- Anti-AFK hareketleri (smooth 10 derece sola/sağa dönme)
- Rastgele hareket (bazen dur, bazen yürü)
- Oyuncu algılama ve kaçınma sistemi

### Oyuncu Algılama
- 3 blok yarıçaplı halka client-side çizimi
- Yakınlarda oyuncu varsa otomatik uzaklaşma
- Güvenli mesafede kazıma devam etme

### Zamanlama
- PC saatine göre otomatik state geçişleri
- Örnek: 04:30 - 04:40 arası Mining, sonra AFK

## Kullanım

### Tuş Atamaları
- **RSHIFT**: Mod menüsünü aç
- Menüden bot'u aktif/deaktif yapabilirsin

### Menü
- Bot durumu kontrolü
- Mining/AFK saat ayarları
- Warp komutları ayarlama
- Oyuncu algılama yarıçapı

## Kurulum

1. Forge 1.8.9 kurulu olmalı
2. Modu `mods` klasörüne koy
3. Oyunu başlat

## Build

```bash
gradlew build
```

Build edilen jar dosyası `build/libs/` klasöründe oluşacak.

## Yapılandırma

Config dosyası: `.minecraft/config/muzmod.cfg`

### Mining Ayarları
- `miningWarpCommand`: Maden bölgesine warp komutu
- `miningStartHour/Minute`: Başlangıç saati
- `miningEndHour/Minute`: Bitiş saati

### AFK Ayarları
- `afkWarpCommand`: AFK bölgesine warp komutu
- `afkStartHour/Minute`: Başlangıç saati
- `afkEndHour/Minute`: Bitiş saati

### Oyuncu Algılama
- `playerDetectionRadius`: Algılama yarıçapı (blok)
- `avoidPlayers`: Oyunculardan kaçın (true/false)

## Ekran Görüntüsü

Oyun içinde:
- Sol üstte durum overlay'i
- Etrafında yeşil/kırmızı oyuncu algılama halkası
- Bulunan ore'lar cyan ile işaretli
- Hedef ore sarı ile işaretli
