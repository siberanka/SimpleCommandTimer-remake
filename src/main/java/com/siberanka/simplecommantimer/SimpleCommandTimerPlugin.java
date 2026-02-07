package com.siberanka.simplecommantimer;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.ZoneId;
import java.util.List;

public final class SimpleCommandTimerPlugin extends JavaPlugin {
    private CommandSchedulerEngine schedulerEngine;
    private ServerDispatcher dispatcher;
    private DiscordWebhookService webhookService;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        dispatcher = new ServerDispatcher(this);
        webhookService = new DiscordWebhookService(this);
        schedulerEngine = new CommandSchedulerEngine(this, dispatcher, webhookService);

        PluginCommand pluginCommand = getCommand("sctimer");
        if (pluginCommand != null) {
            pluginCommand.setExecutor(this);
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
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!"sctimer".equalsIgnoreCase(command.getName())) {
            return false;
        }

        String permissionNode = getConfig().getString("Permission.scTimer_permission", "sctimer.admin");
        if (!sender.hasPermission(permissionNode)) {
            sender.sendMessage(colorize(prefix() + getConfig().getString("Lang.Error_alert", "&cNo permission.")));
            return true;
        }

        if (args.length == 1 && "reload".equalsIgnoreCase(args[0])) {
            reloadAndStart();
            sender.sendMessage(colorize(prefix() + "&aConfig reloaded."));
            return true;
        }

        if (args.length == 2 && "trigger".equalsIgnoreCase(args[0])) {
            String entryId = args[1];
            boolean triggered = schedulerEngine.triggerEntryNow(entryId);
            if (triggered) {
                sender.sendMessage(colorize(prefix() + "&aTriggered entry: &f" + entryId));
            } else {
                sender.sendMessage(colorize(prefix() + "&cEntry not found: &f" + entryId));
            }
            return true;
        }

        sender.sendMessage(colorize(prefix() + "&eUsage: /sctimer reload | /sctimer trigger <entry_id>"));
        return true;
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

        schedulerEngine.start(zoneId, configuredCommands);
        getLogger().info("Loaded " + configuredCommands.size() + " command group(s) with timezone " + zoneId + ".");
    }

    private String prefix() {
        return getConfig().getString("plugin_name", "&7[SimpleCommandTimer] ");
    }

    private String colorize(String value) {
        return ChatColor.translateAlternateColorCodes('&', value);
    }
}
