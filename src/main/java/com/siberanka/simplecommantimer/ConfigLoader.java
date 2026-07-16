package com.siberanka.simplecommantimer;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ConfigLoader {
    private ConfigLoader() {
    }

    public static List<ConfiguredCommand> loadCommands(FileConfiguration config) {
        ConfigurationSection root = config.getConfigurationSection("Commands");
        if (root == null) {
            return Collections.emptyList();
        }

        List<ConfiguredCommand> results = new ArrayList<ConfiguredCommand>();
        for (String key : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(key);
            if (section == null) {
                continue;
            }

            List<String> commands = section.getStringList("command");
            List<String> scheduleRaw = section.getStringList("schedule");
            List<String> embedMessage = section.getStringList("embed-message");
            String webhookColor = section.getString("webhook-color", "#ffffff");
            if (webhookColor == null || webhookColor.trim().isEmpty()) {
                webhookColor = "#ffffff";
            }

            if (commands.isEmpty() || scheduleRaw.isEmpty()) {
                continue;
            }

            List<ScheduleEntry> schedules = new ArrayList<ScheduleEntry>();
            for (String raw : scheduleRaw) {
                if (raw == null || raw.trim().isEmpty()) {
                    continue;
                }
                schedules.add(ScheduleParser.parse(raw));
            }

            if (schedules.isEmpty()) {
                continue;
            }

            results.add(new ConfiguredCommand(key, commands, schedules, embedMessage, webhookColor));
        }

        return results;
    }
}
