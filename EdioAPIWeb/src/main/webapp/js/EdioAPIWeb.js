/* 
 * Copyright (C) 2021 - Wolfe Computer Services
 * ALL RIGHTS RESERVED
 */
$(document).ready(function() {
    $.ajax({
        type: 'GET',
        url: '/EdioAPIWeb/Edio',
        dataType: 'json',
    })
    .done(function(data, textStatus, jqXHR) {
        if (data.setup_required === true) {
            document.location.replace('/EdioAPIWeb/Setup');
        } else {
            $('body').html("<div>" 
                    + 
                    displayChildren(data.children)
                    + "</div>");
        }
    });

    function childHead(child) {
        return "<div class='border-primary text-primary white'><h1>" 
            + child.name 
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
            + "<td>" + upcoming.date.substring(0,10) + "</td><td> "
            + upcoming.course + "</td><td><b>"
            + upcoming.topic + "</b></td></tr>";
    }
    
    function upcomingFoot() {
        return "</tbody></table></blockquote><br><br>";
    }

    function displayChildOverdues(overdues) {
        if (overdues) {
            var entries = overdueHead();
            $.each(overdues, (index, overdue) => {
                entries += overdueEntry(overdue);
            })
            return entries + overdueFoot();
        }

        return "No overdue items";
    }
    function displayChildUpcoming(upcomings) {
        var entries = upcomingHead();
        $.each(upcomings, (index, upcoming) => {
            entries += upcomingEntry(upcoming);
        });
        return entries + upcomingFoot();
    }
    function displayChildData(child) {
        return childHead(child)
            + displayChildOverdues(child.overdues)
            + displayChildUpcoming(child.upcoming);
    }
    
    function displayChildren(children) {
        var entries = "<div class='container mt-3'>";
        $.each(children, (index, child) => {
            entries += displayChildData(child);
        });
        return entries;
    }
})