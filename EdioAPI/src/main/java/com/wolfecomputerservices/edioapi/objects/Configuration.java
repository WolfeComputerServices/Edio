/*
 * Copyright (C) 2021 - Wolfe Computer Services
 * ALL RIGHTS RESERVED
 */
package com.wolfecomputerservices.edioapi.objects;

import org.eclipse.jdt.annotation.NonNullByDefault;

import com.google.gson.annotations.Expose;

/**
 *
 * @author Wolfe Computer Services - Initial contribution
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

    public static class Edio {

        @Expose(serialize = true)
        public Credentials credentials = new Credentials();

        public static class Credentials {

            @Expose(serialize = true)
            public String user = "";
            @Expose(serialize = true)
            public String pass = "";

            public Credentials() {
            }

            public Credentials(String user, String pass) {
                this.user = user;
                this.pass = pass;
            }
        }
    }
}