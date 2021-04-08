/*
 * Copyright (C) 2020 -  Wolfe Computer Services
 * ALL RIGHTS RESERVED
 */
package com.wolfecomputerservices.licensing;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

public class LicenseCode {
      // do license check here
      /* All numbers are in packed decimal
      Format: 11223344-0000-0000-0000-00000000000000
      Where: 11 offset to type
             22 offset to users
             33 offset to expiration date
             44 offset to flags
      All offsets are in the range of 7-32
        Type: Either temp (expires) or permanent (never expires) (4 bits)
        Users: number of users (max number that can be added)    (16 bits)
        Expiration date: Either dddYY where ddd is day of year   (20 bits)
                         and YY is year or random number for permanent
        Flags: Indicators (currently not used)                   (16 bits)
        All unused bits are random
      */
    protected int sizeLicense = 36;
    protected final int[] offDashes = { 8, 13, 18, 23 };
    protected final int EXPIRATION_DATE = 0;
    protected final int FLAGS = 1;
    protected final int MAX_USERS = 2;
    protected final int TYPE = 3;
    protected final List<SectionDefinition> sections = Arrays.asList(
            new SectionDefinition(14),      // expiration date
            new SectionDefinition(8),       // flags
            new SectionDefinition(4),       // max users
            new SectionDefinition(2)        // type
        );
    protected Calendar toUTCCalendar(ZonedDateTime date) {
        Calendar cal = GregorianCalendar.from(date);
        Calendar out = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        out.setTime(cal.getTime());
        return out;
    }
}
