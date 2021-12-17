/*
 * Copyright (C) 2021 - Wolfe Computer Services
 * ALL RIGHTS RESERVED
 */
package com.wolfecomputerservices.edioapi;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.InvalidParameterException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import com.wolfecomputerservices.edioapi.objects.Configuration.Edio.Credentials;

/**
 *
 * @author Wolfe Computer Services - Initial contribution
 *
 */
@NonNullByDefault
public final class EdioAPI implements AutoCloseable {

    // Nullables
    private static final Logger logger = Logger.getLogger(EdioAPI.class.getName());

    private @Nullable Map<String, Object> accountUser = null;

    // Finals
    private final short maxRetries = 3;
    private final String UserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
            + "(KHTML, like Gecko) Chrome/95.0.4638.69 Safari/537.36 Edg/95.0.1020.44";
    private final String sec_ch_ua = "\"Microsoft Edge\";v=\"95\", "
            + "\"Chromium\";v=\"95\", \";Not A Brand\";v=\"99\"";
    private final String sec_ch_ua_mobile = "?0";
    private final String sec_ch_ua_platform = "Windows";
    private final String sec_fetch_dest_empty = "empty";
    private final String sec_fetch_code = "cors";
    private final String sec_fetch_site_sameOrigin = "same-origin";
    private final HttpClient httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2)
            .connectTimeout(Duration.ofSeconds(10)).build();
    private final String authority = "www.myedio.com";
    private final String appJson = "application/json";
    private final String refererDashboard = "https://www.myedio.com/dashboard/";
    private final String refererLogon = "https://www.myedio.com/login/";
    private final String refererDay = "https://www.myedio.com/calendar/day/";

    private final String userId;
    private final String userPass;

    private String cookies = "";
    private short retries = 0;

    @Override
    public void close() throws IOException {
        disconnect();
    }

    public EdioAPI(final Credentials credentials) {
        this(credentials.user, credentials.pass);
    }

    /**
     * Create an instance with credentials
     *
     * @param userId coach's user id
     * @param userPass coach's password
     */
    public EdioAPI(final String userId, final String userPass) {
        this.userId = userId;
        this.userPass = userPass;
        if (!connect()) {
            throw new InvalidParameterException("Failed to log on");
        }
    }

    /**
     * Authenticate into Edio
     *
     * @return true if successful
     */
    public boolean connect() {
        return authentication(AuthType.AT_LOGON);
    }

    /**
     * Logoff Edio
     *
     * @return true if successful
     */
    public boolean disconnect() {
        return authentication(AuthType.AT_LOGOFF);
    }

    /**
     * Checks to see if still authenticated and reconnects if not.
     *
     * @return true if connected, false if error
     */
    private boolean connectIfNeeded() {
        HttpRequest request = HttpRequest.newBuilder().GET()
                .uri(URI.create("https://www.myedio.com/api/v1/user/settings")).header("Content-type", appJson)
                .setHeader("authority", authority).setHeader("referer", refererDashboard)
                .setHeader("sec-ch-ua", sec_ch_ua).setHeader("sec-ch_ua-mobile", sec_ch_ua_mobile)
                .setHeader("sec-ch-ua-platform", sec_ch_ua_platform).setHeader("sec-fetch-dest", sec_fetch_dest_empty)
                .setHeader("sec-fetch-code", sec_fetch_code).setHeader("sec-fetch-site", sec_fetch_site_sameOrigin)
                .setHeader("User-Agent", UserAgent).setHeader("cookie", cookies).build();
        HttpResponse response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException ex) {
            logger.log(Level.SEVERE, null, ex);
            return false;
        }

        switch (response.statusCode()) {
            case 401: // Unauthorized: login timedout
                connect();
                break;
        }

        switch (response.statusCode()) {
            case 200:
            case 404:
                return true;
            default:
                return false;
        }
    }

    /**
     * Perform type authentication
     *
     * @param type type of action to take with regard to authentication
     * @return true if successful
     */
    private boolean authentication(AuthType type) {
        retries++;
        HttpRequest.Builder builder;
        if (null == type) {
            return false;
        } else {
            switch (type) {
                case AT_LOGON:
                    String json = new StringBuilder().append("{")
                            .append(String.format("\"password\": \"%s\",", userPass))
                            .append(String.format("\"username\": \"%s\"", userId)).append("}").toString();
                    builder = HttpRequest.newBuilder().POST(HttpRequest.BodyPublishers.ofString(json));
                    break;
                case AT_LOGOFF:
                    builder = HttpRequest.newBuilder().DELETE();
                    break;
                default:
                    return false;
            }
        }
        HttpRequest request = builder.uri(URI.create("https://www.myedio.com/api/v1/authentication/"))
                .header("Content-type", appJson).setHeader("authority", authority).setHeader("referer", refererLogon)
                .setHeader("sec-ch-ua", sec_ch_ua).setHeader("sec-ch_ua-mobile", sec_ch_ua_mobile)
                .setHeader("sec-ch-ua-platform", sec_ch_ua_platform).setHeader("sec-fetch-dest", sec_fetch_dest_empty)
                .setHeader("sec-fetch-code", sec_fetch_code).setHeader("sec-fetch-site", sec_fetch_site_sameOrigin)
                .setHeader("User-Agent", UserAgent).build();
        HttpResponse response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException ex) {
            logger.log(Level.SEVERE, null, ex);
            return false;
        }
        if (response.statusCode() != 200) {
            return false;
        }

        retries = 0;
        cookies = response.headers().map().get("set-cookie").toString();
        accountUser = (Map<String, Object>) EdioGson.toMap(response.body()).get("resultObject");

        return true;
    }

    /**
     * Gets the user id of the coach
     *
     * @return the id
     */
    private int getAccountUserId() {
        return (int) accountUser.get("id");
    }

    public Map<String, Object> getStudents() {
        if (connectIfNeeded()) {
            HttpRequest request = HttpRequest.newBuilder().GET()
                    .uri(URI.create(
                            String.format("https://www.myedio.com/api/v1/users/%d?includePrimaryRelationships=true",
                                    getAccountUserId())))
                    .header("Content-type", appJson).setHeader("authority", authority)
                    .setHeader("referer", refererDashboard).setHeader("sec-ch-ua", sec_ch_ua)
                    .setHeader("sec-ch_ua-mobile", sec_ch_ua_mobile).setHeader("sec-ch-ua-platform", sec_ch_ua_platform)
                    .setHeader("sec-fetch-dest", sec_fetch_dest_empty).setHeader("sec-fetch-code", sec_fetch_code)
                    .setHeader("sec-fetch-site", sec_fetch_site_sameOrigin).setHeader("User-Agent", UserAgent)
                    .setHeader("cookie", cookies).build();
            HttpResponse response;
            try {
                response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                switch (response.statusCode()) {
                    case 200:
                        return EdioGson.toMap(response.body());
                    default:
                        logger.log(Level.SEVERE, null, "HTTP Code: " + response.statusCode());
                }
            } catch (IOException | InterruptedException ex) {
                logger.log(Level.SEVERE, null, ex);
            }
        }
        return new HashMap<>(0);
    }

    public Map<String, Object> getOverdues(final int studentId) {
        if (connectIfNeeded()) {
            HttpRequest request = HttpRequest.newBuilder().GET()
                    .uri(URI.create(String.format("https://www.myedio.com/api/v1/calendars/dayusers/overdue?userId=%d",
                            studentId)))
                    .header("Content-type", appJson).setHeader("authority", authority).setHeader("referer", refererDay)
                    .setHeader("sec-ch-ua", sec_ch_ua).setHeader("sec-ch_ua-mobile", sec_ch_ua_mobile)
                    .setHeader("sec-ch-ua-platform", sec_ch_ua_platform)
                    .setHeader("sec-fetch-dest", sec_fetch_dest_empty).setHeader("sec-fetch-code", sec_fetch_code)
                    .setHeader("sec-fetch-site", sec_fetch_site_sameOrigin).setHeader("User-Agent", UserAgent)
                    .setHeader("cookie", cookies).build();
            HttpResponse response;
            try {
                response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (IOException | InterruptedException ex) {
                logger.log(Level.SEVERE, null, ex);
                return new HashMap<>(0);
            }

            switch (response.statusCode()) {
                case 200:
                    return EdioGson.toMap(response.body());

                case 401:
                    if (retries < maxRetries) {
                        authentication(AuthType.AT_LOGON);
                    }
                    break;
            }
        }

        return new HashMap<>(0);
    }

    public Map<String, Object> getDayEvents(final int studentId, LocalDate date)
            throws IOException, InterruptedException {
        return getDayEvents(studentId, date, new EventKinds[] { EventKinds.EK_0, EventKinds.EK_1, EventKinds.EK_3,
                EventKinds.EK_4, EventKinds.EK_7, EventKinds.EK_9 });
    }

    private Map<String, Object> combineResultObjects(Map<String, Object> from, Map<String, Object> to) {
        ArrayList<Object> fromResults = (ArrayList<Object>) from.get("resultObject");
        ArrayList<Object> toResults = (ArrayList<Object>) to.get("resultObject");

        if (fromResults == null)
            fromResults = new ArrayList<>(0);

        if (toResults == null)
            to.put("resultObject", fromResults);
        else {
            toResults.addAll(fromResults);
            to.put("resultObject", toResults);
        }

        return to;
    }

    @Deprecated
    private Map<String, Object> combine(Map<String, Object> from, Map<String, Object> to) {
        from.putAll(to);
        return to;
    }

    /**
     * Get the events for a given day
     * 
     * @param aStudentId
     * @param date
     * @param kinds
     * @return Events returned
     * @throws IOException
     * @throws InterruptedException
     */
    private Map<String, Object> getDayEvents(final int aStudentId, LocalDate date, EventKinds[] kinds)
            throws IOException, InterruptedException {

        int[] studentIds = Transformers.getStudentIds(getStudents());
        if (studentIds.length == 0) {
            logger.warning("No students found");
        } else {
            final int studentId = aStudentId == Integer.MIN_VALUE ? studentIds[0] : aStudentId;
            final ZonedDateTime startTime = date.atStartOfDay().atZone(ZoneId.systemDefault())
                    .withZoneSameInstant(ZoneId.of("UTC"));
            final ZonedDateTime endTime = startTime.plusDays(1).minusSeconds(1);

            if (connectIfNeeded()) {
                HttpRequest request = HttpRequest.newBuilder().GET()
                        .uri(URI.create(String.format(
                                "https://www.myedio.com/api/v1/events?" + "endDate=%s&eventKinds=%s&"
                                        + "includeType=true&startDate=%s&userIds=%d",
                                endTime.format(DateTimeFormatter.ISO_INSTANT), EventKinds.delimited(kinds, ","),
                                startTime.format(DateTimeFormatter.ISO_INSTANT), studentId)))
                        .header("Content-type", appJson).setHeader("authority", authority)
                        .setHeader("referer", refererDashboard).setHeader("sec-ch-ua", sec_ch_ua)
                        .setHeader("sec-ch_ua-mobile", sec_ch_ua_mobile)
                        .setHeader("sec-ch-ua-platform", sec_ch_ua_platform)
                        .setHeader("sec-fetch-dest", sec_fetch_dest_empty).setHeader("sec-fetch-code", sec_fetch_code)
                        .setHeader("sec-fetch-site", sec_fetch_site_sameOrigin).setHeader("User-Agent", UserAgent)
                        .setHeader("cookie", cookies).build();
                HttpResponse response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                switch (response.statusCode()) {
                    case 200:
                        return EdioGson.toMap(response.body());
                    default:
                        throw new IOException("HTTP Code: " + response.statusCode());
                }
            }
        }
        return new HashMap<>();
    }

    public Map<String, Object> getUpcoming(final int studentId, final int numberOfDaysAhead) {
        final ZonedDateTime startTime = LocalDateTime.now().atZone(ZoneId.systemDefault()).withHour(0).withMinute(0)
                .withSecond(0).withZoneSameInstant(ZoneId.of("UTC"));

        final ZonedDateTime endTime = startTime.plusDays(numberOfDaysAhead + 1);

        if (connectIfNeeded()) {
            try {
                HttpRequest request = HttpRequest.newBuilder().GET()
                        .uri(URI.create(String.format(
                                "https://www.myedio.com/api/v1/calendars/dayusers?" + "endDate=%s&" + "startDate=%s&"
                                        + "userId=%d",
                                endTime.format(DateTimeFormatter.ISO_INSTANT),
                                startTime.format(DateTimeFormatter.ISO_INSTANT), studentId)))
                        .header("Content-type", appJson).setHeader("authority", authority)
                        .setHeader("referer", refererDashboard).setHeader("sec-ch-ua", sec_ch_ua)
                        .setHeader("sec-ch_ua-mobile", sec_ch_ua_mobile)
                        .setHeader("sec-ch-ua-platform", sec_ch_ua_platform)
                        .setHeader("sec-fetch-dest", sec_fetch_dest_empty).setHeader("sec-fetch-code", sec_fetch_code)
                        .setHeader("sec-fetch-site", sec_fetch_site_sameOrigin).setHeader("User-Agent", UserAgent)
                        .setHeader("cookie", cookies).build();

                HttpResponse response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                switch (response.statusCode()) {
                    case 200:
                        return combineResultObjects(getDayEvents(studentId, startTime.toLocalDate(),
                                new EventKinds[] { EventKinds.EK_CUSTOM }), EdioGson.toMap(response.body()));
                    case 401:
                        if (retries < maxRetries) {
                            authentication(AuthType.AT_LOGON);
                            return getUpcoming(studentId, numberOfDaysAhead);
                        }
                        break;
                    default:
                        throw new IOException("HTTP Code: " + response.statusCode());
                }
            } catch (IOException | InterruptedException ex) {
                Logger.getLogger(EdioAPI.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return new HashMap<>();
    }
}
