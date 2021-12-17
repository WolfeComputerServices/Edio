package com.wolfecomputerservices.edioapiweb;

/*
 * Copyright (C) 2021 - Wolfe Computer Services
 * ALL RIGHTS RESERVED
 */
import com.google.gson.Gson;
import com.wolfecomputerservices.edioapi.objects.Configuration;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;
import java.util.logging.Level;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 *
 * @author Ed Wolfe
 */
@NonNullByDefault
@WebServlet(name = "Edio Setup", urlPatterns = {"/edio.cfg"})
public class EdioSetupServlet extends HttpServlet {

    private Logger logger = Logger.getLogger(EdioSetupServlet.class.getName());
    private final Gson gson = new Gson();
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
            try {
                Configuration config = new Configuration();
                config.edio.credentials.user = request.getParameter("element_1");
                config.edio.credentials.pass = request.getParameter("element_2");
                config.output.upcoming_days  = Integer.parseInt(request.getParameter("element_3").trim());
                config.output.overdue = request.getParameter("element_4_1").equals("1");
                config.output.school  = request.getParameter("element_4_2").equals("1");
                config.output.upcoming= request.getParameter("element_4_3").equals("1");
                config.output.events  = request.getParameter("element_4_4").equals("1");

                response.setContentType("application/json;charset=utf-8");
                ServletContext context = getServletContext();

                try ( PrintWriter out = response.getWriter()) {
                    final Path pathConfig = Paths.get(context.getRealPath("/"), EdioAPIServlet.DATA_PATH, EdioAPIServlet.CONFIG_FILE);
                    File jsonConfigFile = pathConfig.toFile();

                    if (!jsonConfigFile.exists()) {
                        jsonConfigFile.getParentFile().mkdirs();
                        jsonConfigFile.createNewFile();
                    }

                    try ( FileOutputStream fos = new FileOutputStream(jsonConfigFile)) {
                        try ( OutputStreamWriter os = new OutputStreamWriter(fos)) {
                            os.write(gson.toJson(config));
                        }
                    }

                    response.sendRedirect(context.getContextPath() + "/edio.do?refresh");
                }
            } catch (Exception ex) {
                logger.log(Level.WARNING, ex.getMessage(), ex);
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
