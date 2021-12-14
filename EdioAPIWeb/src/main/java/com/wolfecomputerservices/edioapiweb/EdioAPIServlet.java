/*
 * Copyright (C) 2021 - Wolfe Computer Services
 * ALL RIGHTS RESERVED
 */
package com.wolfecomputerservices.edioapiweb;

import com.wolfecomputerservices.edioapi.Edio;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidParameterException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author Ed Wolfe
 */
@WebServlet(name = "Edio", urlPatterns = {"/edio.do"})
public class EdioAPIServlet extends HttpServlet {

    protected static final String CONFIG_FILE = "edioapi-config.json";
    protected static final String DATA_PATH = "WEB-INF";

    private boolean setupNeeded = false;

    private JSONObject jsonConfig;
    private Edio edio;

    private Logger logger = Logger.getLogger(EdioAPIServlet.class.getName());

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        ServletContext context = config.getServletContext();
        loadConfig(context);
    }

    @Override
    public void destroy() {
        if (edio != null) {
            edio.disconnect();
        }
    }

    private void loadConfig(ServletContext context) {
        final Path pathConfig = Paths.get(context.getRealPath("/"), DATA_PATH, CONFIG_FILE);
        File jsonConfigFile = pathConfig.toFile();

        if (jsonConfigFile.exists()) {
            try {
                FileInputStream jsonConfigStream = new FileInputStream(jsonConfigFile);
                InputStreamReader isr = new InputStreamReader(jsonConfigStream);
                BufferedReader reader = new BufferedReader(isr);

                StringBuilder sb = new StringBuilder();
                String text;
                while ((text = reader.readLine()) != null) {
                    sb.append(text);
                }

                jsonConfig = new JSONObject(sb.toString());
                if (edio != null) {
                    edio.disconnect();
                }

                edio = new Edio(jsonConfig);
            } catch (IOException | JSONException ex) {
                logger.log(Level.SEVERE, ex.getMessage(), ex);
                return;
            } catch (InvalidParameterException ex) {
                logger.log(Level.SEVERE, ex.getMessage(), ex);
                edio = null;
            }
        } else {
            logger.log(Level.CONFIG, "Servelet requires setup");
        }
        
        setupNeeded = edio == null;
    }

    /*
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
        response.setContentType("application/json;charset=utf-8");
        //response.setHeader("Access-Control-Allow-Origin", "*");
        ServletContext context = getServletContext();

        if (request.getQueryString() != null && request.getQueryString().equals("refresh")) {
            loadConfig(context);
            response.sendRedirect(request.getContextPath());
        } else {
            try ( PrintWriter out = response.getWriter()) {
                if (setupNeeded) {
                    out.println("{\"setup_required\": true}");
                } else {
                    out.println(edio.executor());
                }
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

    /*    private static String toCamelCase(String value) {
	StringBuilder returnValue = new StringBuilder();
	String throwAwayChars = "()[]{}=?!.:,-_+\\\"#~/";
	value = value.replaceAll("[" + Pattern.quote(throwAwayChars) + "]", " ");
	value = value.toLowerCase();
	boolean makeNextUppercase = true;
	for (char c : value.toCharArray()) {
		if (Character.isSpaceChar(c) || Character.isWhitespace(c)) {
			makeNextUppercase = true;
		} else if (makeNextUppercase) {
			c = Character.toTitleCase(c);
			makeNextUppercase = false;
		}
		returnValue.append(c);
	}
	return returnValue.toString().replaceAll("\\s+", "");
    }
     */
}
