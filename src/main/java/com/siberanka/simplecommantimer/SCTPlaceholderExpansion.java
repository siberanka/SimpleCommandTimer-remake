package com.siberanka.simplecommantimer;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SCTPlaceholderExpansion extends PlaceholderExpansion {

    private final SimpleCommandTimerPlugin plugin;
    private final CommandSchedulerEngine engine;

    private final Map<String, CachedResult> cache = new ConcurrentHashMap<String, CachedResult>();
    private static final long CACHE_DURATION_MS = 1000L; // 1 second cache
    private static final int MAX_PARAM_LENGTH = 128;
    private static final int MAX_CACHE_ENTRIES = 256;

    public SCTPlaceholderExpansion(SimpleCommandTimerPlugin plugin, CommandSchedulerEngine engine) {
        this.plugin = plugin;
        this.engine = engine;
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public String getIdentifier() {
        return "sctimer";
    }

    @Override
    public String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (params == null || params.length() > MAX_PARAM_LENGTH) {
            return "";
        }
        // Cache layer to prevent spamming calculation
        CachedResult cached = cache.get(params);
        if (cached != null && !cached.isExpired()) {
            return cached.getValue();
        }

        String result = calculateResult(params);
        if (result != null) {
            cacheResult(params, result);
        }

        return result == null ? "" : result;
    }

    private String calculateResult(String params) {
        if (params.equalsIgnoreCase("closest_name")) {
            ConfiguredCommand closest = engine.getClosestCommand();
            return closest != null ? closest.getId() : noneValue();
        }

        if (params.equalsIgnoreCase("closest_hours")) {
            ConfiguredCommand closest = engine.getClosestCommand();
            if (closest == null)
                return "0";
            return String.valueOf(getHours(closest.getId()));
        }

        if (params.equalsIgnoreCase("closest_minutes")) {
            ConfiguredCommand closest = engine.getClosestCommand();
            if (closest == null)
                return "0";
            return String.valueOf(getMinutes(closest.getId()));
        }

        if (params.equalsIgnoreCase("closest_seconds")) {
            ConfiguredCommand closest = engine.getClosestCommand();
            if (closest == null)
                return "0";
            return String.valueOf(getSeconds(closest.getId()));
        }

        if (params.equalsIgnoreCase("closest_full")) {
            ConfiguredCommand closest = engine.getClosestCommand();
            if (closest == null)
                return noneValue();
            return getFullString(closest.getId());
        }

        // Format: <entry_id>_<type>
        int lastIndex = params.lastIndexOf('_');
        if (lastIndex == -1) {
            return null;
        }

        String entryId = params.substring(0, lastIndex);
        String type = params.substring(lastIndex + 1);

        if (type.equalsIgnoreCase("hours")) {
            return String.valueOf(getHours(entryId));
        } else if (type.equalsIgnoreCase("minutes")) {
            return String.valueOf(getMinutes(entryId));
        } else if (type.equalsIgnoreCase("seconds")) {
            return String.valueOf(getSeconds(entryId));
        } else if (type.equalsIgnoreCase("full")) {
            return getFullString(entryId);
        }

        return null;
    }

    private long getRemainingSeconds(String entryId) {
        Long nextEpoch = engine.getNextExecutionEpoch(entryId);
        if (nextEpoch == null)
            return 0;

        long diff = nextEpoch.longValue() - Instant.now().getEpochSecond();
        return diff > 0 ? diff : 0;
    }

    private long getHours(String entryId) {
        return getRemainingSeconds(entryId) / 3600;
    }

    private long getMinutes(String entryId) {
        return (getRemainingSeconds(entryId) % 3600) / 60;
    }

    private long getSeconds(String entryId) {
        return getRemainingSeconds(entryId) % 60;
    }

    private String getFullString(String entryId) {
        long remaining = getRemainingSeconds(entryId);
        if (remaining == 0)
            return "0";

        long h = remaining / 3600;
        long m = (remaining % 3600) / 60;
        long s = remaining % 60;

        String hStr = plugin.getConfig().getString("Placeholder_Format.hours", "h ");
        String mStr = plugin.getConfig().getString("Placeholder_Format.minutes", "m ");
        String sStr = plugin.getConfig().getString("Placeholder_Format.seconds", "s");

        StringBuilder sb = new StringBuilder();
        if (h > 0)
            sb.append(h).append(hStr);
        if (m > 0 || h > 0)
            sb.append(m).append(mStr);
        sb.append(s).append(sStr);

        return sb.toString().trim();
    }

    private String noneValue() {
        return plugin.getConfig().getString("Placeholder_Format.none", "");
    }

    private void cacheResult(String params, String result) {
        long now = System.currentTimeMillis();
        if (cache.size() >= MAX_CACHE_ENTRIES) {
            for (Map.Entry<String, CachedResult> entry : cache.entrySet()) {
                if (entry.getValue().isExpired(now)) {
                    cache.remove(entry.getKey(), entry.getValue());
                }
            }
        }
        if (cache.size() < MAX_CACHE_ENTRIES) {
            cache.put(params, new CachedResult(result, now + CACHE_DURATION_MS));
        }
    }

    private static class CachedResult {
        private final String value;
        private final long expireTime;

        public CachedResult(String value, long expireTime) {
            this.value = value;
            this.expireTime = expireTime;
        }

        public String getValue() {
            return value;
        }

        public boolean isExpired() {
            return isExpired(System.currentTimeMillis());
        }

        public boolean isExpired(long now) {
            return now > expireTime;
        }
    }
}
