/*
 * Copyright (C) 2021 - Wolfe Computer Services
 * ALL RIGHTS RESERVED
 */

package com.wolfecomputerservices.edioapi.objects;

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;

import com.google.gson.annotations.Expose;
import com.wolfecomputerservices.edioapi.Transformers;

/**
 *
 * @author Wolfe Computer Services - Initial contribution
 *
 **/
@NonNullByDefault
public class Student {
    @Expose(serialize = true)
    public int id = Integer.MIN_VALUE;
    @Expose(serialize = true)
    public String name = "";
    @Expose(serialize = true)
    public Overdue[] overdues = new Overdue[0];
    @Expose(serialize = true)
    public Event[] events = new Event[0];
    @Expose(serialize = true)
    public Upcoming[] upcomings = new Upcoming[0];

    public Student() {
    }

    public Student(Student s) {
        this.id = s.id;
        this.name = s.name;
        this.overdues = s.overdues;
        this.events = s.events;
        this.upcomings = s.upcomings;
    }

    public Student(int id, String name) {
        this.id = id;
        this.name = Transformers.toCamelCase(name);
        this.events = new Event[0];
        this.overdues = new Overdue[0];
        this.upcomings = new Upcoming[0];
    }

    public Student(int id, String name, List<Overdue> overdues, List<Event> events, List<Upcoming> upcomings) {
        this.id = id;
        this.name = Transformers.toCamelCase(name);
        this.events = events.toArray(new Event[0]);
        this.overdues = overdues.toArray(new Overdue[0]);
        this.upcomings = upcomings.toArray(new Upcoming[0]);
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Overdue[] getOverdues() {
        return overdues;
    }

    public Event[] getEvents() {
        return events;
    }

    public Upcoming[] getUpcomings() {
        return upcomings;
    }
}
