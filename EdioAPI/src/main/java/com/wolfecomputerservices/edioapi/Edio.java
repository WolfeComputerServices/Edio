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
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.mail.MessagingException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/**
 *
 * @author  Wolfe Computer Services (Ed Wolfe}
 *
**/
public class Edio {
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

    private boolean keepAlive = false;
    
    private JSONObject cfgEdio = null, cfgOutput = null;
    private final JSONObject output = new JSONObject();
    private final JSONArray outErrors = new JSONArray();
    
    private JSONObject finalizeOutput() {
        output.put("errors", outErrors);
        return output;
    }
    
    private boolean doOutput(final String outputType) {
        return cfgOutput.has(outputType) && cfgOutput.getBoolean(outputType);
    }
    
    private boolean validConfig() {
        if ((cfgEdio != null && cfgOutput != null) &&
            cfgEdio.has("credentials") &&
            (cfgEdio.getJSONObject("credentials").has("pass") && 
                cfgEdio.getJSONObject("credentials").has("pass")))
            return true;
        
        return false;
    }
    
    private JSONObject requiresSetup() {
        return requiresSetup(output);
    }
    public JSONObject requiresSetup(JSONObject output) {
        output.put("setup_required", true);
        
        return output;
    }

    public void cliRun(String[] args) {
        try {
            File file = new File(args[0]);
            byte[] data;
            try (FileInputStream fis = new FileInputStream(file)) {
                data = new byte[(int) file.length()];
                fis.read(data);
            }
            
            JSONObject json = new JSONObject(new String(data, "UTF-8"));
            cfgEdio = json.getJSONObject("edio");
            cfgOutput = json.getJSONObject("output");
            
            System.out.println(executor().toString());
        } catch (IOException | JSONException ex) {
            Logger.getLogger(Edio.class.getName()).log(Level.SEVERE, null, ex);
            outErrors.put("Config file specified could not be found or is invalid: " + ex.getMessage());
            System.out.println(finalizeOutput());
        }
    }

    public JSONObject srvletRun(JSONObject config) {
        keepAlive = true;
        cfgEdio = config.getJSONObject("edio");
        cfgOutput = config.getJSONObject("output");

        return executor();
    }

    private JSONObject executor() {
        if (!validConfig())
            return requiresSetup();
        
        EdioAPI edioAPI = new EdioAPI(
                cfgEdio.getJSONObject("credentials").getString("user"), 
                cfgEdio.getJSONObject("credentials").getString("pass"));    
        if (!edioAPI.connect()) {
            output.append("error", "Failed to connect with credentials provided");
            return finalizeOutput();
        }
        HashMap<String, Integer> children = edioAPI.getChildren();

        final JSONArray relationships = cfgOutput.getJSONArray("children");
        final JSONArray requestedChildren = relationships.length() > 0 ? 
                relationships : new JSONArray(children.keySet());
        final LocalDateTime dateToInspect = LocalDateTime.now();
        if (doOutput("school")) {
            boolean school;
            try {
                school = edioAPI.hasSchool(children
                            .entrySet()
                            .stream()
                            .findFirst()
                            .get()
                            .getValue(), 
                        dateToInspect);
            } catch (IOException | InterruptedException ex) {
                Logger.getLogger(Edio.class.getName()).log(Level.SEVERE, null, ex);
                school = false;
                output.append("error", "Failed to get school status: " + ex.getMessage());
            }

            output.put("school", school);
        }

        JSONArray jsonChildren = new JSONArray();
        for (int i=0;i<requestedChildren.length();++i) {
            final String childName = requestedChildren.get(i).toString();
            if (!children.containsKey(childName)) {
                outErrors.put("Unable to find " + childName);
                continue;
            }
            
            JSONObject jsonChild = new JSONObject();
            jsonChild.put("name", toCamelCase(childName))
                    .put("id", children.get(childName));
            if (doOutput("overdue")) {
                jsonChild.put("overdues", 
                        edioAPI.overdues(children.get(childName))
                        .parallelStream()
                        .map((u) -> new JSONObject()
                            .put("dueDate", u.dateStart)
                            .put("course", u.eventName)
                            .put("assignment", u.eventDescription))
                        .collect(Collectors.toList())
                );
            }
            
            if (doOutput("upcoming")) {
                JSONObject upcoming_parms = cfgOutput.getJSONObject("upcoming_parms");
                List<EdioAPI.Upcoming> upcoming;
                try {
                    upcoming = edioAPI.getUpComing(children.get(childName),
                            upcoming_parms.getInt("days"));

                    jsonChild.put("upcoming", upcoming
                                    .parallelStream()
                                    .map((u) -> new JSONObject()
                                            .put("course", u.course)
                                            .put("date", u.date)
                                            .put("topic", u.topic))
                                    .collect(Collectors.toList())
                    );
                } catch (IOException | InterruptedException ex) {
                    Logger.getLogger(Edio.class.getName()).log(Level.SEVERE, null, ex);
                    outErrors.put("Unabled to get events for " + childName + ": " + ex.getMessage());
                }
            }

            if (doOutput("events")) {
                JSONObject events_parms = cfgOutput.getJSONObject("events_parms");
                try {
                    List<EdioAPI.Event> events = edioAPI.getDayEvents(
                            children.get(childName), 
                            events_parms.getString("date").equals("") ?
                                    LocalDate.now() : 
                                    LocalDate.parse(events_parms.getString("date")));

                    jsonChild.put("events", events
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
                    Logger.getLogger(Edio.class.getName()).log(Level.SEVERE, null, ex);
                    outErrors.put("Something WENT WRONG determining EVENTS: " + ex.getMessage());
                }
            }
            
            jsonChildren.put(jsonChild);
        }
        
        output.put("children", jsonChildren);
        
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
                        children
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
                Logger.getLogger(Edio.class.getName()).log(Level.SEVERE, null, ex);
                output.append("error", "Something WENT WRONG in iCal Mailer: " + ex.getMessage());
            }
        }
        
        if (!keepAlive)
            edioAPI.disconnect();
        
        return finalizeOutput();
    }
}
