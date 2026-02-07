package com.siberanka.simplecommantimer;

import java.time.DayOfWeek;

public final class ScheduleEntry {
    private final DayOfWeek dayOfWeek;
    private final int hour;
    private final int minute;
    private final int second;

    public ScheduleEntry(DayOfWeek dayOfWeek, int hour, int minute, int second) {
        this.dayOfWeek = dayOfWeek;
        this.hour = hour;
        this.minute = minute;
        this.second = second;
    }

    public DayOfWeek getDayOfWeek() {
        return dayOfWeek;
    }

    public int getHour() {
        return hour;
    }

    public int getMinute() {
        return minute;
    }

    public int getSecond() {
        return second;
    }

    public boolean isDaily() {
        return dayOfWeek == null;
    }
}
