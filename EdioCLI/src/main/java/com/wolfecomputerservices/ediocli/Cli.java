/*
 * Copyright (C) 2021 - Wolfe Computer Services
 * ALL RIGHTS RESERVED
 */

package com.wolfecomputerservices.ediocli;

import com.wolfecomputerservices.edioapi.Edio;

/**
 *
 * @author  Wolfe Computer Services (Ed Wolfe}
 *
**/
public class Cli {
    public static void main(String[] args) {
        if (args.length == 0)
            System.out.println("A configuration file must be specified.");
        else
            new Edio().cliRun(args);
    }
}
