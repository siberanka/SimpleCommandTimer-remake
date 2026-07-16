package com.siberanka.simplecommantimer;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public final class DiscordWebhookService {
    private static final int MAX_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 2000L;
    private static final int MAX_QUEUE_SIZE = 64;
    private static final int MAX_EMBED_TITLE_LENGTH = 256;

    private final JavaPlugin plugin;
    private final ExecutorService executor;

    private volatile boolean enabled;
    private volatile String webhookUrl;

    public DiscordWebhookService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<Runnable>(MAX_QUEUE_SIZE), new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r, "SimpleCommandTimer-Webhook");
                thread.setDaemon(true);
                return thread;
            }
                }, new ThreadPoolExecutor.AbortPolicy());
    }

    public void updateSettings(boolean enabled, String webhookUrl) {
        this.enabled = enabled;
        this.webhookUrl = webhookUrl == null ? "" : webhookUrl.trim();
    }

    public void sendEmbedForEntry(final ConfiguredCommand configuredCommand) {
        if (!enabled) {
            return;
        }

        if (webhookUrl == null || webhookUrl.isEmpty()) {
            return;
        }

        final List<String> lines = configuredCommand.getEmbedMessage();
        if (lines == null || lines.isEmpty()) {
            return;
        }

        final String title = truncate(joinLines(lines), MAX_EMBED_TITLE_LENGTH);
        if (title.trim().isEmpty()) {
            return;
        }

        final String safeWebhookUrl = validateWebhookUrl(webhookUrl);
        if (safeWebhookUrl == null) {
            plugin.getLogger().warning("Discord webhook URL was rejected; only official HTTPS webhook URLs are allowed.");
            return;
        }

        try {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    Integer color = parseHexColor(configuredCommand.getWebhookColor());
                    sendWithRetry(safeWebhookUrl, configuredCommand.getId(), title, color);
                }
            });
        } catch (RejectedExecutionException rejected) {
            plugin.getLogger().warning("Discord webhook queue is full; newest delivery was rejected.");
        }
    }

    public void shutdown() {
        executor.shutdownNow();
    }

    private void sendWithRetry(String targetUrl, String entryId, String title, Integer color) {
        Exception lastException = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                int code = postWebhook(targetUrl, title, color);
                if (code >= 200 && code < 300) {
                    return;
                }
                lastException = new IllegalStateException("HTTP " + code);
            } catch (Exception ex) {
                lastException = ex;
            }

            if (attempt < MAX_ATTEMPTS) {
                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }

        if (lastException != null) {
            plugin.getLogger().warning("Discord webhook failed for entry '" + entryId + "' after " + MAX_ATTEMPTS + " attempts: " + lastException.getMessage());
        }
    }

    private int postWebhook(String targetUrl, String title, Integer color) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(targetUrl).openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

        String payload = buildPayload(title, color);
        byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);

        OutputStream outputStream = connection.getOutputStream();
        outputStream.write(bytes);
        outputStream.flush();
        outputStream.close();

        int responseCode = connection.getResponseCode();
        if (responseCode >= 400) {
            InputStream errorStream = connection.getErrorStream();
            if (errorStream != null) {
                readFully(errorStream);
                errorStream.close();
            }
        }

        connection.disconnect();
        return responseCode;
    }

    private String validateWebhookUrl(String value) {
        try {
            URI uri = new URI(value);
            String host = uri.getHost();
            boolean officialHost = "discord.com".equalsIgnoreCase(host)
                    || "www.discord.com".equalsIgnoreCase(host)
                    || "discordapp.com".equalsIgnoreCase(host)
                    || "www.discordapp.com".equalsIgnoreCase(host);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || !officialHost
                    || uri.getUserInfo() != null || (uri.getPort() != -1 && uri.getPort() != 443)
                    || uri.getPath() == null || !uri.getPath().startsWith("/api/webhooks/")) {
                return null;
            }
            return uri.toASCIIString();
        } catch (Exception exception) {
            return null;
        }
    }

    private String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private String buildPayload(String title, Integer color) {
        StringBuilder builder = new StringBuilder();
        builder.append("{\"embeds\":[{");
        builder.append("\"title\":\"").append(escapeJson(title)).append("\",");
        if (color != null) {
            builder.append("\"color\":").append(color.intValue()).append(",");
        }
        builder.append("\"timestamp\":\"").append(Instant.now().toString()).append("\"");
        builder.append("}]}");
        return builder.toString();
    }

    private Integer parseHexColor(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        if (normalized.isEmpty()) {
            return null;
        }

        if (normalized.startsWith("#")) {
            normalized = normalized.substring(1);
        }

        if (normalized.length() != 6) {
            return null;
        }

        for (int i = 0; i < normalized.length(); i++) {
            char c = normalized.charAt(i);
            boolean hex = (c >= '0' && c <= '9')
                    || (c >= 'a' && c <= 'f')
                    || (c >= 'A' && c <= 'F');
            if (!hex) {
                return null;
            }
        }

        try {
            return Integer.valueOf(Integer.parseInt(normalized, 16));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String joinLines(List<String> lines) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line == null) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(" ");
            }
            builder.append(line);
        }
        return builder.toString();
    }

    private String readFully(InputStream inputStream) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        StringBuilder builder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            builder.append(line);
        }
        return builder.toString();
    }

    private String escapeJson(String text) {
        StringBuilder escaped = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '\\':
                    escaped.append("\\\\");
                    break;
                case '"':
                    escaped.append("\\\"");
                    break;
                case '\n':
                    escaped.append("\\n");
                    break;
                case '\r':
                    escaped.append("\\r");
                    break;
                case '\t':
                    escaped.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        escaped.append(String.format("\\u%04x", Integer.valueOf(c)));
                    } else {
                        escaped.append(c);
                    }
                    break;
            }
        }
        return escaped.toString();
    }
}
