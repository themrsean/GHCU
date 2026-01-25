# GHCU
GitHub Classroom Utilities is a suite of utilities to assist in grading coding assignment submissions by extracting packages, creating .html grade reports, running CheckStyle, and other tasks.

## Utilities
The following utilities are currently implemented:
* Extract packages from the GitHub repository
* Generate a list of student package imports
* Generate HTML grade reports for each student submission
* Run CheckStyle on each student repository

### Menus
#### File
* **Open** - Sets the directory that contains the student repositories
* **Quit** - Closes the program
#### Edit
* **Add Ignored File** - Adds one or more files or directories to the ignore list when extracting packages
  * The ignored file is stored in the `data` folder as `ignored.txt` and can be manually updated
  * Each new entry to the ignored list will be on a separate line in the file
* **Load Configuration** - Loads a custom configuration file
  * The config file contains two paths, one on each line
  * The first path is a path to the `ignored.txt` file
  * The second path is a path to the `defaultHeader.txt` file that stores the default header for the HTML grading reports
    * The first line and the last two lines of the header must remain the same, but the rubric and any other desired information can be added or altered between them
#### Process
* **Extract** - Extracts the student packages from the GitHub repositories and stores them in a `submissions` folder in the same directory as the repositories
* **Imports** - Creates an `imports.txt` file that contains `import.*` statements for each student repository. This can be copied and pasted into a test suite or test driver to facilitate running tests
* **Grading Reports** - Generates HTML grade reports that pulls the defaultHeader, then inserts each source file listed in the **Files List** area into the report. It will also insert the student username and Full Name of the assignment into the report. The resulting file will be named `<short name><package>.html`
  * The reports will be stored into a `feedback` folder inside the same directory as the repositories
* **Run All** - Will run Extract, Imports, and Grading Reports in succession
#### Help
* **About** - The user manual. This thing.

### Other Features
* **CheckStyle** - If checked, when **Grading Reports** is run, a CheckStyle report will be generated for each package and the results will be injected into each student's report, deducting one point for each CheckStyle infraction, to a maximum of 15 points.
* **Short Name** - The prefix used for naming the grading report files
* **Full Name** - The full name of the assignment, injected into the top of each grading report
* **Files List** - A list of the files that will be included in the grading report
