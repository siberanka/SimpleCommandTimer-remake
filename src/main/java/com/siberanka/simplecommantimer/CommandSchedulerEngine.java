package com.siberanka.simplecommantimer;

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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public final class CommandSchedulerEngine {
    private static final long EXECUTION_MEMORY_SECONDS = 3L * 24L * 60L * 60L;

    private final JavaPlugin plugin;
    private final ServerDispatcher dispatcher;
    private final DiscordWebhookService webhookService;
    private final Map<String, Long> executedOccurrences = new ConcurrentHashMap<String, Long>();

    private ScheduledExecutorService executor;
    private volatile Instant lastChecked;
    private volatile ZoneId zoneId;
    private volatile List<ConfiguredCommand> configuredCommands = Collections.emptyList();

    public CommandSchedulerEngine(JavaPlugin plugin, ServerDispatcher dispatcher, DiscordWebhookService webhookService) {
        this.plugin = plugin;
        this.dispatcher = dispatcher;
        this.webhookService = webhookService;
    }

    public synchronized void start(ZoneId zoneId, List<ConfiguredCommand> commands) {
        stop();

        this.zoneId = zoneId;
        this.configuredCommands = commands;
        this.lastChecked = Instant.now().minusSeconds(1);
        this.executedOccurrences.clear();

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
                int dueExecutions = countAndMarkDueExecutions(configuredCommand, from, to, zoneSnapshot);
                for (int i = 0; i < dueExecutions; i++) {
                    dispatcher.dispatchCommands(configuredCommand.getCommands());
                    webhookService.sendEmbedForEntry(configuredCommand);
                }
            }

            pruneExecutionMemory(now);
            lastChecked = now;
        } catch (Throwable t) {
            plugin.getLogger().severe("Scheduler tick failed: " + t.getMessage());
        }
    }

    private int countAndMarkDueExecutions(ConfiguredCommand configuredCommand, ZonedDateTime from, ZonedDateTime to, ZoneId zone) {
        Set<DueOccurrence> dueOccurrences = new TreeSet<DueOccurrence>();

        List<ScheduleEntry> schedules = configuredCommand.getSchedules();
        for (int scheduleIndex = 0; scheduleIndex < schedules.size(); scheduleIndex++) {
            collectDueInstants(configuredCommand, schedules.get(scheduleIndex), scheduleIndex, from, to, zone, dueOccurrences);
        }

        int dueCount = 0;
        for (DueOccurrence dueOccurrence : dueOccurrences) {
            if (executedOccurrences.putIfAbsent(dueOccurrence.dedupKey, Long.valueOf(dueOccurrence.epochSecond)) == null) {
                dueCount++;
            }
        }

        return dueCount;
    }

    private void collectDueInstants(
            ConfiguredCommand configuredCommand,
            ScheduleEntry schedule,
            int scheduleIndex,
            ZonedDateTime from,
            ZonedDateTime to,
            ZoneId zone,
            Set<DueOccurrence> dueOccurrences
    ) {
        LocalDate startDate = from.toLocalDate().minusDays(1);
        LocalDate endDate = to.toLocalDate().plusDays(1);

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            if (!matchesDay(schedule, date.getDayOfWeek())) {
                continue;
            }

            addMatchingInstants(configuredCommand, scheduleIndex, date, schedule, zone, from.toInstant(), to.toInstant(), dueOccurrences);
        }
    }

    private boolean matchesDay(ScheduleEntry schedule, DayOfWeek day) {
        return schedule.isDaily() || schedule.getDayOfWeek() == day;
    }

    private void addMatchingInstants(
            ConfiguredCommand configuredCommand,
            int scheduleIndex,
            LocalDate date,
            ScheduleEntry schedule,
            ZoneId zone,
            Instant from,
            Instant to,
            Set<DueOccurrence> dueOccurrences
    ) {
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
                return;
            }

            Instant candidate = transition.getDateTimeAfter().atZone(zone).toInstant();
            if (candidate.isAfter(from) && !candidate.isAfter(to)) {
                dueOccurrences.add(new DueOccurrence(
                        buildDailyOccurrenceKey(configuredCommand.getId(), scheduleIndex, date),
                        candidate.getEpochSecond()
                ));
            }
            return;
        }

        for (ZoneOffset offset : validOffsets) {
            Instant candidate = ZonedDateTime.ofLocal(localDateTime, zone, offset).toInstant();
            if (candidate.isAfter(from) && !candidate.isAfter(to)) {
                dueOccurrences.add(new DueOccurrence(
                        buildDailyOccurrenceKey(configuredCommand.getId(), scheduleIndex, date),
                        candidate.getEpochSecond()
                ));
            }
        }
    }

    private String buildDailyOccurrenceKey(String commandId, int scheduleIndex, LocalDate date) {
        return commandId + ":" + scheduleIndex + ":" + date.toString();
    }

    private void pruneExecutionMemory(Instant now) {
        long cutoff = now.minusSeconds(EXECUTION_MEMORY_SECONDS).getEpochSecond();
        Iterator<Map.Entry<String, Long>> iterator = executedOccurrences.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Long> entry = iterator.next();
            if (entry.getValue().longValue() < cutoff) {
                iterator.remove();
            }
        }
    }

    private static final class DueOccurrence implements Comparable<DueOccurrence> {
        private final String dedupKey;
        private final long epochSecond;

        private DueOccurrence(String dedupKey, long epochSecond) {
            this.dedupKey = dedupKey;
            this.epochSecond = epochSecond;
        }

        @Override
        public int compareTo(DueOccurrence other) {
            if (epochSecond < other.epochSecond) {
                return -1;
            }
            if (epochSecond > other.epochSecond) {
                return 1;
            }
            return dedupKey.compareTo(other.dedupKey);
        }
    }
}
