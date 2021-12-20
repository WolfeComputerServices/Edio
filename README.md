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

```json
  {
    "edio":{
      "credentials":{ /* credentials used to log into the edio system as assigned by CCA */
        "pass":"",    /* coach's password */
        "user":""     /* coach's userid */
      }
    },
    "output":{
      "overdue":true,   /* true to retrieve overdue items, false otherwise */
      "students":[
        /* list of student's names you want to retrieve. Leave empty to retrieve all. */
      ],
      "school":true,      /* true to retrieve if the student has school today, false otherwise */
      "upcoming":true,    /* true to retrieve upcoming quizes, tests and due items, false otherwise */
      "events":true,      /* true to retrieve todays events, false otherwise */
      "upcoming_days":14  /* how many days worth of upcoming events to retrieve */
    }
  }
```

If using the cli, the output is a JSON string similiar to the following:

```json
  {
    "setup_required":false,   /* if true, the configuration file (see above) is incorrect */
    "school":true,            /* true|false */ if students have school today */
    "students":[              /* list of enrolled students */
      {
        "id":99999,
        "name":"Billy Bob Smith",
        "overdues":[],      /* list of overdue quizz(es), test(s) and due assignments */
        "events":[],        /* list of non-class related events in the next x (as defined by configuration file output.upcoming_days parameter) days.
        "upcomings":[
          {
            "date":{
              "iso":"2019-12-20",
              "year":2019,
              "month":12,
              "day":20
            },
            "course":"English Language Arts",
            "topic":"Researching the Holocaust: Creating Citations and Conclusion Due"
          },
          {
            "date":{
              "iso":"2019-12-21",
              "year":2019,
              "month":12,
              "day":21
            },
            "course":"Math",
            "topic":"END OF UNIT TEST"
          },
          {
            "date":{
              "iso":"2020-01-04",
              "year":2020,
              "month":1,
              "day":4
            },
            "course":"History",
            "topic":"Section 3 Quiz"
          }
        ]
      }
    ],
    "events":[],
    "errors":[]
  }

```

Ideas for future enhancement:

  + Add ability to propigate to Google gmail (and possibly others). The work is started as it generats iCal format for events already.
