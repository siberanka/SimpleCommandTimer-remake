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

        changed |= ensureString(config, "Lang.Error_alert", "&cYou do not have permission.");
        changed |= ensureString(config, "Lang.Console_alert", "&cThis command cannot be used from console.");
        changed |= ensureString(config, "Permission.scTimer_permission", "sctimer.admin");

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
        if (!config.contains(path)) {
            config.set(path, value);
            return true;
        }
        return false;
    }

    private static boolean ensureBoolean(FileConfiguration config, String path, boolean value) {
        if (!config.contains(path)) {
            config.set(path, Boolean.valueOf(value));
            return true;
        }
        return false;
    }

    private static boolean ensureStringList(FileConfiguration config, String path, List<String> value) {
        if (!config.contains(path)) {
            config.set(path, value);
            return true;
        }
        return false;
    }
}
