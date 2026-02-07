package com.simplecommandtimer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ConfiguredCommand {
    private final String id;
    private final List<String> commands;
    private final List<ScheduleEntry> schedules;

    public ConfiguredCommand(String id, List<String> commands, List<ScheduleEntry> schedules) {
        this.id = id;
        this.commands = Collections.unmodifiableList(new ArrayList<String>(commands));
        this.schedules = Collections.unmodifiableList(new ArrayList<ScheduleEntry>(schedules));
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
}
