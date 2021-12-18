/*
 * Copyright (C) 2021 - Wolfe Computer Services
 * ALL RIGHTS RESERVED
 */

package com.wolfecomputerservices.edioapi.objects;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.eclipse.jdt.annotation.NonNullByDefault;

import com.google.gson.annotations.Expose;

/**
 * Represents upcoming item in Edio
 * 
 * @author Wolfe Computer Services - Initial contribution
 *
 **/
@NonNullByDefault
public class Upcoming {
    @Expose(serialize = true)
    public LocalDate date;
    @Expose(serialize = true)
    public String course;
    @Expose(serialize = true)
    public String topic;

    public Upcoming(final LocalDate date, final String course, final String topic) {
        this.date = date;
        this.course = course;
        this.topic = topic;
    }

    public Upcoming(final String date, final String course, final String topic) {
        this.date = LocalDate.parse(date, DateTimeFormatter.ISO_DATE_TIME);
        this.course = course;
        this.topic = topic;
    }
}
