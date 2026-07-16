package com.siberanka.simplecommantimer;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;

public final class ConfigIntegrityService {
    private static final String DEFAULT_WEBHOOK_COLOR = "#ffffff";

    private ConfigIntegrityService() {
    }

    public static boolean ensure(FileConfiguration config) {
        boolean changed = false;

        changed |= ensureString(config, "plugin_name", "&7[SimpleCommandTimer] ");
        changed |= ensureString(config, "time-zone", "UTC");
        changed |= ensureBoolean(config, "discord-webhook", false);
        changed |= ensureString(config, "webhook-url", "");
        changed |= ensureBoolean(config, "Update_Check.enabled", true);
        changed |= ensureString(config, "Update_Check.repository", "siberanka/SimpleCommandTimer-remake");
        changed |= ensureInteger(config, "Update_Check.check-interval-hours", 6);
        changed |= ensureBoolean(config, "Update_Check.notify-on-join", true);
        changed |= ensureInteger(config, "Command.trigger-cooldown-ms", 1000);

        changed |= ensureString(config, "Lang.Error_alert", "&cYou do not have permission.");
        changed |= ensureString(config, "Lang.Console_alert", "&cThis command cannot be used from console.");
        changed |= ensureString(config, "Lang.Config_reloaded", "&aConfiguration reloaded.");
        changed |= ensureString(config, "Lang.Entry_triggered", "&aTriggered entry: &f%entry%");
        changed |= ensureString(config, "Lang.Entry_not_found", "&cEntry not found: &f%entry%");
        changed |= ensureString(config, "Lang.Trigger_rate_limited",
                "&cPlease wait before triggering another entry.");
        changed |= ensureString(config, "Lang.Usage", "&eUsage: /sctimer reload | /sctimer trigger <entry_id>");
        changed |= ensureString(config, "Lang.Update_available",
                "&eA new version is available: &f%latest_version% &7(current: %current_version%) &b%release_url%");
        changed |= ensureString(config, "Permission.scTimer_permission", "sctimer.admin");

        changed |= ensureString(config, "Placeholder_Format.hours", "h ");
        changed |= ensureString(config, "Placeholder_Format.minutes", "m ");
        changed |= ensureString(config, "Placeholder_Format.seconds", "s");
        changed |= ensureString(config, "Placeholder_Format.none", "None");

        ConfigurationSection commands = config.getConfigurationSection("Commands");
        if (commands == null) {
            config.createSection("Commands");
            return true;
        }

        for (String key : commands.getKeys(false)) {
            String base = "Commands." + key;
            changed |= ensureStringList(config, base + ".command", new ArrayList<String>());
            changed |= ensureStringList(config, base + ".schedule", new ArrayList<String>());
            changed |= ensureStringList(config, base + ".embed-message", new ArrayList<String>());
            changed |= ensureString(config, base + ".webhook-color", DEFAULT_WEBHOOK_COLOR);

            String color = config.getString(base + ".webhook-color", DEFAULT_WEBHOOK_COLOR);
            if (color == null || color.trim().isEmpty()) {
                config.set(base + ".webhook-color", DEFAULT_WEBHOOK_COLOR);
                changed = true;
            }
        }

        return changed;
    }

    private static boolean ensureString(FileConfiguration config, String path, String value) {
        if (!config.isString(path)) {
            config.set(path, value);
            return true;
        }
        return false;
    }

    private static boolean ensureBoolean(FileConfiguration config, String path, boolean value) {
        if (!config.isBoolean(path)) {
            config.set(path, Boolean.valueOf(value));
            return true;
        }
        return false;
    }

    private static boolean ensureInteger(FileConfiguration config, String path, int value) {
        if (!config.isInt(path)) {
            config.set(path, Integer.valueOf(value));
            return true;
        }
        return false;
    }

    private static boolean ensureStringList(FileConfiguration config, String path, List<String> value) {
        if (!config.isList(path) || config.getList(path) == null) {
            config.set(path, value);
            return true;
        }
        for (Object element : config.getList(path)) {
            if (!(element instanceof String)) {
                config.set(path, value);
                return true;
            }
        }
        return false;
    }
}
