/*
 * Copyright (C) 2021 - Wolfe Computer Services
 * ALL RIGHTS RESERVED
 */
package com.wolfecomputerservices.edioapi.objects;

import com.google.gson.annotations.Expose;
import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 *
 * @author Wolfe Computer Services (Ed Wolfe}
 *
 *
 */
@NonNullByDefault
public class Configuration {

    public class Output {

        @Expose(serialize = true)
        public boolean school;
        @Expose(serialize = true)
        public boolean overdue;
        @Expose(serialize = true)
        public boolean upcoming;
        @Expose(serialize = true)
        public boolean events;
        @Expose(serialize = true)
        public int upcoming_days = 14;

    }
    @Expose(serialize = true)
    public Output output = new Output();
    @Expose(serialize = true)
    public Edio edio = new Edio();


    public class Edio {

        @Expose(serialize = true)
        public Credentials credentials = new Credentials();

        public class Credentials {

            @Expose(serialize = true)
            public String user;
            @Expose(serialize = true)
            public String pass;
        }
    }
}
