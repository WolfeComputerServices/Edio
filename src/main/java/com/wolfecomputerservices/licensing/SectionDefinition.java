/*
 * Copyright (C) 2020 -  Wolfe Computer Services
 * ALL RIGHTS RESERVED
 */
package com.wolfecomputerservices.licensing;

public class SectionDefinition {
    public int offset;
    public final int length;

    public SectionDefinition(int length) {
        this.length = length;
    }

    public void setOffset(int offset) { this.offset = offset; }
}
