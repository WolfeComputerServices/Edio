/*
 * Copyright (C) 2021 - Wolfe Computer Services
 * ALL RIGHTS RESERVED
 */
package com.wolfecomputerservices.edioapi;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.URI;
import java.time.Duration;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.activation.DataHandler;
import org.json.JSONArray;

import org.json.JSONObject;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import org.springframework.mail.javamail.JavaMailSender;
/**
 *
 * @author Ed Wolfe
 * Copyright (C) 2021 Wolfe Computer Services
 * 
 */
public class EdioAPI {
    /*  Event Kinds
        --------------------------
        0
        1
        3
        4       Field Trip
        7
        9       Holiday
        
    */
    public EdioAPI(final String userId, final String userPass) {
        this.userId = userId;
        this.userPass = userPass;
    }
    
    public boolean connect() {
        return authentication(Authentication.logon);
    }
    public boolean disconnect() {
        return authentication(Authentication.logoff);
    }
    
    //private final String iCalDateFormat = "yyyMMdd HHmmss";
    private final short maxRetries = 3;
    private short retries = 0;
    private final String UserAgent = 
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
            + "(KHTML, like Gecko) Chrome/95.0.4638.69 Safari/537.36 Edg/95.0.1020.44";
    private final String sec_ch_ua = "\"Microsoft Edge\";v=\"95\", "
            + "\"Chromium\";v=\"95\", \";Not A Brand\";v=\"99\"";
    private final String sec_ch_ua_mobile = "?0";
    private final String sec_ch_ua_platform = "Windows";
    private final String sec_fetch_dest_empty = "empty";
    private final String sec_fetch_code = "cors";
    private final String sec_fetch_site_sameOrigin = "same-origin";
    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final String authority = "www.myedio.com";
    private final String appJson = "application/json";
    private final String refererDashboard = "https://www.myedio.com/dashboard/";
    private final String refererLogon = "https://www.myedio.com/login/";
    
    private final String userId;
    private final String userPass;
    private String cookies = null;
    private HttpResponse<String> lastResponse;
    
    private JSONObject accountUser = null;

    private enum Authentication {
        logon,
        logoff
    }
    
    public class Event {
        public final int id;
        public final String dateCreated;
        public final String dateStart;
        public final String dateEnd;
        public final String eventName;
        public final String eventDescription;
        
        private final String ICAL_FORMAT = "yyyyMMdd HHmmss";
        
        public Event(final int Id, final String CreatedDate, 
                final String StartDate, final String EndDate, 
                final String EventName, final String EventDescription) {
            id = Id;
            dateCreated = CreatedDate;
            dateStart = StartDate;
            dateEnd = EndDate;
            eventName = EventName;
            eventDescription = EventDescription;
        }
        
        public String formatDateAs(final String date, final String format) {
            return ZonedDateTime
                    .parse(date)
                    .format(DateTimeFormatter.ofPattern(format));
        }

        public String formatDateAsiCal(final String date) {
            return formatDateAs(date, ICAL_FORMAT)
                    .replace(" ", "T");
                    //+ "Z";
        }

        public String formatDateAsISO8601(final String date) {
            return ZonedDateTime
                    .parse(date)
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                    .replace(" ", "T");
                    //+"Z";
        }
        public String formatDateAs(final String date, final DateTimeFormatter dtfFormat) {
            return ZonedDateTime
                    .parse(date)
                    .format(dtfFormat);
        }
    }
    public class UpComing {
        public final String date;
        public final String course;
        public final String topic;
        
        public UpComing(final String Date, final String Course, final String Topic) {
            date = Date;
            course = Course;
            topic = Topic;
        }
    }
    
    private boolean connectIfNeeded() {
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create("https://www.myedio.com/api/v1/user/settings"))
                .header("Content-type", appJson)
                .setHeader("authority", authority)
                .setHeader("referer", refererDashboard)
                .setHeader("sec-ch-ua", sec_ch_ua)
                .setHeader("sec-ch_ua-mobile", sec_ch_ua_mobile)
                .setHeader("sec-ch-ua-platform", sec_ch_ua_platform)
                .setHeader("sec-fetch-dest", sec_fetch_dest_empty)
                .setHeader("sec-fetch-code", sec_fetch_code)
                .setHeader("sec-fetch-site", sec_fetch_site_sameOrigin)
                .setHeader("User-Agent", UserAgent)
                .setHeader("cookie", cookies)
                .build();
        try {
            lastResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException ex) {
            Logger.getLogger(EdioAPI.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }

        switch (lastResponse.statusCode()) {
            case 401:           // Unauthorized: login timedout
                authentication(Authentication.logon);
                break;
        }
        
        switch (lastResponse.statusCode()) {
            case 200:
            case 404:
                return true;
            default:
                return false;
        }
    }
    private boolean authentication(Authentication type) {
        retries++;
        HttpRequest.Builder builder;
        if (null == type) {
            return false;
        } else switch (type) {
            case logon:
                String json = new StringBuilder()
                        .append("{")
                        .append(String.format("\"password\": \"%s\",", userPass))
                        .append(String.format("\"username\": \"%s\"", userId))
                        .append("}").toString();
                builder = HttpRequest.newBuilder()
                        .POST(HttpRequest.BodyPublishers.ofString(json));
                break;
            case logoff:
                builder = HttpRequest.newBuilder()
                        .DELETE();
                break;
            default:
                return false;
        }
        HttpRequest request = builder
                .uri(URI.create("https://www.myedio.com/api/v1/authentication/"))
                .header("Content-type", appJson)
                .setHeader("authority", authority)
                .setHeader("referer", refererLogon)
                .setHeader("sec-ch-ua", sec_ch_ua)
                .setHeader("sec-ch_ua-mobile", sec_ch_ua_mobile)
                .setHeader("sec-ch-ua-platform", sec_ch_ua_platform)
                .setHeader("sec-fetch-dest", sec_fetch_dest_empty)
                .setHeader("sec-fetch-code", sec_fetch_code)
                .setHeader("sec-fetch-site", sec_fetch_site_sameOrigin)
                .setHeader("User-Agent", UserAgent)
                .build();

        try {
            lastResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException ex) {
            Logger.getLogger(EdioAPI.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
        if (lastResponse.statusCode() != 200)
            return false;

        retries = 0;
        cookies = lastResponse.headers().map().get("set-cookie").toString();
        accountUser = new JSONObject(lastResponse.body()).getJSONObject("resultObject");
        
        return true;
    }
    
    private int getAccountUserId() {
        return accountUser.getInt("id");
    }
    private String getAccountEmail() {
        return accountUser.getString("email");
    }
    
    public HashMap<String, Integer> getChildren() {
        HashMap<String, Integer> values = new HashMap<>();
        if (connectIfNeeded()) {
            HttpRequest request = HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create(String.format(
                            "https://www.myedio.com/api/v1/users/%d?includePrimaryRelationships=true",
                            getAccountUserId())))
                    .header("Content-type", appJson)
                    .setHeader("authority", authority)
                    .setHeader("referer", refererDashboard)
                    .setHeader("sec-ch-ua", sec_ch_ua)
                    .setHeader("sec-ch_ua-mobile", sec_ch_ua_mobile)
                    .setHeader("sec-ch-ua-platform", sec_ch_ua_platform)
                    .setHeader("sec-fetch-dest", sec_fetch_dest_empty)
                    .setHeader("sec-fetch-code", sec_fetch_code)
                    .setHeader("sec-fetch-site", sec_fetch_site_sameOrigin)
                    .setHeader("User-Agent", UserAgent)
                    .setHeader("cookie", cookies)
                    .build();
            try {
                lastResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (IOException | InterruptedException ex) {
                Logger.getLogger(EdioAPI.class.getName()).log(Level.SEVERE, null, ex);
                return new HashMap<>();
            }

            switch (lastResponse.statusCode()) {
                case 200:
                    JSONArray relationships = new JSONObject(lastResponse.body())
                            .getJSONObject("resultObject")
                            .getJSONArray("primaryUserRoleRelationships");
                    for (int i=0;i < relationships.length();++i) {
                        JSONObject relationship = relationships.getJSONObject(i).getJSONObject("secondaryUser");
                        values.put(relationship.getString("firstName"),
                             relationship.getInt("id"));
                    };
                    break;
                default:
                    Logger.getLogger(EdioAPI.class.getName()).log(Level.SEVERE, null, "HTTP Code: " + lastResponse.statusCode());
                    return new HashMap<>();
            }
        }
        return values;
    }
    
    public boolean hasOverdues(final int studentId) {
        if (connectIfNeeded()) {
            HttpRequest request = HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create(String.format(
                            "https://www.myedio.com/api/v1/days/users/progress?userIds=%d",
                            studentId)))
                    .header("Content-type", appJson)
                    .setHeader("authority", authority)
                    .setHeader("referer", refererDashboard)
                    .setHeader("sec-ch-ua", sec_ch_ua)
                    .setHeader("sec-ch_ua-mobile", sec_ch_ua_mobile)
                    .setHeader("sec-ch-ua-platform", sec_ch_ua_platform)
                    .setHeader("sec-fetch-dest", sec_fetch_dest_empty)
                    .setHeader("sec-fetch-code", sec_fetch_code)
                    .setHeader("sec-fetch-site", sec_fetch_site_sameOrigin)
                    .setHeader("User-Agent", UserAgent)
                    .setHeader("cookie", cookies)
                    .build();
            try {
                lastResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (lastResponse.statusCode() != 200)
                    return false;
            } catch (IOException | InterruptedException ex) {
                Logger.getLogger(EdioAPI.class.getName()).log(Level.SEVERE, null, ex);
                return false;
            }

            if (lastResponse.statusCode() == 200) {
                JSONArray courses = new JSONObject(lastResponse.body()).getJSONArray("resultObject");
                for (int i=0;i < courses.length();++i) {
                    JSONObject course = courses.getJSONObject(i);
                    if (course.getInt("studentOverdue") > 0)
                        return true;
                };
            } else if (lastResponse.statusCode() == 401) {
                if (retries < maxRetries) {
                    authentication(Authentication.logon);
                    return hasOverdues(studentId);
                }
            }
        }
        return false;
    }
    
    public boolean hasSchool(final int studentId, LocalDateTime dateTime) 
            throws IOException, InterruptedException {
        switch (dateTime.getDayOfWeek()) {
            case SATURDAY:
            case SUNDAY:
                return false;
        }
        final ZonedDateTime startTime = dateTime
                .atZone(ZoneId.systemDefault())
                .withHour(0)
                .withMinute(0)
                .withSecond(0)
                .withZoneSameInstant(ZoneId.of("UTC"));
        final ZonedDateTime endTime = startTime
                .plusDays(1);
        if (connectIfNeeded()) {
            HttpRequest request = HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create(String.format(
                            "https://www.myedio.com/api/v1/events?"
                                    + "endDate=%s&eventKinds=1,4,7,9,0,3&"
                                    + "includeType=true&startDate=%s&userIds=%d",
                            endTime.format(DateTimeFormatter.ISO_INSTANT),
                            startTime.format(DateTimeFormatter.ISO_INSTANT),
                            studentId)))
                    .header("Content-type", appJson)
                    .setHeader("authority", authority)
                    .setHeader("referer", refererDashboard)
                    .setHeader("sec-ch-ua", sec_ch_ua)
                    .setHeader("sec-ch_ua-mobile", sec_ch_ua_mobile)
                    .setHeader("sec-ch-ua-platform", sec_ch_ua_platform)
                    .setHeader("sec-fetch-dest", sec_fetch_dest_empty)
                    .setHeader("sec-fetch-code", sec_fetch_code)
                    .setHeader("sec-fetch-site", sec_fetch_site_sameOrigin)
                    .setHeader("User-Agent", UserAgent)
                    .setHeader("cookie", cookies)
                    .build();
            lastResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            switch (lastResponse.statusCode()) {
                case 200:
                    JSONArray events = new JSONObject(lastResponse.body()).getJSONArray("resultObject");
                    for (int i=0;i < events.length();++i) {
                        JSONObject event = events.getJSONObject(i);
                        if (event.getString("description").toLowerCase().contains("no school "))
                            return false;
                    };
                    break;
                default:
                    throw new IOException("HTTP Code: " + lastResponse.statusCode());
            }
        }        
        
        return true;
    }
    
    @Deprecated
    public List<Event> getTodaysEvents(final int studentId) 
            throws IOException, InterruptedException {
        try {
            return getDayEvents(studentId, LocalDate.now());
        } catch (InterruptedException ex) {
            Logger.getLogger(EdioAPI.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }
    
    public List<Event> getDayEvents(final int studentId, LocalDate date) 
            throws IOException, InterruptedException {
        ArrayList<Event> values = new ArrayList();
        final ZonedDateTime startTime = date.atStartOfDay()
                .atZone(ZoneId.systemDefault())
                .withZoneSameInstant(ZoneId.of("UTC"));
        final ZonedDateTime endTime = startTime.plusDays(1).minusSeconds(1);
        
        if (connectIfNeeded()) {
            HttpRequest request = HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create(String.format(
                            "https://www.myedio.com/api/v1/events?"
                                    + "endDate=%s&eventKinds=1,4,7,9,0,3&"
                                    + "includeType=true&startDate=%s&userIds=%d",
                            endTime.format(DateTimeFormatter.ISO_INSTANT),
                            startTime.format(DateTimeFormatter.ISO_INSTANT),
                            studentId)))
                    .header("Content-type", appJson)
                    .setHeader("authority", authority)
                    .setHeader("referer", refererDashboard)
                    .setHeader("sec-ch-ua", sec_ch_ua)
                    .setHeader("sec-ch_ua-mobile", sec_ch_ua_mobile)
                    .setHeader("sec-ch-ua-platform", sec_ch_ua_platform)
                    .setHeader("sec-fetch-dest", sec_fetch_dest_empty)
                    .setHeader("sec-fetch-code", sec_fetch_code)
                    .setHeader("sec-fetch-site", sec_fetch_site_sameOrigin)
                    .setHeader("User-Agent", UserAgent)
                    .setHeader("cookie", cookies)
                    .build();
            lastResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            switch (lastResponse.statusCode()) {
                case 200:
                    JSONArray events = new JSONObject(lastResponse.body()).getJSONArray("resultObject");
                    for (int i=0;i < events.length();++i) {
                        JSONObject event = events.getJSONObject(i);
                        values.add(new Event(event.getInt("id"),
                                event.getString("createdOn"),
                                event.getString("startsOn"),
                                event.getString("endsOn"),
                                event.getString("name"),
                                event.getString("description")));
                    };
                    break;
                default:
                    throw new IOException("HTTP Code: " + lastResponse.statusCode());
            }
        }        
        
        return values;
    }

    private String fmtiCalContentLine(final String line) {
        int index = 0;
        String prefix = "";
        StringBuilder rValue = new StringBuilder();
        while (index < line.length()) {
            rValue.append(prefix);
            prefix = "\n ";
            rValue.append(line.substring(index, Math.min(index + 75, line.length())));
            index += 75;
        }
            
        return rValue.toString() + "\n";
    }
    public int mailCalenderEvents(final int StudentId, JavaMailSender mailSender, 
            final String fromEmail, final String toEmail, final LocalDate dt) 
            throws MessagingException {
        int count = 0;
        try {
            List<Event> events = null;
            events = getDayEvents(StudentId, dt);
            
            for (int i=0;i<events.size();++i) {
                count++;
                Event event = events.get(i);

                StringBuilder ics = new StringBuilder();
                ics.append(fmtiCalContentLine("BEGIN:VCALENDAR"))
                    .append(fmtiCalContentLine("METHOD:REQUEST"))
                    .append(fmtiCalContentLine("PRODID:-//Wolfe Computer Services/Edio API//EN"))
                    .append(fmtiCalContentLine("VERSION:2.0"));
                      

                ics.append(fmtiCalContentLine("BEGIN:VEVENT"))
                    .append(fmtiCalContentLine(String.format("DESCRIPTION;LANGUAGE=en-US:%s",event.eventDescription)))
                    .append(fmtiCalContentLine(String.format("SUMMARY;LANGUAGE=en-US:%s", event.eventName)))
                    .append(fmtiCalContentLine(String.format("DTSTAMP:%s", event.formatDateAsiCal(event.dateCreated))))
                    .append(fmtiCalContentLine(String.format("ORGANIZER;CN=Commonwealth Charter Academy:mailto:%s", getAccountEmail())))
                    .append(fmtiCalContentLine(String.format("ATTENDEE;PARTSTAT=ACCEPTED;CN=Student:EMAIL=%s:MAILTO:%s",toEmail,toEmail)))
                    .append(fmtiCalContentLine(String.format("DTSTART:%s", event.formatDateAsiCal(event.dateStart))))
                    .append(fmtiCalContentLine(String.format("DTEND:%s", event.formatDateAsiCal(event.dateEnd))))
                    .append(fmtiCalContentLine(String.format("UID:edio-%d", event.id)))
                    .append(fmtiCalContentLine("STATUS:CONFIRMED"))
                    .append(fmtiCalContentLine("END:VEVENT"));
                
                ics.append(fmtiCalContentLine("END:VCALENDAR"));
        
                if (count > 0) {
                    Message msg = mailSender.createMimeMessage();
                    MimeMessage mimeMessage = mailSender.createMimeMessage();
                    mimeMessage.setSubject("Edio Event");
                    mimeMessage.addHeaderLine("method=REQUEST");
                    mimeMessage.addHeaderLine("charset=UTF-8");
                    mimeMessage.addHeaderLine("component=VEVENT");
                    mimeMessage.setFrom(new InternetAddress(fromEmail));
                    mimeMessage.addRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));

                    MimeBodyPart messageBodyPart = new MimeBodyPart();
                    messageBodyPart.setHeader("Content-Class", "urn:content-classes:calendarmessage");
                    messageBodyPart.setHeader("Content-ID", "calendar_message");
                    messageBodyPart.setHeader("Content-Transfer-Encoding", "base64");
                    messageBodyPart.setHeader("Content-Disposition", "attachment; filename=invite.ics");
                    messageBodyPart.setDataHandler(new DataHandler(
                            new ByteArrayDataSource(ics.toString(), "text/calendar;charset=utf-8;method=REQUEST;name=invite.ics")
                    ));

                    MimeMultipart multipart = new MimeMultipart();
                    multipart.addBodyPart(messageBodyPart);

                    mimeMessage.setContent(multipart);

                    mailSender.send(mimeMessage);

                }
            }
        } catch (InterruptedException | IOException ex) {
            Logger.getLogger(EdioAPI.class.getName()).log(Level.SEVERE, null, ex);
        }
       return count;
    }
    
    public List<UpComing> getUpComing(final int studentId, final int numberOfDaysAhead) 
            throws IOException, InterruptedException {
        final List<String> keyWords = Collections.unmodifiableList(
            new ArrayList<>(Arrays.asList(
                    "Quiz", "Test", "Due"
            )));
        ArrayList<UpComing> values = new ArrayList<>();
        
        final ZonedDateTime startTime = LocalDateTime.now()
                .atZone(ZoneId.systemDefault())
                .withHour(0)
                .withMinute(0)
                .withSecond(0)
                .withZoneSameInstant(ZoneId.of("UTC"));
        
        final ZonedDateTime endTime = startTime.plusDays(numberOfDaysAhead+1);

        if (connectIfNeeded()) {
            HttpRequest request = HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create(String.format(
                            "https://www.myedio.com/api/v1/calendars/dayusers?"
                                    + "endDate=%s&"
                                    + "startDate=%s&"
                                    + "userId=%d",
                            endTime.format(DateTimeFormatter.ISO_INSTANT),
                            startTime.format(DateTimeFormatter.ISO_INSTANT),
                            studentId)))
                    .header("Content-type", appJson)
                    .setHeader("authority", authority)
                    .setHeader("referer", refererDashboard)
                    .setHeader("sec-ch-ua", sec_ch_ua)
                    .setHeader("sec-ch_ua-mobile", sec_ch_ua_mobile)
                    .setHeader("sec-ch-ua-platform", sec_ch_ua_platform)
                    .setHeader("sec-fetch-dest", sec_fetch_dest_empty)
                    .setHeader("sec-fetch-code", sec_fetch_code)
                    .setHeader("sec-fetch-site", sec_fetch_site_sameOrigin)
                    .setHeader("User-Agent", UserAgent)
                    .setHeader("cookie", cookies)
                    .build();

            lastResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            switch (lastResponse.statusCode()) {
                case 200:
                    JSONArray courses = new JSONObject(lastResponse.body()).getJSONArray("resultObject");
                    for (int i=0;i < courses.length();++i) {
                        JSONObject course = courses.getJSONObject(i);
                        final JSONObject day = course.getJSONObject("day");
                        final String topic = day.getString("name");
                        final String name = day.getJSONObject("course").getString("name");
                        keyWords.parallelStream()
                                .forEach((key_word) -> {
                                    if (topic.toLowerCase().contains(key_word.toLowerCase()))
                                        values.add(new UpComing(day.getString("scheduledDate"), name, topic));
                                });
                    }   ;
                    break;
                case 401:
                    if (retries < maxRetries) {
                        authentication(Authentication.logon);
                        if (lastResponse.statusCode() == 200)
                            return getUpComing(studentId, numberOfDaysAhead);
                    }   break;
                default:
                    throw new IOException("HTTP Code: " + lastResponse.statusCode());
            }
        }
        return values
                .stream()
                .sorted((e1, e2) -> e1.date.compareTo(e2.date))
                .collect(Collectors.toList());
    }
}
