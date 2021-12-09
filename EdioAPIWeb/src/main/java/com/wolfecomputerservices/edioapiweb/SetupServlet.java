package com.wolfecomputerservices.edioapiweb;

/*
 * Copyright (C) 2021 - Wolfe Computer Services
 * ALL RIGHTS RESERVED
 */

import com.wolfecomputerservices.edioapi.EdioAPI;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONObject;

/**
 *
 * @author Ed Wolfe
 */
@WebServlet(urlPatterns = {"/Setup"})
public class SetupServlet extends HttpServlet {

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if ("GET".equals(request.getMethod())) {
            getServletContext()
                    .getRequestDispatcher("/setup/form.html")
                    .forward(request, response);
        } else {
            final String euid = request.getParameter("element_1");
            final String eup  = request.getParameter("element_2");
            final int updays   = Integer.parseInt(request.getParameter("element_3"));
            final boolean overdues = request.getParameter("element_4_1").equals("1");
            final boolean school = request.getParameter("element_4_2").equals("1");
            final boolean upcoming = request.getParameter("element_4_3").equals("1");
            final boolean events = request.getParameter("element_4_4").equals("1");

            response.setContentType("application/json;charset=utf-8");
            ServletContext context = getServletContext();
            
            try ( PrintWriter out = response.getWriter()) {
                final Path pathConfig = Paths.get(context.getRealPath("/"), EdioAPIServlet.DATA_PATH, EdioAPIServlet.CONFIG_FILE);
                File jsonConfigFile = pathConfig.toFile();
                
                JSONObject json = new JSONObject("{"
                        + "\"edio\": {"
                            + "\"credentials\": {"
                                + String.format("\"user\": \"%s\",", euid)
                                + String.format("\"pass\": \"%s\"", eup)
                            + "}"
                        + "},"
                        + "\"output\": {"
                            + "\"children\": [],"
                            + String.format("\"overdue\": %s,", overdues ? "true" : "false")
                            + String.format("\"school\": %s,", school ? "true" : "false")
                            + String.format("\"upcoming\": %s,", upcoming ? "true" : "false")
                            + String.format("\"events\": %s,", events ? "true" : "false")
                            + "\"events_parms\" : {\"date\": \"\" },"
                            + "\"upcoming_parms\": {"
                                + String.format("\"days\": %d", updays)
                            + "}"
                        + "}"
                    + "}");
                
                if (!jsonConfigFile.exists()) {
                    jsonConfigFile.getParentFile().mkdirs();
                    jsonConfigFile.createNewFile();
                }
                    
                try (FileOutputStream fos = new FileOutputStream(jsonConfigFile)) {
                    try (OutputStreamWriter os = new OutputStreamWriter(fos)) {
                        json.write(os);
                    }
                }
                
                RequestDispatcher dispatcher = getServletContext()
                    .getRequestDispatcher("/Edio?refresh");
                dispatcher.forward(request, response);
            }
        }
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

}
