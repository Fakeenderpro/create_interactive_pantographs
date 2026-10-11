package com.pantographinteract;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PantographSchedule {

    public static final String RAISED_KEY = "PantographScheduleRaised";

    private static final Map<UUID, Map<Integer, Boolean>> STATES = new ConcurrentHashMap<>();

    private PantographSchedule() {}

    public static void set(UUID trainId, int mode, boolean raised) {
        if (mode != PantographDirection.SCHEDULE_STANDARD && mode != PantographDirection.SCHEDULE_INVERTED)
            return;
        STATES.computeIfAbsent(trainId, id -> new ConcurrentHashMap<>()).put(mode, raised);
    }

    public static Boolean get(UUID trainId, int mode) {
        Map<Integer, Boolean> states = STATES.get(trainId);
        return states == null ? null : states.get(mode);
    }
}
