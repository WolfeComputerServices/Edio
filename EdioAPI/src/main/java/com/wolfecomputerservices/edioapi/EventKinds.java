/*
 * Copyright (C) 2021 - Wolfe Computer Services
 * ALL RIGHTS RESERVED
 */
package com.wolfecomputerservices.edioapi;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Event kinds as returned by Edio
 *
 * @author Wolfe Computer Services - Initial contribution
 *
 */
@NonNullByDefault
/* internal */ enum EventKinds {
    EK_0(0),
    EK_1(1),
    EK_2(2),
    EK_3(3),
    EK_4(4),
    EK_5(5),
    EK_6(6),
    EK_7(7),
    EK_8(7),
    EK_9(9),
    // Known types
    EK_CUSTOM(EK_3),
    EK_FIELD_TRIP(EK_4),
    EK_HOLLIDAY(EK_9);

    private final int value;

    EventKinds(int value) {
        this.value = value;
    }

    EventKinds(EventKinds value) {
        this.value = value.asInt();
    }

    public int asInt() {
        return value;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

    public static String delimited(EventKinds[] kinds, String delimiter) {
        return Arrays.stream(kinds).map(EventKinds::toString).collect(Collectors.joining(","));
    }
}
