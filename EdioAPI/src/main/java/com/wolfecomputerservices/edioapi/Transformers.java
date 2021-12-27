/*
 * Copyright (C) 2021 - Wolfe Computer Services
 * ALL RIGHTS RESERVED
 */

package com.wolfecomputerservices.edioapi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import com.wolfecomputerservices.edioapi.objects.Event;
import com.wolfecomputerservices.edioapi.objects.Upcoming;
import java.util.Arrays;

/**
 *
 * @author Wolfe Computer Services - Initial contribution
 *
 **/
@NonNullByDefault
@SuppressWarnings("unchecked")
public class Transformers {
    /**
     * Add all from items to to
     * 
     * @param from source List
     * @param to destination list
     */
    public static void addAll(List<Event> from, List<Upcoming> to) {
        to.addAll(from.stream()
                .map((event) -> new Upcoming(event.dateStart.toLocalDate(), event.eventName, event.eventDescription))
                .collect(Collectors.toList()));
    }

    public static Map<String, Object> toMapValueMap(Object object) {
        return (Map<String, Object>) object;
    }

    public static int getMapValueAsInt(Map<String, Object> map, String valueName) {
        @Nullable
        Object r = getMapValueAs(map, valueName);
        if (r instanceof Double) {
            String a = "" + r;
            if (a.contains(".") && a.endsWith(".0"))
                r = Integer.parseInt(a.split("\\.")[0]);
        }
        if (r == null || !(r instanceof Integer))
            return Integer.MIN_VALUE;

        return (int) r;
    }

    public static String getMapValueAsString(Map<String, Object> map, String valueName) {
        @Nullable
        Object r = getMapValueAs(map, valueName);
        return r instanceof String ? (String) r : "";
    }

    public static ArrayList<Object> getMapValueAsArrayList(Map<String, Object> map, String valueName) {
        @Nullable
        Object r = getMapValueAs(map, valueName);
        return r instanceof ArrayList ? (ArrayList<Object>) r : new ArrayList<>(0);
    }

    public static Map<String, Object> getMapValueAsMap(Map<String, Object> map, String valueName) {
        @Nullable
        Object r = getMapValueAs(map, valueName);
        return r instanceof Map ? (Map<String, Object>) r : new HashMap<>(0);
    }

    public static boolean getMapValueAsBoolean(Map<String, Object> map, String valueName) {
        Object r = getMapValueAs(map, valueName);
        if (r == null || !(r instanceof Boolean))
            return false;

        return (boolean) r;
    }

    public static Collection<Object> getMapValueAsValues(Map<String, Object> map, String valueName) {
        return getMapValueAsMap(map, valueName).values();
    }

    private static @Nullable Object getMapValueAs(Map<String, Object> map, String valueName) {
        return map.get(valueName);
    }

    public static int[] getStudentIds(Map<String, Object> map) {
        return Transformers
                .getMapValueAsArrayList(getMapValueAsMap(map, "resultObject"), "primaryUserRoleRelationships")
                .stream().map(s -> (Map<String, Object>) s).map(s -> getMapValueAsMap(s, "secondaryUser"))
                .mapToInt(s -> Transformers.getMapValueAsInt(s, "id"))
                .toArray();
/*        return getMapValueAsArrayList(getMapValueAsMap(map, "resultObject"), "primaryUserRoleRelationships").stream()
                .map(s -> (Map<String, Object>) s).map(s -> Transformers.getMapValueAsMap(s, "secondaryUser"))
                .mapToInt(s -> getMapValueAsInt(toMapValueMap(s), "id")).toArray();
*/
    }
    
    public static String toCamelCase(String value) {
        return Arrays.stream(value.split("[\\W_]+"))
                .map(w->w.isEmpty() ? w : Character.toUpperCase(w.charAt(0)) + w.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }
}
