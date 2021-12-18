/*
 * Copyright (C) 2021 - Wolfe Computer Services
 * ALL RIGHTS RESERVED
 */

package com.wolfecomputerservices.edioapi;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import com.google.gson.Gson;
import com.wolfecomputerservices.edioapi.objects.Configuration;
import com.wolfecomputerservices.edioapi.objects.Event;
import com.wolfecomputerservices.edioapi.objects.ExecutorOutput;
import com.wolfecomputerservices.edioapi.objects.Overdue;
import com.wolfecomputerservices.edioapi.objects.Student;
import com.wolfecomputerservices.edioapi.objects.Upcoming;

/**
 *
 * @author Wolfe Computer Services - Initial contribution
 *
 **/
@NonNullByDefault
public class Edio implements AutoCloseable {
    // Nullables
    private static final Logger logger = Logger.getLogger(Edio.class.getName());
    private @Nullable EdioAPI edioAPI;

    private Configuration config;

    private final Gson gson = new Gson();

    /**
     * Create an instance Edio.class
     * 
     * @param jsonConfig JSONObject containing the run parameters
     */
    public Edio(String jsonConfig) {
        Object object = gson.fromJson(jsonConfig, Configuration.class);
        this.config = object == null ? new Configuration() : (Configuration) object;
    }

    /**
     * Create an instance of Edio.class
     * 
     * @param configFilePath Path to a file containing run parameters
     * @throws java.io.FileNotFoundException
     */
    public Edio(Path configFilePath) throws FileNotFoundException, IOException {
        File file = configFilePath.toFile();
        byte[] data;
        try (FileInputStream fis = new FileInputStream(file)) {
            data = new byte[(int) file.length()];
            fis.read(data);
        }

        String jsonString = new String(data, "UTF-8");
        Object object = gson.fromJson(jsonString, Configuration.class);
        this.config = object == null ? new Configuration() : (Configuration) object;
        logger.config(String.format("Parameters loaded from file %s: %s", file.getName(), jsonString));

        edioAPI = new EdioAPI(this.config.edio.credentials);
    }

    /**
     * Create instance of Edio.class
     * 
     * @param credentials credentials for Edio
     * @param upcoming_days number of days to read ahead for "upcomings"
     * 
     *            This constructor is typically used by openHAB bindings.
     */
    public Edio(Configuration.Edio.Credentials credentials, int upcoming_days) {
        this.config = new Configuration();

        this.config.edio.credentials = credentials;
        this.config.output.upcoming_days = upcoming_days;
    }

    /**
     * Returns any overdue assignments, tests and/or quizzes
     * 
     * @param studentName The student user to get getOverdues for
     * @return JSONArray containing 0...n items
     */
    public Overdue[] getOverdues(String studentName) {
        return Arrays.stream(executor(false, false, true, false).students)
                .filter(student -> student.getName().equalsIgnoreCase(studentName)).findFirst()
                .map(student -> student.getOverdues()).orElse(new Overdue[0]);
    }

    /**
     * Returns any upcoming, non-class, events. This includes any student
     * entered events.
     * 
     * @param studentName The student to get upcoming events for
     * @return JSONArray of 0...n items
     */
    public Upcoming[] getUpcomingEvents(String studentName) {
        return Arrays.stream(executor(false, false, true, false).students)
                .filter(student -> student.getName().equalsIgnoreCase(studentName)).findFirst()
                .map(student -> student.getUpcomings()).orElse(new Upcoming[0]);
    }

    /**
     * Return the students associated with coach's account
     * 
     * @return JSONArray of 0...n items.
     */
    public List<Student> getStudents() {
        return (List<Student>) Transformers
                .getMapValueAsArrayList(Transformers.getMapValueAsMap(edioAPI.getStudents(), "resultObject"),
                        "primaryUserRoleRelationships")
                .stream().map(s -> (Map<String, Object>) s).map(s -> Transformers.getMapValueAsMap(s, "secondaryUser"))
                .map(s -> new Student(Transformers.getMapValueAsInt(s, "id"),
                        Transformers.getMapValueAsString(s, "firstName")))
                .collect(Collectors.toList());
    }

    /**
     * Get list of students as a map
     * 
     * @return Map containing 0...n students.
     */
    public Map<String, Student> getStudentsAsMap() {
        return getStudents().stream().parallel().collect(Collectors.toMap(Student::getName, s -> s));
    }

    /**
     * Determines if there is school today.
     * 
     * @param date Date to check or null for today
     * @return True if there is school today, false if not.
     */
    public boolean hasSchool(LocalDate date) {
        LocalDate dateToUse = date == null ? LocalDate.now() : date;
        switch (dateToUse.getDayOfWeek()) {
            case SATURDAY:
            case SUNDAY:
                return false;
            default:
                try {
                    return Transformers
                            .getMapValueAsValues(edioAPI.getDayEvents(Integer.MIN_VALUE, dateToUse), "resultObject")
                            .stream()
                            .filter(s -> Transformers.getMapValueAsString((Map<String, Object>) s, "description")
                                    .toLowerCase().contains("no school "))
                            .count() == 0;
                } catch (IOException | InterruptedException ex) {
                    logger.log(Level.SEVERE, "Edio.java::hasSchool()", ex);
                    return false;
                }
        }
    }

    /**
     * Determines if there are overdue assignments, quizzes and/or test.
     * 
     * @param studentName The student user to make the determination for.
     * @return True if studentName has getOverdues, false if not.
     */
    public boolean hasOverdues(String studentName) {
        return getOverdues(studentName).length > 0;
    }

    /**
     * Returns all available data
     * 
     * @return JSONObject containing data.
     */
    public ExecutorOutput executor() {
        return executor(config.output.school, config.output.overdue, config.output.upcoming, config.output.events);
    }

    /**
     * Locates the JSONObject in students with user of studentName
     * 
     * @param studentName The user of the student to find
     * @param students the student list to search
     * @return the JSONObject if found, null otherwise.
     */
    public Student findStudent(String studentName, List<Student> students) {
        return students.stream().filter(student -> student.getName().equalsIgnoreCase((studentName))).findFirst()
                .orElse(new Student());
    }

    public boolean disconnect() {
        return edioAPI == null ? true : edioAPI.disconnect();
    }

    /**
     * Returns the data specified by the parameters.
     * 
     * @param doSchool If true, has school indicator will be returned
     * @param doOverdue if true, overdue items will be returned
     * @param doUpcoming if true, upcoming events will be returned
     * @param doEvents if true, events for today will be returned
     * @return
     */
    public ExecutorOutput executor(boolean doSchool, boolean doOverdue, boolean doUpcoming, boolean doEvents) {
        ExecutorOutput output = new ExecutorOutput();
        List<String> outErrors = new ArrayList<>();

        if (edioAPI == null)
            return requiresSetup(output);

        final Student[] requestedStudents = getStudentsAsMap().values().toArray(new Student[0]);
        /*
         * .parallelStream()
         * .filter(student->contains(config.output.students, student.getName()))
         * .collect(Collectors.toList())
         * .toArray(new Student[0]);
         */
        final LocalDateTime dateToInspect = LocalDateTime.now();
        if (doSchool)
            output.school = hasSchool(dateToInspect.toLocalDate());

        List<Student> outStudents = new ArrayList<>();
        for (int i = 0; i < requestedStudents.length; ++i) {
            final Student requestedStudent = requestedStudents[i];
            final String newStudentName = requestedStudent.getName();
            final int newStudentId = requestedStudent.getId();
            List<Overdue> newStudentOverdues = new ArrayList<>();
            if (doOverdue) {
                newStudentOverdues = Transformers
                        .getMapValueAsArrayList(edioAPI.getOverdues(newStudentId), "resultObject").parallelStream()
                        .map((u) -> new Overdue(
                                Transformers.getMapValueAsString((Map<String, Object>) u, "scheduledDate"),
                                Transformers.getMapValueAsString(Transformers.getMapValueAsMap(
                                        Transformers.getMapValueAsMap((Map<String, Object>) u, "day"), "course"),
                                        "name"),
                                Transformers.getMapValueAsString((Map<String, Object>) u, "name")))
                        .sorted((s1, s2) -> s1.dueDate.compareTo(s2.dueDate)).collect(Collectors.toList());
            }

            List<Upcoming> newStudentUpcoming = new ArrayList<>();
            if (doUpcoming) {
                final String patternString = "\\b(quiz|test|due)\\b";
                Pattern pattern = Pattern.compile(patternString);
                newStudentUpcoming = Transformers
                        .getMapValueAsArrayList(edioAPI.getUpcoming(newStudentId, config.output.upcoming_days),
                                "resultObject")
                        .parallelStream().map(s -> {
                            Map<String, Object> map = (Map<String, Object>) s;
                            final String courseName = Transformers.getMapValueAsString(
                                    Transformers.getMapValueAsMap(Transformers.getMapValueAsMap(map, "day"), "course"),
                                    "name");
                            final String assignment = Transformers
                                    .getMapValueAsString(Transformers.getMapValueAsMap(map, "day"), "name");
                            final String date = Transformers.getMapValueAsString(map, "scheduledDate");

                            return new Upcoming(date, courseName, assignment);
                        }).filter(s -> {
                            Matcher matcher = pattern.matcher(s.topic.toLowerCase());
                            return matcher.find();
                        }).sorted((s1, s2) -> s1.date.compareTo(s2.date)).collect(Collectors.toList());
            }

            List<Event> newStudentEvents = new ArrayList<>();
            if (doEvents) {
                try {
                    newStudentEvents = Transformers
                            .getMapValueAsArrayList(edioAPI.getDayEvents(newStudentId, LocalDate.now()), "resultObject")
                            .parallelStream().map(s -> {
                                Map<String, Object> map = (Map<String, Object>) s;
                                final int id = Transformers.getMapValueAsInt(map, "id");
                                final String createdOn = Transformers.getMapValueAsString(map, "createdOn");
                                final String startsOn = Transformers.getMapValueAsString(map, "startsOn");
                                final String endsOn = Transformers.getMapValueAsString(map, "endsOn");
                                final String name = Transformers.getMapValueAsString(map, "name");
                                final String description = Transformers.getMapValueAsString(map, "description");
                                return new Event(id, createdOn, startsOn, endsOn, name, description);
                            }).sorted((e1, e2) -> e1.dateStart.compareTo(e2.dateStart)).collect(Collectors.toList());
                } catch (IOException | InterruptedException ex) {
                    logger.log(Level.SEVERE, null, ex);
                    outErrors.add("Something WENT WRONG determining EVENTS: " + ex.getMessage());
                }
            }

            outStudents.add(new Student(newStudentId, newStudentName, newStudentOverdues, newStudentEvents,
                    newStudentUpcoming));
        }

        output.students = outStudents.toArray(new Student[0]);

        return finalizeOutput(output, outErrors);
    }

    /**
     * Adds indicator saying setup is required to the output
     * 
     * @param output The current output JSONObject
     * @return The output JSONObject with indicator added
     */
    private ExecutorOutput requiresSetup(ExecutorOutput output) {
        output.setup_required = true;

        return output;
    }

    /**
     * Package any errors into the output in preparation for returning
     * 
     * @return the prepared output
     */
    private ExecutorOutput finalizeOutput(ExecutorOutput output, List<String> outErrors) {
        output.errors = outErrors.toArray(new String[0]);
        return output;
    }

    @Override
    public void close() throws IOException {
        disconnect();
    }
}
