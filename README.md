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
- Asynchronous GitHub release checks with `sctimer.admin` notifications
- Configurable player-facing messages (including update notifications)
- Bounded PlaceholderAPI cache, manual-trigger cooldown, and bounded webhook queue
- Official HTTPS Discord webhook validation
- **PlaceholderAPI integration** (Remaining time and closest entry detection)

### Placeholders
You must have [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) installed.
Time formats (suffixes) can be customized via `config.yml` (`Placeholder_Format.hours/minutes/seconds`).

- `%sctimer_<entry_id>_hours%` - Returns remaining hours for a specific entry.
- `%sctimer_<entry_id>_minutes%` - Returns remaining minutes.
- `%sctimer_<entry_id>_seconds%` - Returns remaining seconds.
- `%sctimer_<entry_id>_full%` - Returns formatted full remaining time (e.g. `1h 30m 15s`).
- `%sctimer_closest_name%` - Returns the `entry_id` of the closest scheduled command.
- `%sctimer_closest_hours%`, `..._minutes%`, `..._seconds%`, `..._full%` - Returns the respective remaining time of the closest scheduled command.

### Build
```bash
mvn clean package
```

### Output
- `target/SimpleCommandTimer-1.1.0.jar`

### Command
- `/sctimer reload`
- `/sctimer trigger <entry_id>`

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
- Asenkron GitHub surum kontrolu ve `sctimer.admin` yetkililerine bildirim
- Oyuncuya gosterilen mesajlar icin eksiksiz config destegi
- Sinirli PlaceholderAPI onbellegi, manuel tetikleme bekleme suresi ve webhook kuyrugu
- Yalnizca resmi HTTPS Discord webhook adreslerine izin veren dogrulama
- **PlaceholderAPI entegrasyonu** (Kalan sure ve en yakin komut tespiti)

### Placeholder'lar
[PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) eklentisinin yuklu olmasi gerekir.
Zaman formatlari (son ekler) `config.yml` icerisinden kisisellestirilebilir (`Placeholder_Format.hours/minutes/seconds`).

- `%sctimer_<entry_id>_hours%` - Belirli bir gorev icin kalan saat bilgisini dondurur.
- `%sctimer_<entry_id>_minutes%` - Kalan dakikayi dondurur.
- `%sctimer_<entry_id>_seconds%` - Kalan saniyeyi dondurur.
- `%sctimer_<entry_id>_full%` - Tam formati dondurur (orn. `1sa 30dk 15sn`).
- `%sctimer_closest_name%` - En yakin zamanda calisacak olan komutun `entry_id` sini dondurur.
- `%sctimer_closest_hours%`, `..._minutes%`, `..._seconds%`, `..._full%` - En yakin komutun ilgili sure formatlarini dondurur.

### Derleme
```bash
mvn clean package
```

### Cikti
- `target/SimpleCommandTimer-1.1.0.jar`

### Komut
- `/sctimer reload`
- `/sctimer trigger <entry_id>`

### Konfig
Tam ornek format icin `src/main/resources/config.yml` dosyasina bakin.


