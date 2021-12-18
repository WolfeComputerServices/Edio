/*
 * Copyright (C) 2021 - Wolfe Computer Services
 * ALL RIGHTS RESERVED
 */

package com.wolfecomputerservices.edioapi;

import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;
import com.wolfecomputerservices.edioapi.objects.ExecutorOutput;

/**
 *
 * @author Wolfe Computer Services - Initial contribution
 *
 **/
@NonNullByDefault
public class EdioGson {
    public static final Type LIST_TYPE = new TypeToken<Map<String, Object>>() {
    }.getType();

    public static Map<String, Object> toMap(Object json) {
        @Nullable
        Map<String, Object> map = new Gson().fromJson((String) json, LIST_TYPE);
        return map == null ? new HashMap<String, Object>(0) : fix(map);
    }

    public static String toJson(ExecutorOutput output) {
        return new GsonBuilder().registerTypeAdapter(LocalDate.class,
                (JsonSerializer<LocalDate>) (LocalDate src, Type typeofSrc, JsonSerializationContext context) -> {
                    JsonObject jsonLocalDate = new JsonObject();

                    jsonLocalDate.addProperty("iso", src.format(DateTimeFormatter.ISO_DATE));
                    jsonLocalDate.addProperty("year", src.getYear());
                    jsonLocalDate.addProperty("month", src.getMonthValue());
                    jsonLocalDate.addProperty("day", src.getDayOfMonth());

                    return jsonLocalDate;
                }).registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>) (LocalDateTime src,
                        Type typeOfSrc, JsonSerializationContext context) -> {
                    JsonObject jsonLocalDateTime = new JsonObject();

                    jsonLocalDateTime.addProperty("iso", src.format(DateTimeFormatter.ISO_DATE_TIME));
                    jsonLocalDateTime.addProperty("year", src.getYear());
                    jsonLocalDateTime.addProperty("month", src.getMonthValue());
                    jsonLocalDateTime.addProperty("day", src.getDayOfMonth());
                    jsonLocalDateTime.addProperty("hour", src.getHour());
                    jsonLocalDateTime.addProperty("minute", src.getMinute());
                    jsonLocalDateTime.addProperty("second", src.getSecond());

                    return jsonLocalDateTime;
                }).create().toJson(output);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> fix(Map<String, Object> map) {
        Map<String, Object> values = new HashMap<>();

        for (String key : map.keySet()) {
            String a = null;
            try {
                Object value = map.get(key);
                if (value instanceof Double || value instanceof Integer)
                    a = "" + value;
                else if (value instanceof Map) {
                    values.put(key, fix((Map<String, Object>) value));
                    continue;
                } else if (value instanceof String)
                    a = (String) value;

                if (a != null && a.contains(".") && a.endsWith(".0"))
                    values.put(key, Integer.parseInt(a.split("\\.")[0]));
                else {
                    if (value == null)
                        value = "null";
                    values.put(key, value);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        return values;
    }
}
