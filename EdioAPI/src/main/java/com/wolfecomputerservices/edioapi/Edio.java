/*
 * Copyright (C) 2021 - Wolfe Computer Services
 * ALL RIGHTS RESERVED
 */

package com.wolfecomputerservices.edioapi;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.eclipse.jdt.annotation.Nullable;

/**
 *
 * @author  Wolfe Computer Services (Ed Wolfe}
 *
**/
public class Edio implements AutoCloseable {
    // Nullables
    private static final @Nullable Logger logger = Logger.getLogger(Edio.class.getName());
    private @Nullable EdioAPI edioAPI;
    private @Nullable JSONObject cfgEdio;
    private @Nullable JSONObject cfgOutput;
    
    // Finals
    private final JSONObject output = new JSONObject();
    private final JSONArray outErrors = new JSONArray();

    /**
     * Create an instance Edio.class
     * @param config JSONObject containing the run parameters
     */
    public Edio(JSONObject config) {
        setConfig(config);
    }
    
    /**
     * Create an instance of Edio.class
     * @param configFilePath Path to a file containing run parameters
     */
    public Edio(String configFilePath) {
        try {
            File file = new File(configFilePath);
            byte[] data;
            try (FileInputStream fis = new FileInputStream(file)) {
                data = new byte[(int) file.length()];
                fis.read(data);
            }

            String jsonString = new String(data, "UTF-8");
            setConfig(new JSONObject(jsonString)); // Added for binding support
            logger.config(String.format("Parameters loaded from file %s: %s", file.getName(), jsonString));
        } catch (IOException | JSONException ex) {
            logger.log(Level.SEVERE, ex.getMessage(), ex);
            outErrors.put("Config file specified could not be found or is invalid: " + ex.getMessage());
            System.out.println(finalizeOutput());
        }
    }
    
    /**
     * Create an instance of Edio.class. This constructor is useful when 
     * running as a console application.
     * @param args arg[0] must contain path to file containing run parameters
     */
    public Edio(String[] args)
    {
        this(args[0]);
    }

    /**
     * Returns any overdue assignments, tests and/or quizzes
     * @param studentName The student name to get overdues for
     * @return JSONArray containing 0...n items
     */
    public JSONArray getOverdues(String studentName) {
        try {
            return findStudent(studentName, executor(false, true, false, false).getJSONArray("students"))
                    .getJSONArray("overdues");
        } catch (JSONException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            return new JSONArray();
        }
    }

    /**
     * Returns any upcoming, non-class, events. This includes any student
     * entered events.
     * @param studentName The student to get upcoming events for
     * @return JSONArray of 0...n items
     */
    public JSONArray getUpcomingEvents(String studentName) {
        try {
            return findStudent(studentName, executor(false, false, true, false).getJSONArray("students"))
                    .getJSONArray("upcoming");
        } catch (JSONException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            return new JSONArray();
        }
    }

    /**
     * Return the students associated with coach's account
     * @return JSONArray of 0...n items.
     */
    public JSONArray getStudents() {
        return edioAPI.getStudents();
    }

    /**
     * Get list of students as a map
     * @return Map containing 0...n students.
     */
    public Map<String, Integer> getStudentsAsMap() {
        return edioAPI.getStudentsAsMap();
    }

    /**
     * Determines if there is school today.
     * @return True if there is school today, false if not.
     */
    public boolean hasSchool() {
        return executor(true, false, false, false).getBoolean("school");
    }

    /**
     * Determines if there are overdue assignments, quizzes and/or test.
     * @param studentName The student name to make the determination for.
     * @return True if studentName has overdues, false if not.
     */
    public boolean hasOverdues(String studentName) {
        try {
            return getOverdues(studentName).length() > 0;
        } catch (NullPointerException e) {
            return false;
        }
    }

/*    public void cliRun(String[] args) {
        try {
            File file = new File(args[0]);
            byte[] data;
            try (FileInputStream fis = new FileInputStream(file)) {
                data = new byte[(int) file.length()];
                fis.read(data);
            }
            
            setConfig(new JSONObject(new String(data, "UTF-8")));
            
            System.out.println(executor().toString());
        } catch (IOException | JSONException ex) {
            logger.log(Level.SEVERE, null, ex);
            outErrors.put("Config file specified could not be found or is invalid: " + ex.getMessage());
            System.out.println(finalizeOutput());
        }
    }
    
    public JSONObject srvletRun() {
        return executor();
    }
*/
    /**
     * Returns all available data
     * @return JSONObject containing data.
     */
    public JSONObject executor() {
        return executor(outputWanted("school"), outputWanted("overdue"), 
                outputWanted("upcoming"), outputWanted("events"));
    }

    /**
     * Locates the JSONObject in students with name of studentName
     * @param studentName The name of the student to find
     * @param students the student list to search
     * @return the JSONObject if found, null otherwise.
     */
    public @Nullable JSONObject findStudent(String studentName, JSONArray students) {
        for (int i = 0; i < students.length(); i++) {
            JSONObject student = students.getJSONObject(i);
            if (studentName.equalsIgnoreCase(student.getString("name")))
                return student;
        }

        return null;
    }

    public boolean disconnect() {
        return edioAPI.disconnect();
    }
    /**
     * Returns the data specified by the parameters.
     * @param doSchool If true, has school indicator will be returned
     * @param doOverdue if true, overdue items will be returned
     * @param doUpcoming if true, upcoming events will be returned
     * @param doEvents if true, events for today will be returned
     * @return 
     */
    public JSONObject executor(boolean doSchool, boolean doOverdue, boolean doUpcoming, boolean doEvents) {
        if (edioAPI == null)
            return requiresSetup();
        
        Map<String, Integer> students = edioAPI.getStudentsAsMap();

        final JSONArray relationships = cfgOutput.has("students") ? 
                cfgOutput.getJSONArray("students") : new JSONArray();
        final JSONArray requestedStudents = relationships.length() > 0 ? 
                relationships : new JSONArray(students.keySet());
        final LocalDateTime dateToInspect = LocalDateTime.now();
        if (doSchool) {
            boolean school;
            try {
                school = edioAPI.hasSchool(students
                            .entrySet()
                            .stream()
                            .findFirst()
                            .get()
                            .getValue(), 
                        dateToInspect);
            } catch (IOException | InterruptedException ex) {
                logger.log(Level.SEVERE, null, ex);
                school = false;
                output.append("error", "Failed to get school status: " + ex.getMessage());
            }

            output.put("school", school);
        }

        JSONArray jsonStudents = new JSONArray();
        for (int i=0;i<requestedStudents.length();++i) {
            final String studentName = requestedStudents.get(i).toString();
            if (!students.containsKey(studentName)) {
                outErrors.put("Unable to find " + studentName);
                continue;
            }
            
            JSONObject jsonStudent = new JSONObject();
            jsonStudent.put("name", toCamelCase(studentName))
                    .put("id", students.get(studentName));
            if (doOverdue) {
                jsonStudent.put("overdues", 
                        edioAPI.overdues(students.get(studentName))
                        .parallelStream()
                        .map((u) -> new JSONObject()
                            .put("dueDate", u.dateStart)
                            .put("course", u.eventName)
                            .put("assignment", u.eventDescription))
                        .collect(Collectors.toList())
                );
            }
            
            if (doUpcoming) {
                JSONObject upcoming_parms = cfgOutput.getJSONObject("upcoming_parms");
                List<EdioAPI.Upcoming> upcoming;
                try {
                    upcoming = edioAPI.getUpComing(students.get(studentName),
                            upcoming_parms.getInt("days"));

                    jsonStudent.put("upcoming", upcoming
                                    .parallelStream()
                                    .map((u) -> new JSONObject()
                                            .put("course", u.course)
                                            .put("date", u.date)
                                            .put("topic", u.topic))
                                    .collect(Collectors.toList())
                    );
                } catch (IOException | InterruptedException ex) {
                    logger.log(Level.SEVERE, null, ex);
                    outErrors.put("Unabled to get events for " + studentName + ": " + ex.getMessage());
                }
            }

            if (doEvents) {
                JSONObject events_parms = cfgOutput.getJSONObject("events_parms");
                try {
                    List<EdioAPI.Event> events = edioAPI.getDayEvents(
                            students.get(studentName), 
                            events_parms.getString("date").equals("") ?
                                    LocalDate.now() : 
                                    LocalDate.parse(events_parms.getString("date")));

                    jsonStudent.put("events", events
                                .parallelStream()
                                .map((u) -> new JSONObject()
                                    .put("date", new JSONObject()
                                        .put("end", u.dateEnd)
                                        .put("start", u.dateStart))
                                    .put("description", u.eventDescription)
                                    .put("name", u.eventName))
                                 .collect(Collectors.toList())
                    );
                } catch (IOException | InterruptedException ex) {
                    logger.log(Level.SEVERE, null, ex);
                    outErrors.put("Something WENT WRONG determining EVENTS: " + ex.getMessage());
                }
            }
            
            jsonStudents.put(jsonStudent);
        }
        
        output.put("students", jsonStudents);
/*        
        if (doOutput("ical_mailer")) {
            try {
                JSONObject ical_mailer_parms = cfgOutput.getJSONObject("ical_mailer_parms");
                JSONObject ical_mailer_smtp = ical_mailer_parms.getJSONObject("smtp");
                JSONObject ical_mailer_credentials = ical_mailer_smtp.getJSONObject("credentials");
                JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
                mailSender.setUsername(ical_mailer_credentials.getString("user"));
                mailSender.setPassword(ical_mailer_credentials.getString("pass"));

                Properties properties = new Properties();
                properties.put("mailsmtp.auth", "true");
                properties.put("mail.smtp.starttls.enable", ical_mailer_smtp.getBoolean("ssl"));
                properties.put("mail.smtp.host", ical_mailer_smtp.getString("host"));
                properties.put("mail.smtp.port", String.valueOf(ical_mailer_smtp.getInt("port")));
                mailSender.setJavaMailProperties(properties);

                final LocalDate ld = ical_mailer_parms.has("date") ?
                        LocalDate.parse(ical_mailer_parms.getString("date")) : 
                        LocalDate.now();

                int sent = edioAPI.mailCalenderEvents(
                        students
                            .entrySet()
                            .stream()
                            .findFirst()
                            .get()
                            .getValue(), 
                        mailSender,
                        ical_mailer_parms.getString("from"), 
                        ical_mailer_parms.getString("to"), 
                        ld);
                output.put("icals_sent", sent);
            } catch (MessagingException ex) {
                logger.log(Level.SEVERE, null, ex);
                output.append("error", "Something WENT WRONG in iCal Mailer: " + ex.getMessage());
            }
        }
  */      
        return finalizeOutput();
    }
    /**
     * Adds indicator saying setup is required to the output
     * @param output The current output JSONObject
     * @return The output JSONObject with indicator added
     */
    private JSONObject requiresSetup(JSONObject output) {
        output.put("setup_required", true);
        
        return output;
    }

    /**
     * Set the configuration parameters for this instance.
     * @param config 
     */
    private void setConfig(JSONObject config) {
        edioAPI = null;
        cfgEdio = config.getJSONObject("edio");
        cfgOutput = config.getJSONObject("output");
        
        if (validConfig()) {
            JSONObject credentials = cfgEdio.getJSONObject("credentials");
            edioAPI = new EdioAPI(
                    credentials.getString("user"),
                    credentials.getString("pass"));
        }
    }

    /**
     * Convert a string to camel case
     * @param value the string to convert
     * @return the converted string
     */
    private String toCamelCase(String value) {
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

    /**
     * Package any errors into the output in preparation for returning
     * @return the prepared output
     */
    private JSONObject finalizeOutput() {
        output.put("errors", outErrors);
        return output;
    }
    
    /**
     * Determines if the outputType is requested for output based on the 
     * configuration.
     * @param outputType the type to check
     * @return true if outputType is requested, false otherwise.
     */
    private boolean outputWanted(final String outputType) {
        return cfgOutput.has(outputType) && cfgOutput.getBoolean(outputType);
    }
    
    /**
     * Determines if the configuration is valid.
     * @return true if the configuration is valid, false otherwise.
     */
    private boolean validConfig() {
        return (cfgEdio != null && cfgOutput != null) &&
                cfgEdio.has("credentials") &&
                (cfgEdio.getJSONObject("credentials").has("pass") &&
                cfgEdio.getJSONObject("credentials").has("pass"));
    }
    
    /**
     * Helper function to set indicator showing a setup is required.
     * @return output JSONObject with indicator
     */
    private JSONObject requiresSetup() {
        return requiresSetup(output);
    }

    @Override
    public void close() throws IOException {
        disconnect();
    }
}
