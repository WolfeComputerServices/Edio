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

import com.google.gson.internal.LinkedTreeMap;
import com.wolfecomputerservices.edioapi.objects.Event;
import com.wolfecomputerservices.edioapi.objects.Upcoming;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 *
 * @author Wolfe Computer Services - Initial contribution
 *
 **/
@NonNullByDefault
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
        return object == null ? new HashMap<>() : (Map<String, Object>) object;
    }

    public static int getMapValueAsInt(Map<String, Object> map, String valueName) {
        Object r = getMapValueAs(map, valueName);
        if (r instanceof Double) {
            String a = "" + r;
            if (a != null && a.contains(".") && a.endsWith(".0"))
                r = Integer.parseInt(a.split("\\.")[0]);
        }
        return r instanceof Integer ? (int) r : Integer.MIN_VALUE;
    }

    public static String getMapValueAsString(Map<String, Object> map, String valueName) {
        Object r = getMapValueAs(map, valueName);
        return r instanceof String ? (String) r : "";
    }

    public static ArrayList<Object> getMapValueAsArray(Map<String, Object> map, String valueName) {
        Object r = getMapValueAs(map, valueName);
        return r instanceof ArrayList ? (ArrayList<Object>) r : new ArrayList<>();
    }

    public static ArrayList<Object> getMapValueAsArrayList(Map<String, Object> map, String valueName) {
        Object r = getMapValueAs(map, valueName);
        return r instanceof ArrayList ? (ArrayList<Object>) r : new ArrayList<>(0);
    }

    public static Map<String, Object> getMapValueAsMap(Map<String, Object> map, String valueName) {
        Object r = getMapValueAs(map, valueName);
        return r instanceof Map ? (Map<String, Object>) r : new HashMap<>(0);
    }

    public static boolean getMapValueAsBoolean(Map<String, Object> map, String valueName) {
        Object r = getMapValueAs(map, valueName);
        return r instanceof Boolean ? (boolean) r : false;
    }

    public static Collection<Object> getMapValueAsValues(Map<String, Object> map, String valueName) {
        Map<String, Object> r = getMapValueAsMap(map, valueName);
        return r != null ? r.values() : new ArrayList<>(0);
    }

    private static Object getMapValueAs(Map<String, Object> map, String valueName) {
        return map.get(valueName);
    }

    public static int[] getStudentIds(Map<String, Object> map) {
        /*
         * Transformers.getMapValueAsArrayList(Transformers.getMapValueAsMap(
         * edioAPI.getStudents(), "resultObject"), "primaryUserRoleRelationships")
         * .stream()
         * .map(s->(LinkedTreeMap<String, Object>)s)
         * .map(s->Transformers.getMapValueAsMap(s, "secondaryUser"))
         * .map(s->new Student(
         * Transformers.getMapValueAsInt(s, "id"),
         * Transformers.getMapValueAsString(s, "firstName")))
         * .collect(Collectors.toList());
         */

        return getMapValueAsArrayList(getMapValueAsMap(map, "resultObject"), "primaryUserRoleRelationships").stream()
                .map(s -> (LinkedTreeMap<String, Object>) s).map(s -> Transformers.getMapValueAsMap(s, "secondaryUser"))
                .mapToInt(s -> getMapValueAsInt(toMapValueMap(s), "id")).toArray();
    }
}
