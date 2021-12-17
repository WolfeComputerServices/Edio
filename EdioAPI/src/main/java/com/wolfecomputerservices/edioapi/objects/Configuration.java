/*
 * Copyright (C) 2021 - Wolfe Computer Services
 * ALL RIGHTS RESERVED
 */
package com.wolfecomputerservices.edioapi.objects;

import java.time.LocalDate;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 *
 * @author Wolfe Computer Services (Ed Wolfe}
 *
 *
 */
@NonNullByDefault
public class Configuration {

    public class Output {

        public boolean school;
        public boolean overdue;
        public boolean upcoming;
        public boolean events;
        public int upcoming_days = 14;

    }
    public Output output = new Output();
    public Edio edio = new Edio();


    public class Edio {

        public Credentials credentials = new Credentials();

        public class Credentials {

            public String user;
            public String pass;
        }
    }
}
