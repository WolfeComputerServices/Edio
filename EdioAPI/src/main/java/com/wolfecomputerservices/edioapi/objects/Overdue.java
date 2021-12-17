/*
 * Copyright (C) 2021 - Wolfe Computer Services
 * ALL RIGHTS RESERVED
 */
package com.wolfecomputerservices.edioapi.objects;

import com.google.gson.annotations.Expose;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 *
 * @author Wolfe Computer Services (Ed Wolfe}
 *
 *
 */
@NonNullByDefault
public final class Overdue {

    @Expose(serialize = true)
    public LocalDate dueDate;
    @Expose(serialize = true)
    public String course;
    @Expose(serialize = true)
    public String assignment;

    public Overdue(String dueDate, String course, String assignment) {
        this.dueDate = LocalDate.parse(dueDate, DateTimeFormatter.ISO_DATE_TIME);
        
        this.course = course;
        this.assignment = assignment;
    }
}
