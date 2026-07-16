package com.siberanka.simplecommantimer;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SimpleCommandTimerPlugin extends JavaPlugin implements Listener {
    private static final String ADMIN_PERMISSION = "sctimer.admin";

    private CommandSchedulerEngine schedulerEngine;
    private ServerDispatcher dispatcher;
    private DiscordWebhookService webhookService;
    private UpdateChecker updateChecker;
    private volatile UpdateChecker.Result updateResult;
    private volatile String lastAnnouncedVersion = "";
    private final Map<UUID, Long> lastManualTrigger = new ConcurrentHashMap<UUID, Long>();
    private volatile List<String> knownEntryIds = Collections.emptyList();

    @Override
    public void onEnable() {
        saveDefaultConfig();

        dispatcher = new ServerDispatcher(this);
        webhookService = new DiscordWebhookService(this);
        updateChecker = new UpdateChecker(this);
        schedulerEngine = new CommandSchedulerEngine(this, dispatcher, webhookService);
        getServer().getPluginManager().registerEvents(this, this);

        PluginCommand pluginCommand = getCommand("sctimer");
        if (pluginCommand != null) {
            pluginCommand.setExecutor(this);
            pluginCommand.setTabCompleter(this);
        }

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new SCTPlaceholderExpansion(this, schedulerEngine).register();
            getLogger().info("PlaceholderAPI expression successfully registered!");
        }

        reloadAndStart();
    }

    @Override
    public void onDisable() {
        if (schedulerEngine != null) {
            schedulerEngine.stop();
        }
        if (webhookService != null) {
            webhookService.shutdown();
        }
        if (updateChecker != null) {
            updateChecker.shutdown();
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!"sctimer".equalsIgnoreCase(command.getName())) {
            return false;
        }

        String permissionNode = getConfig().getString("Permission.scTimer_permission", "sctimer.admin");
        if (!sender.hasPermission(permissionNode)) {
            sendConfiguredMessage(sender, "Lang.Error_alert");
            return true;
        }

        if (args.length == 1 && "reload".equalsIgnoreCase(args[0])) {
            reloadAndStart();
            sendConfiguredMessage(sender, "Lang.Config_reloaded");
            return true;
        }

        if (args.length == 2 && "trigger".equalsIgnoreCase(args[0])) {
            if (sender instanceof Player && !allowManualTrigger((Player) sender)) {
                sendConfiguredMessage(sender, "Lang.Trigger_rate_limited");
                return true;
            }
            String entryId = args[1];
            boolean triggered = schedulerEngine.triggerEntryNow(entryId);
            if (triggered) {
                sendConfiguredMessage(sender, "Lang.Entry_triggered", "%entry%", entryId);
            } else {
                sendConfiguredMessage(sender, "Lang.Entry_not_found", "%entry%", entryId);
            }
            return true;
        }

        sendConfiguredMessage(sender, "Lang.Usage");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!"sctimer".equalsIgnoreCase(command.getName())) {
            return Collections.emptyList();
        }

        if (!canTabComplete(sender)) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            return filterByPrefix(args[0], asList("reload", "trigger"));
        }

        if (args.length == 2 && "trigger".equalsIgnoreCase(args[0])) {
            return filterByPrefix(args[1], knownEntryIds);
        }

        return Collections.emptyList();
    }

    private void reloadAndStart() {
        reloadConfig();
        FileConfiguration config = getConfig();
        boolean configChanged = ConfigIntegrityService.ensure(config);
        if (configChanged) {
            saveConfig();
            getLogger().info("Config integrity check added missing values with defaults.");
        }

        ZoneId zoneId;
        try {
            zoneId = ZoneId.of(config.getString("time-zone", "UTC"));
        } catch (Exception ex) {
            getLogger().warning("Invalid time-zone in config, falling back to UTC: " + ex.getMessage());
            zoneId = ZoneId.of("UTC");
        }

        List<ConfiguredCommand> configuredCommands;
        try {
            configuredCommands = ConfigLoader.loadCommands(config);
        } catch (Exception ex) {
            getLogger().severe("Failed to load command schedules: " + ex.getMessage());
            configuredCommands = java.util.Collections.emptyList();
        }

        boolean discordWebhookEnabled = config.getBoolean("discord-webhook", false);
        String webhookUrl = config.getString("webhook-url", "");
        webhookService.updateSettings(discordWebhookEnabled, webhookUrl);

        boolean updateCheckEnabled = config.getBoolean("Update_Check.enabled", true);
        updateResult = null;
        updateChecker.start(
                updateCheckEnabled,
                config.getString("Update_Check.repository", "siberanka/SimpleCommandTimer-remake"),
                config.getInt("Update_Check.check-interval-hours", 6),
                new java.util.function.Consumer<UpdateChecker.Result>() {
                    @Override
                    public void accept(UpdateChecker.Result result) {
                        updateResult = result;
                        if (result.isUpdateAvailable()
                                && !result.getLatestVersion().equals(lastAnnouncedVersion)) {
                            lastAnnouncedVersion = result.getLatestVersion();
                            notifyOnlineAdmins(result);
                        }
                    }
                });

        schedulerEngine.start(zoneId, configuredCommands);
        knownEntryIds = collectEntryIds(configuredCommands);
        getLogger().info("Loaded " + configuredCommands.size() + " command group(s) with timezone " + zoneId + ".");
    }

    private boolean canTabComplete(CommandSender sender) {
        String permissionNode = getConfig().getString("Permission.scTimer_permission", ADMIN_PERMISSION);
        return sender instanceof ConsoleCommandSender || sender.hasPermission(permissionNode);
    }

    private List<String> collectEntryIds(List<ConfiguredCommand> configuredCommands) {
        List<String> ids = new ArrayList<String>();
        for (ConfiguredCommand configuredCommand : configuredCommands) {
            ids.add(configuredCommand.getId());
        }
        return Collections.unmodifiableList(ids);
    }

    private List<String> filterByPrefix(String arg, List<String> source) {
        String prefix = arg == null ? "" : arg.toLowerCase(Locale.ROOT);
        List<String> results = new ArrayList<String>();
        for (String value : source) {
            if (value.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                results.add(value);
            }
        }
        return results;
    }

    private List<String> asList(String first, String second) {
        List<String> values = new ArrayList<String>();
        values.add(first);
        values.add(second);
        return values;
    }

    private String prefix() {
        return getConfig().getString("plugin_name", "&7[SimpleCommandTimer] ");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        UpdateChecker.Result result = updateResult;
        if (result != null && result.isUpdateAvailable()
                && getConfig().getBoolean("Update_Check.notify-on-join", true)
                && event.getPlayer().hasPermission(ADMIN_PERMISSION)) {
            sendUpdateMessage(event.getPlayer(), result);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        lastManualTrigger.remove(event.getPlayer().getUniqueId());
    }

    private boolean allowManualTrigger(Player player) {
        long cooldown = Math.max(0L, Math.min(60000L,
                getConfig().getLong("Command.trigger-cooldown-ms", 1000L)));
        if (cooldown == 0L) {
            return true;
        }
        long now = System.currentTimeMillis();
        Long previous = lastManualTrigger.put(player.getUniqueId(), Long.valueOf(now));
        return previous == null || now - previous.longValue() >= cooldown;
    }

    private void notifyOnlineAdmins(final UpdateChecker.Result result) {
        if (!isEnabled()) {
            return;
        }
        dispatcher.runGlobal(new Runnable() {
            @Override
            public void run() {
                for (final Player player : getServer().getOnlinePlayers()) {
                    if (!player.hasPermission(ADMIN_PERMISSION)) {
                        continue;
                    }
                    dispatcher.runForPlayer(player, new Runnable() {
                        @Override
                        public void run() {
                            if (player.isOnline() && player.hasPermission(ADMIN_PERMISSION)) {
                                sendUpdateMessage(player, result);
                            }
                        }
                    });
                }
            }
        });
    }

    private void sendUpdateMessage(CommandSender sender, UpdateChecker.Result result) {
        sendConfiguredMessage(sender, "Lang.Update_available",
                "%latest_version%", result.getLatestVersion(),
                "%current_version%", result.getCurrentVersion(),
                "%release_url%", result.getReleaseUrl());
    }

    private void sendConfiguredMessage(CommandSender sender, String path, String... replacements) {
        String configured = getConfig().getString(path);
        if (configured == null || configured.trim().isEmpty()) {
            return;
        }
        String message = configured;
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            message = message.replace(replacements[i], replacements[i + 1]);
        }
        sender.sendMessage(colorize(prefix() + message));
    }

    private String colorize(String value) {
        return ChatColor.translateAlternateColorCodes('&', value);
    }
}
