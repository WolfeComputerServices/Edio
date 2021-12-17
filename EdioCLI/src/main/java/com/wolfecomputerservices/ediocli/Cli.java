/*
 * Copyright (C) 2021 - Wolfe Computer Services
 * ALL RIGHTS RESERVED
 */

package com.wolfecomputerservices.ediocli;

import com.google.gson.Gson;
import com.wolfecomputerservices.edioapi.Edio;
import com.wolfecomputerservices.edioapi.objects.ExecutorOutput;
import java.io.IOException;
import java.nio.file.Paths;
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
            Gson gson = new Gson();
            try (Edio edio = new Edio(Paths.get(args[0]))) {
                ExecutorOutput output = edio.executor();
                String str = gson.toJson(output);
                System.out.println(str);
            } catch (IOException ex) {
                Logger.getLogger(Cli.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
