package com.siberanka.simplecommantimer;

import org.bukkit.plugin.java.JavaPlugin;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Pattern;

final class UpdateChecker {
    private static final Pattern REPOSITORY_PATTERN = Pattern.compile("[A-Za-z0-9_.-]{1,100}/[A-Za-z0-9_.-]{1,100}");
    private static final int MAX_RESPONSE_CHARS = 65536;

    private final JavaPlugin plugin;
    private final ScheduledExecutorService executor;
    private ScheduledFuture<?> task;

    UpdateChecker(JavaPlugin plugin) {
        this.plugin = plugin;
        this.executor = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable runnable) {
                Thread thread = new Thread(runnable, "SimpleCommandTimer-UpdateChecker");
                thread.setDaemon(true);
                return thread;
            }
        });
    }

    synchronized void start(boolean enabled, final String repository, int intervalHours,
            final Consumer<Result> callback) {
        if (task != null) {
            task.cancel(false);
            task = null;
        }
        if (!enabled) {
            return;
        }
        if (repository == null || !REPOSITORY_PATTERN.matcher(repository.trim()).matches()) {
            plugin.getLogger().warning("Update check disabled: repository must use the owner/name format.");
            return;
        }

        final String safeRepository = repository.trim();
        long safeInterval = Math.max(1L, Math.min(168L, intervalHours));
        task = executor.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                try {
                    callback.accept(fetch(safeRepository));
                } catch (Exception exception) {
                    plugin.getLogger().warning("GitHub update check failed: " + safeMessage(exception));
                }
            }
        }, 0L, safeInterval, TimeUnit.HOURS);
    }

    synchronized void shutdown() {
        if (task != null) {
            task.cancel(true);
            task = null;
        }
        executor.shutdownNow();
    }

    private Result fetch(String repository) throws Exception {
        URL url = new URL("https://api.github.com/repos/" + repository + "/releases/latest");
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        connection.setInstanceFollowRedirects(false);
        connection.setRequestProperty("Accept", "application/vnd.github+json");
        connection.setRequestProperty("User-Agent", "SimpleCommandTimer/" + plugin.getDescription().getVersion());

        try {
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpsURLConnection.HTTP_OK) {
                throw new IllegalStateException("GitHub API returned HTTP " + responseCode);
            }
            String response = readBounded(connection.getInputStream());
            String latestVersion = extractJsonString(response, "tag_name");
            String releaseUrl = extractJsonString(response, "html_url");
            if (latestVersion == null || latestVersion.trim().isEmpty()
                    || releaseUrl == null || !releaseUrl.startsWith("https://github.com/")) {
                throw new IllegalStateException("GitHub release response is incomplete");
            }
            String currentVersion = plugin.getDescription().getVersion();
            return new Result(latestVersion, currentVersion, releaseUrl,
                    VersionComparator.isNewer(latestVersion, currentVersion));
        } finally {
            connection.disconnect();
        }
    }

    private String readBounded(InputStream inputStream) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        StringBuilder result = new StringBuilder();
        char[] buffer = new char[2048];
        int read;
        while ((read = reader.read(buffer)) != -1) {
            if (result.length() + read > MAX_RESPONSE_CHARS) {
                throw new IllegalStateException("GitHub response exceeded the size limit");
            }
            result.append(buffer, 0, read);
        }
        return result.toString();
    }

    static String extractJsonString(String json, String key) {
        String marker = "\"" + key + "\"";
        int keyIndex = json.indexOf(marker);
        if (keyIndex < 0) {
            return null;
        }
        int colon = json.indexOf(':', keyIndex + marker.length());
        int quote = colon < 0 ? -1 : json.indexOf('"', colon + 1);
        if (quote < 0) {
            return null;
        }
        StringBuilder value = new StringBuilder();
        boolean escaped = false;
        for (int i = quote + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escaped) {
                switch (c) {
                    case '"':
                    case '\\':
                    case '/':
                        value.append(c);
                        break;
                    case 'b': value.append('\b'); break;
                    case 'f': value.append('\f'); break;
                    case 'n': value.append('\n'); break;
                    case 'r': value.append('\r'); break;
                    case 't': value.append('\t'); break;
                    default: return null;
                }
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == '"') {
                return value.toString();
            } else {
                value.append(c);
            }
        }
        return null;
    }

    private String safeMessage(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.trim().isEmpty() ? exception.getClass().getSimpleName() : message;
    }

    static final class Result {
        private final String latestVersion;
        private final String currentVersion;
        private final String releaseUrl;
        private final boolean updateAvailable;

        Result(String latestVersion, String currentVersion, String releaseUrl, boolean updateAvailable) {
            this.latestVersion = latestVersion;
            this.currentVersion = currentVersion;
            this.releaseUrl = releaseUrl;
            this.updateAvailable = updateAvailable;
        }

        String getLatestVersion() { return latestVersion; }
        String getCurrentVersion() { return currentVersion; }
        String getReleaseUrl() { return releaseUrl; }
        boolean isUpdateAvailable() { return updateAvailable; }
    }
}
