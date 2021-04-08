/*
 * Copyright (C) 2020 -  Wolfe Computer Services
 * ALL RIGHTS RESERVED
 */
package com.wolfecomputerservices.licensing;

import com.wolfecomputerservices.licensing.LicenseData.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public class LicenseDecode extends LicenseCode {
    public Object[] decode(String encLicense) {
        if (encLicense.length() > 8) {
            int offsetType  = getSubInt(encLicense,    TYPE * 2,            2);
            int offsetUser  = getSubInt(encLicense,    MAX_USERS * 2,       2);
            int offsetDate  = getSubInt(encLicense,    EXPIRATION_DATE * 2, 2);
            int offsetFlags = getSubInt(encLicense,    FLAGS * 2,           2);
            Integer[] offsets = { offsetType, offsetUser, offsetDate, offsetFlags}; 


            int max = Collections.max(Arrays.asList(offsets));
            int lenRequired = max;
            if (max == offsetType)
                lenRequired += sections.get(TYPE).length;
            else if (max == offsetUser)
                lenRequired += sections.get(MAX_USERS).length;
            else if (max == offsetDate)
                lenRequired += sections.get(EXPIRATION_DATE).length;
            else if (max == offsetFlags)
                lenRequired += sections.get(FLAGS).length;

            if (encLicense.length() == lenRequired) {
                Calendar expirationDate = null;
                long maxUsers = Long.MAX_VALUE;
                int flags;

                Type type = LicenseData.Type.valueOf(
                        getSubInt(encLicense, offsetType, sections.get(TYPE).length));

                switch (type) {
                    case PerminateUnlimited:
                        expirationDate = null;
                        break;
                    case PerminateLimited:
                        maxUsers = getSubInt(encLicense, offsetUser,
                               sections.get(MAX_USERS).length);
                        expirationDate = null;
                        break;
                    case Expiration:
                        expirationDate = getSubCalendar(encLicense, offsetDate,
                               sections.get(EXPIRATION_DATE).length);
                       break;
                }

                flags = getSubInt(encLicense, offsetFlags, 
                        sections.get(FLAGS).length);

                if (flags >= 32768 /*0x8000*/)
                    flags = 0;

                ArrayList<Object> data = new ArrayList<>();
                data.add(type);
                data.add(maxUsers);
                data.add(expirationDate);
                data.add(flags);

                Object[] a = new Object[data.size()];
                return data.toArray(a);
            }
        }
        return null;
    }
    private int[] decodeDoyYear(int doyYear) {
        int[] r = { doyYear / 10000, (doyYear-((doyYear / 10000) * 10000) - 50) / 2 };
        
        return r;
    }
    private Calendar getSubCalendar(String string, int start, int length) {
        int[] doyYear = decodeDoyYear(getSubInt(string, start, length));
        
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        cal.set(doyYear[1], 1, 1);
        cal.add(Calendar.DATE, doyYear[0] - 1);
        
        return cal;
    }
    private int getSubInt(String string, int start, int length) {
        return Integer.parseInt(getSubString(string, start, length));
    }   
    private String getSubString(String string, int start, int length) {
        return string.substring(start, start+length);
    }
}
