/* 
 * Copyright (C) 2021 - Wolfe Computer Services
 * ALL RIGHTS RESERVED
 */
$(document).ready(function() {
    $.ajax({
        type: 'GET',
        url: 'edio.do',
        dataType: 'json',
    })
    .done(function(data, textStatus, jqXHR) {
        if (data.setup_required === true) {
            document.location.replace('edio.cfg');
        } else {
            $('body').html("<div>" 
                    + 
                    displayStudents(data.students)
                    + "</div>");
        }
    });

    function studentHead(student) {
        return "<div class='border-primary text-primary white'><h1>" 
            + student.name 
            + "</h1></div><br/><blockquote>";
    }
    function overdueHead() {
        return "<div class='p-3 mb-2 bg-warning text-dark'><h2>Overdue</h2></div>"
            + "<table class='table table-striped'><thead><tr><th>Due Date</th><th>Course</th><th>Assignment/Test</th></tr></thead>"
            + "<tbody>";
    }
    function overdueEntry(overdues) {
        return "<tr><td>" + overdues.dueDate.substring(0,10)+"</td><td> "+
            overdues.course+"</td><td><b>"+
            overdues.assignment+"</b></td></tr>";				   
    }
    function overdueFoot() {
        return "</tbody></table><br/><br/>";
    }
    
    function upcomingHead() {
        return "<div class='container mt-3'>"
            + "<h2>Upcoming...</h2>"
            + "<table class='table table-striped'><thead><tr><th>Due Date</th><th>Course</th><th>Assignment/Test</th></tr></thead>"
            + "<tbody>";
    }
    
    function upcomingEntry(upcoming) {
        return "<tr>"
            + "<td>" + upcoming.date.iso.substring(0,10) + "</td><td> "
            + upcoming.course + "</td><td><b>"
            + upcoming.topic + "</b></td></tr>";
    }
    
    function upcomingFoot() {
        return "</tbody></table></blockquote><br><br>";
    }

    function displayStudentOverdues(overdues) {
        if (overdues) {
            var entries = overdueHead();
            $.each(overdues, (index, overdue) => {
                entries += overdueEntry(overdue);
            })
            return entries + overdueFoot();
        }

        return "No overdue items";
    }
    function displayStudentUpcoming(upcomings) {
        var entries = upcomingHead();
        $.each(upcomings, (index, upcoming) => {
            entries += upcomingEntry(upcoming);
        });
        return entries + upcomingFoot();
    }
    function displayStudentData(student) {
        return studentHead(student)
            + displayStudentOverdues(student.overdues)
            + displayStudentUpcoming(student.upcomings);
    }
    
    function displayStudents(student) {
        var entries = "<div class='container mt-3'>";
        $.each(student, (index, student) => {
            entries += displayStudentData(student);
        });
        return entries;
    }
})