# SimpleCommandTimer

## English
SimpleCommandTimer is a command scheduler plugin for Paper `1.16.5 - 1.21.x / 26.x` and Folia `1.21.x / 26.x`.
It runs commands by day/time in a selected timezone, supports daily schedules, and includes optional Discord webhook embeds per entry.

### Features
- Timezone-based scheduling (`time-zone` via `java.time.ZoneId`)
- Daily and weekday schedules (`DAILY`, `MONDAY`...`SUNDAY`, plus Spanish day names)
- Catch-up logic for temporary lag/freeze periods
- Same entry executes only once per day for each schedule line
- Folia-safe dispatch with Paper fallback
- Paper/Folia 26.x global and entity scheduler support
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

Paper/Folia 26.x compatibility build (requires Java 25):
```bash
mvn -Ppaper-26 clean package
```

Paper/Folia 26.1+ servers require Java 25. Paper 1.20 through 1.21.11 use Java 21; the default legacy-compatible artifact remains Java 8 bytecode.

### Output
- `target/SimpleCommandTimer-1.2.0.jar`

### Command
- `/sctimer reload`
- `/sctimer trigger <entry_id>`

### Configuration
See `src/main/resources/config.yml` for the full example format.

## Türkçe
SimpleCommandTimer, Paper `1.16.5 - 1.21.x / 26.x` ve Folia `1.21.x / 26.x` için komut zamanlayıcı eklentisidir.
Komutları seçilen saat dilimine göre gün/saat bazlı çalıştırır ve girdi bazlı isteğe bağlı Discord webhook embed desteği sunar.

### Özellikler
- Saat dilimi tabanlı zamanlama (`time-zone`)
- Günlük ve haftalık zamanlama (`DAILY`, `MONDAY`...`SUNDAY`, İspanyolca gün adları dâhil)
- Geçici lag/freeze durumlarında catch-up mantığı
- Her zamanlama satırı aynı gün içinde en fazla 1 kez çalışır
- Folia uyumlu dispatch, Paper fallback
- Paper/Folia 26.x global ve entity scheduler desteği
- Girdi bazlı Discord webhook embed gönderimi ve hata durumunda 3 deneme
- Asenkron GitHub sürüm kontrolü ve `sctimer.admin` yetkililerine bildirim
- Oyuncuya gösterilen mesajlar için eksiksiz config desteği
- Sınırlı PlaceholderAPI önbelleği, manuel tetikleme bekleme süresi ve webhook kuyruğu
- Yalnızca resmi HTTPS Discord webhook adreslerine izin veren doğrulama
- **PlaceholderAPI entegrasyonu** (Kalan süre ve en yakın komut tespiti)

### Placeholder'lar
[PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) eklentisinin yüklü olması gerekir.
Zaman biçimleri (son ekler) `config.yml` içerisinden kişiselleştirilebilir (`Placeholder_Format.hours/minutes/seconds`).

- `%sctimer_<entry_id>_hours%` - Belirli bir görev için kalan saat bilgisini döndürür.
- `%sctimer_<entry_id>_minutes%` - Kalan dakikayı döndürür.
- `%sctimer_<entry_id>_seconds%` - Kalan saniyeyi döndürür.
- `%sctimer_<entry_id>_full%` - Tam biçimi döndürür (örn. `1sa 30dk 15sn`).
- `%sctimer_closest_name%` - En yakın zamanda çalışacak komutun `entry_id` değerini döndürür.
- `%sctimer_closest_hours%`, `..._minutes%`, `..._seconds%`, `..._full%` - En yakın komutun ilgili süre biçimlerini döndürür.

### Derleme
```bash
mvn clean package
```

Paper/Folia 26.x uyumluluk derlemesi (Java 25 gerektirir):
```bash
mvn -Ppaper-26 clean package
```

Paper/Folia 26.1+ sunucuları Java 25 gerektirir. Paper 1.20 ile 1.21.11 arası Java 21 kullanır; varsayılan geriye uyumlu artifact Java 8 bytecode olarak kalır.

### Çıktı
- `target/SimpleCommandTimer-1.2.0.jar`

### Komut
- `/sctimer reload`
- `/sctimer trigger <entry_id>`

### Konfig
Tam örnek biçim için `src/main/resources/config.yml` dosyasına bakın.


