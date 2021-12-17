/*
 * Copyright (C) 2021 - Wolfe Computer Services
 * ALL RIGHTS RESERVED
 */

package com.wolfecomputerservices.edioapi.objects;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Represents upcoming item in Edio
 * 
 * @author  Wolfe Computer Services (Ed Wolfe}
 *
**/
public class Upcoming {
        public final LocalDate date;
        public final String course;
        public final String topic;

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
