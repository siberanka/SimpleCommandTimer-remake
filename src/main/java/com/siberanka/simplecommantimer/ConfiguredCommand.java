package com.siberanka.simplecommantimer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ConfiguredCommand {
    private final String id;
    private final List<String> commands;
    private final List<ScheduleEntry> schedules;
    private final List<String> embedMessage;

    public ConfiguredCommand(String id, List<String> commands, List<ScheduleEntry> schedules, List<String> embedMessage) {
        this.id = id;
        this.commands = Collections.unmodifiableList(new ArrayList<String>(commands));
        this.schedules = Collections.unmodifiableList(new ArrayList<ScheduleEntry>(schedules));
        this.embedMessage = Collections.unmodifiableList(new ArrayList<String>(embedMessage));
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
}
