# Edio
Commonwealth Charter Academy (CCA) Edio "enhacnments

This is a set of java classes, programs and servlets used to gether information from the CCA Edio about student schedule. I wrote it for two reasons:

  1) I wanted a way to determine if students have school or not. I need it to be accessable by my home automation system in OpenHAB for the purpose of turnning on/off alarms,        reminders, etc.)
  2) We needed a way to see the next so many days worth of assignments, tests and quizzes. Edio does not currently show this.

+ EdioApi contains the java classes used to link to the Edio system using the coache's login credentials
+ EdioAPIWeb contains a java servlet to be used with Tomcat webserver providing a page showing student information
+ EdioCLI is a java command line JAR for access the same student information as the java servlet. This is usefull in automation systems like OpenHAB

The CLI is launched with the following command:
  java edio-cli.jar [path_to_config file]

The servlet contains a WAR file that must be deployed to a java servlet capable web server such as Tomcat. The config file must be placed under the 
root of the deployed servlet in a directory named data and the file must be called edioapi-config.json

Items retrieve:
  + overdue item(s): items that are marked overdue
  + school status: determins if today is a school day or not
  + upcoming: student entered calendar item(s), test(s), quiz(es) and due dates that are coming up
  + events: items like field trips, not classes.
 
The config file's contents are as follows:

  {
    "output":{
      "overdue":true,   /* true to retrieve overdue items, false otherwise */
      "children":[
        /* list of children's names you want to retrieve. Leave empty to retrieve all. */
      ],
      "school":true,    /* true to retrieve if the student has school today, false otherwise */
      "events_parms":{
        "date":""       /* date to retrieve events for or blank for today. */
      },
      "upcoming":true,  /* true to retrieve upcoming quizes, tests and due items, false otherwise */
      "events":true,    /* true to retrieve todays events, false otherwise */
      "upcoming_parms":{
        "days":14       /* how many days worth of upcoming events to retrieve */
      }
    },
    "edio":{
      "credentials":{ /* credentials used to log into the edio system as assigned by CCA */
        "pass":"",    /* coache's password */
        "user":""     /* coache's userid */
      }
    }
  }
  
If using the cli, the output is a JSON string similiar to the following:

  {
    "school":true,
    "children":[
      {
        "overdues":[],
        "name":"Bob",
        "id":134656,
        "upcoming":[
          {
            "date":"2012-01-10T00:00:00-05:00",
            "course":"Science",
            "topic":"Quiz: Interactions of Forces and Mass"},
          {
            "date":"2012-01-13T00:00:00-05:00",
            "course":"Math",
            "topic":"Independent Practice and Quiz"
          },
          {
            "date":"2012-01-14T00:00:00-05:00",
            "course":"History",
            "topic":"Chapter 5 Section 1 Quiz"
          },
          {
            "date":"2012-01-15T00:00:00-05:00",
            "course":"English Language Arts",
            "topic":"Act II Quiz"
          },
          {
            "date":"2012-01-16T00:00:00-05:00",
            "course":"English Language Arts",
            "topic":"Researching the Holocaust: Body Paragraphs Due"
          },
          {
            "date":"2012-01-20T00:00:00-05:00",
            "course":"English Language Arts",
            "topic":"Researching the Holocaust: Creating Citations and Conclusion Due"
          }
        ]
        "events":[]
      }
    ],
  "errors":[]
}
