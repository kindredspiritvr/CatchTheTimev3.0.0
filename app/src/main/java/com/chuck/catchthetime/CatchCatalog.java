package com.chuck.catchthetime;

import android.content.Context;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public final class CatchCatalog {
    public static final DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HH:mm");
    private static final Map<LocalTime,String> DEFAULTS = new LinkedHashMap<>();

    static {
        add("repeats", 0,0, 1,11, 2,22, 3,33, 4,44, 5,55, 10,10, 11,11, 12,12, 13,13, 14,14, 15,15, 16,16, 17,17, 18,18, 19,19, 20,20, 21,21, 22,22, 23,23);
        add("ascending", 1,23, 2,34, 3,45, 4,56, 12,34, 23,45);
        add("descending", 3,21, 4,32, 5,43, 6,54);
        add("mirrors", 10,1, 12,21, 13,31, 14,41, 15,51, 20,2, 21,12, 23,32);
    }

    private CatchCatalog() {}

    private static void add(String pack, int... hm) {
        for (int i = 0; i < hm.length; i += 2) DEFAULTS.put(LocalTime.of(hm[i], hm[i+1]), pack);
    }

    public static List<LocalTime> activeTimes(Context c) {
        String mode = SettingsStore.mode(c);
        Set<LocalTime> set = new TreeSet<>();
        if (!"custom".equals(mode)) {
            for (Map.Entry<LocalTime,String> e : DEFAULTS.entrySet()) {
                if (SettingsStore.packEnabled(c, e.getValue())) set.add(e.getKey());
            }
        }
        if (!"defaults".equals(mode)) {
            for (String s : SettingsStore.customTimes(c)) {
                try { set.add(LocalTime.parse(s, HHMM)); } catch (Exception ignored) {}
            }
        }
        return new ArrayList<>(set);
    }

    public static List<LocalTime> defaultsForPack(String pack) {
        List<LocalTime> result = new ArrayList<>();
        for (Map.Entry<LocalTime,String> e : DEFAULTS.entrySet()) if (e.getValue().equals(pack)) result.add(e.getKey());
        Collections.sort(result);
        return result;
    }

    public static LocalDateTime nextAfter(Context c, LocalDateTime now) {
        List<LocalTime> times = activeTimes(c);
        if (times.isEmpty()) return null;
        LocalDate today = now.toLocalDate();
        for (LocalTime t : times) {
            LocalDateTime candidate = LocalDateTime.of(today, t);
            if (candidate.isAfter(now)) return candidate;
        }
        return LocalDateTime.of(today.plusDays(1), times.get(0));
    }

    public static int currentIndex(Context c, LocalDateTime now) {
        LocalTime minute = now.toLocalTime().withSecond(0).withNano(0);
        List<LocalTime> times = activeTimes(c);
        for (int i = 0; i < times.size(); i++) if (times.get(i).equals(minute)) return i;
        return -1;
    }

    public static int remainingToday(Context c, LocalDateTime now) {
        LocalTime minute = now.toLocalTime().withSecond(0).withNano(0);
        int n = 0;
        for (LocalTime t : activeTimes(c)) if (!t.isBefore(minute)) n++;
        return n;
    }
}
