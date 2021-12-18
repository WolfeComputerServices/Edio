/*
 * Copyright (C) 2021 - Wolfe Computer Services
 * ALL RIGHTS RESERVED
 */
package com.wolfecomputerservices.edioapi.objects;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.eclipse.jdt.annotation.NonNullByDefault;

import com.google.gson.annotations.Expose;

/**
 * Represents an event from edio
 *
 * @author Wolfe Computer Services - Initial contribution
 *
 *
 */
@NonNullByDefault
public class Event {
    @Expose(serialize = true)
    public int id;
    @Expose(serialize = true)
    public LocalDate dateCreated;
    @Expose(serialize = true)
    public LocalDateTime dateStart;
    @Expose(serialize = true)
    public LocalDateTime dateEnd;
    @Expose(serialize = true)
    public String eventName;
    @Expose(serialize = true)
    public String eventDescription;

    @Expose(serialize = false)
    private final String ICAL_FORMAT = "yyyyMMdd HHmmss";

    public Event(final int Id, final String CreatedDate, final String StartDate, final String EndDate,
            final String EventName, final String EventDescription) {
        id = Id;
        dateCreated = LocalDate.parse(CreatedDate, DateTimeFormatter.ISO_DATE_TIME);
        dateStart = LocalDateTime.parse(StartDate, DateTimeFormatter.ISO_DATE_TIME);
        ;
        dateEnd = LocalDateTime.parse(EndDate, DateTimeFormatter.ISO_DATE_TIME);
        ;
        eventName = EventName;
        eventDescription = EventDescription;
    }

    public String formatDateAs(final String date, final String format) {
        return ZonedDateTime.parse(date).format(DateTimeFormatter.ofPattern(format));
    }

    public String formatDateAsiCal(final String date) {
        return formatDateAs(date, ICAL_FORMAT).replace(" ", "T");
        // + "Z";
    }

    public String formatDateAsISO8601(final String date) {
        return ZonedDateTime.parse(date).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).replace(" ", "T");
        // +"Z";
    }

    public String formatDateAs(final String date, final DateTimeFormatter dtfFormat) {
        return ZonedDateTime.parse(date).format(dtfFormat);
    }
}
