package com.simplecommandtimer;

import java.text.Normalizer;
import java.time.DayOfWeek;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class ScheduleParser {
    private static final Map<String, DayOfWeek> DAY_LOOKUP = new HashMap<String, DayOfWeek>();

    static {
        DAY_LOOKUP.put("MONDAY", DayOfWeek.MONDAY);
        DAY_LOOKUP.put("TUESDAY", DayOfWeek.TUESDAY);
        DAY_LOOKUP.put("WEDNESDAY", DayOfWeek.WEDNESDAY);
        DAY_LOOKUP.put("THURSDAY", DayOfWeek.THURSDAY);
        DAY_LOOKUP.put("FRIDAY", DayOfWeek.FRIDAY);
        DAY_LOOKUP.put("SATURDAY", DayOfWeek.SATURDAY);
        DAY_LOOKUP.put("SUNDAY", DayOfWeek.SUNDAY);

        DAY_LOOKUP.put("LUNES", DayOfWeek.MONDAY);
        DAY_LOOKUP.put("MARTES", DayOfWeek.TUESDAY);
        DAY_LOOKUP.put("MIERCOLES", DayOfWeek.WEDNESDAY);
        DAY_LOOKUP.put("JUEVES", DayOfWeek.THURSDAY);
        DAY_LOOKUP.put("VIERNES", DayOfWeek.FRIDAY);
        DAY_LOOKUP.put("SABADO", DayOfWeek.SATURDAY);
        DAY_LOOKUP.put("DOMINGO", DayOfWeek.SUNDAY);
    }

    private ScheduleParser() {
    }

    public static ScheduleEntry parse(String value) {
        String[] parts = value.split(";");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid schedule format: " + value);
        }

        String dayPart = normalize(parts[0]);
        DayOfWeek day = null;
        if (!"DAILY".equals(dayPart) && !"DIARIO".equals(dayPart)) {
            day = DAY_LOOKUP.get(dayPart);
            if (day == null) {
                throw new IllegalArgumentException("Unknown day token: " + parts[0]);
            }
        }

        String[] timeParts = parts[1].trim().split(":");
        if (timeParts.length != 3) {
            throw new IllegalArgumentException("Invalid time format in schedule: " + value);
        }

        int hour = Integer.parseInt(timeParts[0]);
        int minute = Integer.parseInt(timeParts[1]);
        int second = Integer.parseInt(timeParts[2]);

        if (hour < 0 || hour > 23 || minute < 0 || minute > 59 || second < 0 || second > 59) {
            throw new IllegalArgumentException("Time out of range in schedule: " + value);
        }

        return new ScheduleEntry(day, hour, minute, second);
    }

    private static String normalize(String token) {
        String normalized = Normalizer.normalize(token.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT);
        return normalized;
    }
}
