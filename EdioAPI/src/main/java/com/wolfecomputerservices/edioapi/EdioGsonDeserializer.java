/*
 * Copyright (C) 2021 - Wolfe Computer Services
 * ALL RIGHTS RESERVED
 */

package com.wolfecomputerservices.edioapi;

import com.google.gson.Gson;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author  Wolfe Computer Services (Ed Wolfe}
 *
**/
public class EdioGsonDeserializer {
    
    public static Map<String, Object> toMap(Object json) {
        return fix(new Gson().fromJson((String)json, Map.class));
    }
    
    private static Map<String, Object> fix(Map<String,Object> map) {
        Map<String,Object> values = new HashMap<>();

        for (String key : map.keySet()) {
            String a = null;
            try {
                Object value = map.get(key);
                if (value instanceof Double || value instanceof Integer)
                    a = "" + value;
                else if (value instanceof Map) {
                    values.put(key, fix((Map<String, Object>)value));
                    continue;
                }
                else if (value instanceof String)
                    a = (String) value;
                else
                    a = null;
                
                if (a != null && a.contains(".") && a.endsWith(".0"))
                    values.put(key, Integer.parseInt(a.split("\\.")[0]));
                else
                    values.put(key, map.get(key));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        
        return values;
    }
}
