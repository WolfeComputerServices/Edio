/*
 * Copyright (C) 2021 - Wolfe Computer Services
 * ALL RIGHTS RESERVED
 */

package com.wolfecomputerservices.edioapi.objects;

import com.wolfecomputerservices.edioapi.objects.Event;
import com.wolfecomputerservices.edioapi.objects.Student;
import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 *
 * @author  Wolfe Computer Services (Ed Wolfe}
 *
**/
@NonNullByDefault
public class ExecutorOutput {
    public boolean setup_required;
    public boolean school;
    public Student[] students = new Student[0];
    public Event[] events = new Event[0];
    public String[] errors = new String[0];
}
