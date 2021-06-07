# Summary
Simple Command-Line Utility that transfers the lastModified
and created date/time information from an entire directory
tree to the directory and subdirectories of a duplicate copy.

This is useful if a copy utility (like DoubleCommander) only
preserves the date/time information on files but creates
the directories in the target copy with the current system
time.

# Details
* Runs **AFTER** the copy is complete.
* Can be run without the **-commitMode true** parameter in order
to test to see what would happen first.
* Skips over any directory pairs where the lastModified and
created dates already match.

# Building and Using
* Clone this repository
* Run **mvn clean package** in the main/base directory of the repo
* Unzip the distribution zip archive from the Maven build
target directory into an app directory somewhere.
  * e.g. (on Windows) **unzip -d c:\javaapps\copieddirdatefixer target\CopiedDirDateFixer...distribution.zip**
    * Note: Use the actual name of the zip file created by the Maven build.
* Run the jar in JRE/Java
  * Option 1 - Full paths
     * Test... **java -jar c:\javaapps\copieddirdatefixer\CopiedDirDateFixer.jar
     --sourceBaseDirectoryPath d:/orig_dir_w_correct_dates
     --targetBaseDirectoryPath g:/copy_of_dir_w_wrong_dates**
     * Do it... **java -jar c:\javaapps\copieddirdatefixer\CopiedDirDateFixer.jar
     --commitMode true
     --sourceBaseDirectoryPath d:/orig_dir_w_correct_dates
     --targetBaseDirectoryPath g:/copy_of_dir_w_wrong_dates**     
  * Option 2 - Drives and a common subdirectory (Windows only)
     * Test... **java -jar c:\javaapps\copieddirdatefixer\CopiedDirDateFixer.jar
     --sourceDrive d --targetDrive g --subdirectory subdirectory_named_alike**
     * Do it... **java -jar c:\javaapps\copieddirdatefixer\CopiedDirDateFixer.jar
     --commitMode true 
     --sourceDrive d --targetDrive g --subdirectory subdirectory_named_alike**
* Note: There are also single-letter short names for each param/option.  Run
without arguments to show usage info.
  * **java -jar c:\javaapps\copieddirdatefixer\CopiedDirDateFixer.jar**               