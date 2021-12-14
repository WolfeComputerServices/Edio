/*
 * Copyright (C) 2021 - Wolfe Computer Services
 * ALL RIGHTS RESERVED
 */

package com.wolfecomputerservices.ediocli;

import com.wolfecomputerservices.edioapi.Edio;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author  Wolfe Computer Services (Ed Wolfe}
 *
**/
public class Cli {
    public static void main(String[] args) {
        if (args.length == 0)
            System.out.println("A configuration file must be specified.");
        else {
            try (Edio edio = new Edio(args)) {
                System.out.println(edio.executor());
            } catch (IOException ex) {
                Logger.getLogger(Cli.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
