package com.siberanka.simplecommantimer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ConfiguredCommand {
    private final String id;
    private final List<String> commands;
    private final List<ScheduleEntry> schedules;
    private final List<String> embedMessage;
    private final String webhookColor;

    public ConfiguredCommand(
            String id,
            List<String> commands,
            List<ScheduleEntry> schedules,
            List<String> embedMessage,
            String webhookColor
    ) {
        this.id = id;
        this.commands = Collections.unmodifiableList(new ArrayList<String>(commands));
        this.schedules = Collections.unmodifiableList(new ArrayList<ScheduleEntry>(schedules));
        this.embedMessage = Collections.unmodifiableList(new ArrayList<String>(embedMessage));
        this.webhookColor = webhookColor == null ? "" : webhookColor.trim();
    }

    public String getId() {
        return id;
    }

    public List<String> getCommands() {
        return commands;
    }

    public List<ScheduleEntry> getSchedules() {
        return schedules;
    }

    public List<String> getEmbedMessage() {
        return embedMessage;
    }

    public String getWebhookColor() {
        return webhookColor;
    }
}
