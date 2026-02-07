# SimpleCommandTimer

## English
SimpleCommandTimer is a command scheduler plugin for Paper `1.16 - 1.21.x` and Folia `1.21.1`.
It runs commands by day/time in a selected timezone, supports daily schedules, and includes optional Discord webhook embeds per entry.

### Features
- Timezone-based scheduling (`time-zone` via `java.time.ZoneId`)
- Daily and weekday schedules (`DAILY`, `MONDAY`...`SUNDAY`, plus Spanish day names)
- Catch-up logic for temporary lag/freeze periods
- Same entry executes only once per day for each schedule line
- Folia-safe dispatch with Paper fallback
- Optional Discord webhook embed per command entry with 3 retries

### Build
```bash
mvn clean package
```

### Output
- `target/SimpleCommandTimer-1.0.2.jar`

### Command
- `/sctimer reload`

### Configuration
See `src/main/resources/config.yml` for the full example format.

## Turkce
SimpleCommandTimer, Paper `1.16 - 1.21.x` ve Folia `1.21.1` icin komut zamanlayici eklentisidir.
Komutlari secilen saat dilimine gore gun/saat bazli calistirir ve entry bazli opsiyonel Discord webhook embed destegi sunar.

### Ozellikler
- Saat dilimi tabanli zamanlama (`time-zone`)
- Gunluk ve haftalik zamanlama (`DAILY`, `MONDAY`...`SUNDAY`, Ispanyolca gun adlari dahil)
- Gecici lag/freeze durumlarinda catch-up mantigi
- Her schedule satiri ayni gun icinde en fazla 1 kez calisir
- Folia uyumlu dispatch, Paper fallback
- Entry bazli Discord webhook embed gonderimi ve hata durumunda 3 deneme

### Derleme
```bash
mvn clean package
```

### Cikti
- `target/SimpleCommandTimer-1.0.2.jar`

### Komut
- `/sctimer reload`

### Konfig
Tam ornek format icin `src/main/resources/config.yml` dosyasina bakin.

