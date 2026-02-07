package com.simplecommandtimer;

import org.bukkit.plugin.java.JavaPlugin;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.zone.ZoneOffsetTransition;
import java.time.zone.ZoneRules;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public final class CommandSchedulerEngine {
    private final JavaPlugin plugin;
    private final ServerDispatcher dispatcher;

    private ScheduledExecutorService executor;
    private volatile Instant lastChecked;
    private volatile ZoneId zoneId;
    private volatile List<ConfiguredCommand> configuredCommands = Collections.emptyList();

    public CommandSchedulerEngine(JavaPlugin plugin, ServerDispatcher dispatcher) {
        this.plugin = plugin;
        this.dispatcher = dispatcher;
    }

    public synchronized void start(ZoneId zoneId, List<ConfiguredCommand> commands) {
        stop();

        this.zoneId = zoneId;
        this.configuredCommands = commands;
        this.lastChecked = Instant.now().minusSeconds(1);

        this.executor = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r, "SimpleCommandTimer-Scheduler");
                thread.setDaemon(true);
                return thread;
            }
        });

        this.executor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                tick();
            }
        }, 250L, 1000L, TimeUnit.MILLISECONDS);
    }

    public synchronized void stop() {
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }

    private void tick() {
        try {
            Instant now = Instant.now();
            Instant previous = lastChecked;

            if (previous == null) {
                previous = now.minusSeconds(1);
            }

            if (now.isBefore(previous)) {
                previous = now.minusSeconds(1);
            }

            List<ConfiguredCommand> commandsSnapshot = configuredCommands;
            ZoneId zoneSnapshot = zoneId;

            ZonedDateTime from = previous.atZone(zoneSnapshot);
            ZonedDateTime to = now.atZone(zoneSnapshot);

            for (ConfiguredCommand configuredCommand : commandsSnapshot) {
                if (isAnyScheduleDue(configuredCommand.getSchedules(), from, to, zoneSnapshot)) {
                    dispatcher.dispatchCommands(configuredCommand.getCommands());
                }
            }

            lastChecked = now;
        } catch (Throwable t) {
            plugin.getLogger().severe("Scheduler tick failed: " + t.getMessage());
        }
    }

    private boolean isAnyScheduleDue(List<ScheduleEntry> schedules, ZonedDateTime from, ZonedDateTime to, ZoneId zone) {
        for (ScheduleEntry schedule : schedules) {
            if (isScheduleDue(schedule, from, to, zone)) {
                return true;
            }
        }
        return false;
    }

    private boolean isScheduleDue(ScheduleEntry schedule, ZonedDateTime from, ZonedDateTime to, ZoneId zone) {
        LocalDate startDate = from.toLocalDate().minusDays(1);
        LocalDate endDate = to.toLocalDate().plusDays(1);

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            if (!matchesDay(schedule, date.getDayOfWeek())) {
                continue;
            }

            if (matchesAnyOffset(date, schedule, zone, from.toInstant(), to.toInstant())) {
                return true;
            }
        }

        return false;
    }

    private boolean matchesDay(ScheduleEntry schedule, DayOfWeek day) {
        return schedule.isDaily() || schedule.getDayOfWeek() == day;
    }

    private boolean matchesAnyOffset(LocalDate date, ScheduleEntry schedule, ZoneId zone, Instant from, Instant to) {
        LocalDateTime localDateTime = LocalDateTime.of(
                date.getYear(),
                date.getMonthValue(),
                date.getDayOfMonth(),
                schedule.getHour(),
                schedule.getMinute(),
                schedule.getSecond()
        );

        ZoneRules rules = zone.getRules();
        List<ZoneOffset> validOffsets = rules.getValidOffsets(localDateTime);

        if (validOffsets.isEmpty()) {
            ZoneOffsetTransition transition = rules.getTransition(localDateTime);
            if (transition == null) {
                return false;
            }

            Instant candidate = transition.getDateTimeAfter().atZone(zone).toInstant();
            return candidate.isAfter(from) && !candidate.isAfter(to);
        }

        for (ZoneOffset offset : validOffsets) {
            Instant candidate = ZonedDateTime.ofLocal(localDateTime, zone, offset).toInstant();
            if (candidate.isAfter(from) && !candidate.isAfter(to)) {
                return true;
            }
        }

        return false;
    }
}
