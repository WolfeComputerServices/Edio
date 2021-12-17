/*
 * Copyright (C) 2021 - Wolfe Computer Services
 * ALL RIGHTS RESERVED
 */

package com.wolfecomputerservices.edioapi.objects;

import org.eclipse.jdt.annotation.NonNullByDefault;

import com.google.gson.annotations.Expose;
import com.wolfecomputerservices.edioapi.EdioGson;

/**
 *
 * @author Wolfe Computer Services - Initial contribution
 *
 **/
@NonNullByDefault
public class ExecutorOutput {
    @Expose(serialize = true)
    public boolean setup_required;
    @Expose(serialize = true)
    public boolean school;
    @Expose(serialize = true)
    public Student[] students = new Student[0];
    @Expose(serialize = true)
    public Event[] events = new Event[0];
    @Expose(serialize = true)
    public String[] errors = new String[0];

    public String toJson() {
        return EdioGson.toJson(this);
    }
}
