package org.liveintellect.standaloneapps.copieddirdatefixer;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.UnrecognizedOptionException;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CopiedDirDateFixer {

    private static final String OPTION_LONG_SOURCE_BASE_DIRECTORY_PATH = "sourceBaseDirectoryPath";
    private static final String OPTION_SHORT_SOURCE_BASE_DIRECTORY_PATH = "s";
    private static final String OPTION_LONG_TARGET_BASE_DIRECTORY_PATH = "targetBaseDirectoryPath";
    private static final String OPTION_SHORT_TARGET_BASE_DIRECTORY_PATH = "t";

    /**
     * This option set only makes sense in Windows where drives have letters, but it
     * allows repeated commands to be edited more easily when working through directories
     * at the root level of two drives.
     */
    private static final String OPTION_LONG_SOURCE_DRIVE = "sourceDrive";
    private static final String OPTION_SHORT_SOURCE_DRIVE = "a";
    private static final String OPTION_LONG_TARGET_DRIVE = "targetDrive";
    private static final String OPTION_SHORT_TARGET_DRIVE = "b";
    private static final String OPTION_LONG_SUBDIRECTORY_NAME = "subdirectoryName";
    private static final String OPTION_SHORT_SUBDIRECTORY_NAME = "n";

    private static final String OPTION_LONG_COMMIT_MODE = "commitMode";
    private static final String OPTION_SHORT_COMMIT_MODE = "c";

    private final File sourceBaseDirectoryPath;
    private final File targetBaseDirectoryPath;
    private final boolean commitMode;

    private static final String TARGET_DATE_STRING_TEMPLATE = "yyyyMMdd-HHmmss";
    // Be careful not to use this format object in multi-threaded code - it is NOT threadsafe
    private static final DateFormat TARGET_DATE_STRING_FORMAT = new SimpleDateFormat(
            TARGET_DATE_STRING_TEMPLATE);

    /**
     * Params used to specify the full path to the source and the full path to the
     * target.
     *
     * @param sourceBaseDirectoryPath full path to the source directory
     * @param targetBaseDirectoryPath full path to the target directory
     * @param commitMode
     */
    public CopiedDirDateFixer(File sourceBaseDirectoryPath,
            File targetBaseDirectoryPath, boolean commitMode) {
        this.sourceBaseDirectoryPath = sourceBaseDirectoryPath;
        System.out.println("sourceBaseDirectoryPath = " + sourceBaseDirectoryPath);
        this.targetBaseDirectoryPath = targetBaseDirectoryPath;
        System.out.println("targetBaseDirectoryPath = " + targetBaseDirectoryPath);
        this.commitMode = commitMode;
    }

    /**
     * Alternate set of params that are used to build the same directory path
     * from two "drive letters" on windows.  Most other filesystems use path
     * mount points so this probably ONLY makes sense in windows.
     * @param sourceDriveLetter just the letter of the source drive (case doesn't seem to matter)
     * @param targetDriveLetter just the letter of the source drive (case doesn't seem to matter)
     * @param subdirectoryName the name of the subdirectory to use on both source and target drives
     * @param commitMode true if the changes should actually be made
     */
    public CopiedDirDateFixer(String sourceDriveLetter, String targetDriveLetter, String subdirectoryName,
                              boolean commitMode) {
        this(new File(sourceDriveLetter + ":/" + subdirectoryName),
                new File(targetDriveLetter + ":/" + subdirectoryName),
                commitMode);
    }

    private class DirectoryFilenameFilter implements FilenameFilter {
        @Override
        public boolean accept(File dir, String name) {
            File fileToFilter = new File(dir, name);
            return fileToFilter.isDirectory();
        }
    }

    public void fixTargetDirectoryDates() {
        fixTargetDirectoryDates(sourceBaseDirectoryPath, targetBaseDirectoryPath);
    }

    /**
     * Recursive call where:
     *   * if both directories exist, the
     * Uses File.setLastModified() to change the file's last modified date. If
     * that doesn't work... Uses touch -t to offset the file dates on all files
     * in the same directory as baseFile is found.
     */
    private void fixTargetDirectoryDates(File sourceDirectoryFile,
                                        File targetDirectoryFile) {
        if (sourceDirectoryFile.exists() && targetDirectoryFile.exists()) {
            try {
                Date sourceDirectoryLastModifiedDate = new Date(sourceDirectoryFile.lastModified());
                String sourceDirectoryLastModifiedDateText = TARGET_DATE_STRING_FORMAT.format(sourceDirectoryLastModifiedDate);
                BasicFileAttributes sourceDirectoryAttributes = Files.readAttributes(sourceDirectoryFile.toPath(), BasicFileAttributes.class);
                FileTime sourceDirectoryCreatedFileTime = sourceDirectoryAttributes.creationTime();
                Date sourceDirectoryCreatedDate = new Date(sourceDirectoryCreatedFileTime.toMillis());
                String sourceDirectoryCreatedDateText = TARGET_DATE_STRING_FORMAT.format(sourceDirectoryCreatedDate);

                Date targetDirectoryLastModifiedDate = new Date(targetDirectoryFile.lastModified());
                String targetDirectoryLastModifiedDateText = TARGET_DATE_STRING_FORMAT.format(targetDirectoryLastModifiedDate);
                BasicFileAttributes targetDirectoryAttributes = Files.readAttributes(targetDirectoryFile.toPath(), BasicFileAttributes.class);
                FileTime targetDirectoryCreatedFileTime = targetDirectoryAttributes.creationTime();
                Date targetDirectoryCreatedDate = new Date(targetDirectoryCreatedFileTime.toMillis());
                String targetDirectoryCreatedDateText = TARGET_DATE_STRING_FORMAT.format(targetDirectoryCreatedDate);

                boolean lastModMismatch = sourceDirectoryLastModifiedDate.getTime() != targetDirectoryLastModifiedDate.getTime();
                boolean createdMismatch = sourceDirectoryCreatedDate.getTime() != targetDirectoryCreatedDate.getTime();
                // Only report and/or change dates if something is actually different, otherwise leave it alone
                if (lastModMismatch || createdMismatch) {
                    System.out.println(targetDirectoryFile.getAbsolutePath() +
                            ((commitMode) ? " overwritten" : " mismatched") + " from file dates on source directory: " + sourceDirectoryFile.getAbsolutePath()
                            + ((lastModMismatch) ? "\n     lastModified - S: " + sourceDirectoryLastModifiedDateText
                                + " -> " + ((commitMode) ? "overwrites" : "would overwrite")
                                + " -> T: " + targetDirectoryLastModifiedDateText : "")
                            + ((createdMismatch) ? "\n     created - S: " + sourceDirectoryCreatedDateText
                                + " -> "  + ((commitMode) ? "overwrites" : "would overwrite")
                                + " -> T: " + targetDirectoryCreatedDateText : "")
                        );
                    // Overwrite the lastModified timestamp
                    if (commitMode) {
                        if (lastModMismatch)
                            targetDirectoryFile.setLastModified(sourceDirectoryLastModifiedDate.getTime());
                        if (createdMismatch)
                            Files.setAttribute(targetDirectoryFile.toPath(), "creationTime", FileTime.fromMillis(sourceDirectoryCreatedFileTime.toMillis()));
                    }
                } else {
                    System.out.println(targetDirectoryFile.getAbsolutePath() + " - Dates/times already match. Skipping.");
                }

            } catch (IOException ex) {
                ex.printStackTrace();
            }

        } else if (!sourceDirectoryFile.exists()) {
            System.out.println(targetDirectoryFile.getAbsolutePath() + "- No corresponding source directory");
        } else { // by elimination, targetDirectoryFile does not exist - if neither exists, how did we get here?
            System.out.println(" >> " + sourceDirectoryFile.getAbsolutePath() + " >> ? - No corresponding target directory");
        }

        // recursions for both source and target directory
        Set<String> alreadyDoneOnSourceRecursion = new HashSet<>();
        if (sourceDirectoryFile.exists()) {
            String[] subdirectoryFilenames = sourceDirectoryFile.list(new DirectoryFilenameFilter());
            for (String subdirectoryFilename : subdirectoryFilenames) {
                // call assuming there is a matching target for the source
                File recursedSourceDirectoryFile = new File(sourceDirectoryFile, subdirectoryFilename);
                File recursedTargetDirectoryFile = new File(targetDirectoryFile, subdirectoryFilename);  // this may not exist
                alreadyDoneOnSourceRecursion.add(subdirectoryFilename);
                fixTargetDirectoryDates(recursedSourceDirectoryFile, recursedTargetDirectoryFile);
            }
        }
        /*
         * Also recurse by looping the subdirectories in the targetDirectory.
         * Note: This is only here to report the targets for which there is no source, since processing
         * the subdirectory list in sources would have called fixTargetDirectoryDates for anything that
         * was actually part of a matching pair.
         */
        if (targetDirectoryFile.exists()) {
            String[] subdirectoryFilenames = targetDirectoryFile.list(new DirectoryFilenameFilter());
            for (String subdirectoryFilename : subdirectoryFilenames) {
                if (!alreadyDoneOnSourceRecursion.contains(subdirectoryFilename)) {
                    // call assuming there is a matching source for the target
                    File recursedTargetDirectoryFile = new File(targetDirectoryFile, subdirectoryFilename);
                    File recursedSourceDirectoryFile = new File(sourceDirectoryFile, subdirectoryFilename);  // this may not exist
                    fixTargetDirectoryDates(recursedSourceDirectoryFile, recursedTargetDirectoryFile);
                }
            }
        }

    }

    public static void main(String[] args) throws Throwable {
        Options pathsOptions = getPathsCommandLineOptions();
        Options drivesOptions = getDrivesCommandLineOptions();
        CommandLine pathsCli = null;
        try {
            pathsCli = new BasicParser().parse(pathsOptions, args);
        } catch(UnrecognizedOptionException ex) {
            // ignore - this could happen if the drive options are passed instead
            //System.out.println("Valid options for drivesCli may have caused this: "
            //        + ex.getMessage());
        }
        CommandLine drivesCli = null;
        try {
            drivesCli = new BasicParser().parse(drivesOptions, args);
        } catch (UnrecognizedOptionException ex) {
            // ignore this too when a mismatched combination of options is used
            // like -s with -n
            //System.out.println("A mixed set of options for drivesCli and pathsCli may have caused this: "
            //    + ex.getMessage());
        }
        if (pathsCli != null && pathsCli.hasOption(OPTION_SHORT_SOURCE_BASE_DIRECTORY_PATH)
                && pathsCli.hasOption(OPTION_SHORT_TARGET_BASE_DIRECTORY_PATH)) {
            CopiedDirDateFixer copiedDirDateFixer = new CopiedDirDateFixer(
                    new File(pathsCli.getOptionValue(OPTION_SHORT_SOURCE_BASE_DIRECTORY_PATH)),
                    new File(pathsCli.getOptionValue(OPTION_SHORT_TARGET_BASE_DIRECTORY_PATH)),
                    new Boolean(pathsCli.getOptionValue(OPTION_SHORT_COMMIT_MODE, "false")).booleanValue());
            copiedDirDateFixer.fixTargetDirectoryDates();
        } else if (drivesCli != null && drivesCli.hasOption(OPTION_SHORT_SOURCE_DRIVE)
                && drivesCli.hasOption(OPTION_SHORT_TARGET_DRIVE)
                && drivesCli.hasOption(OPTION_SHORT_SUBDIRECTORY_NAME)) {
            CopiedDirDateFixer copiedDirDateFixer = new CopiedDirDateFixer(
                    drivesCli.getOptionValue(OPTION_SHORT_SOURCE_DRIVE),
                    drivesCli.getOptionValue(OPTION_SHORT_TARGET_DRIVE),
                    drivesCli.getOptionValue(OPTION_SHORT_SUBDIRECTORY_NAME),
                    new Boolean(drivesCli.getOptionValue(OPTION_SHORT_COMMIT_MODE, "false")).booleanValue());
            copiedDirDateFixer.fixTargetDirectoryDates();
        } else {
            usage();
        }
    }

    private static void usage() {
        new HelpFormatter().printHelp(CopiedDirDateFixer.class.getSimpleName(),
                getPathsCommandLineOptions());
        new HelpFormatter().printHelp(CopiedDirDateFixer.class.getSimpleName(),
                getDrivesCommandLineOptions());
    }

    /**
     * Note OptionGroup combines multiple options such that only one of them
     * may be used.  The only way to group options such that a set
     * of them must be used together is to create multiple Options objects.
     * @return paths option set.
     */
    private static Options getPathsCommandLineOptions() {
        Options options = new Options();

        options.addOption(new Option(
                OPTION_SHORT_SOURCE_BASE_DIRECTORY_PATH,
                OPTION_LONG_SOURCE_BASE_DIRECTORY_PATH,
                true,
                "The full path to the base directory where the 'source' directories"
                        + ", with the correct file dates, are stored."));
        options.addOption(new Option(
                OPTION_SHORT_TARGET_BASE_DIRECTORY_PATH,
                OPTION_LONG_TARGET_BASE_DIRECTORY_PATH,
                true,
                "The full path to the base directory where the 'target' directories"
                        + ", with the incorrect file dates, are stored."));
        // common to both combinations
        options.addOption(new Option(
                OPTION_SHORT_COMMIT_MODE,
                OPTION_LONG_COMMIT_MODE,
                true,
                "true to indicate that directory dates in the target hierarchy should be altered. defaults to false"));
        return options;
    }

    /**
     * @return drives option set.
     */
    private static Options getDrivesCommandLineOptions() {
        Options options = new Options();
        options.addOption(new Option(
                OPTION_SHORT_SOURCE_DRIVE,
                OPTION_LONG_SOURCE_DRIVE,
                true,
                "The drive letter for the 'source' disk/volume."
                        + "  Used with the " + OPTION_LONG_SUBDIRECTORY_NAME + " parameter."));
        options.addOption(new Option(
                OPTION_SHORT_TARGET_DRIVE,
                OPTION_LONG_TARGET_DRIVE,
                true,
                "The drive letter for the 'target' disk/volume."
                        + "  Used with the " + OPTION_LONG_SUBDIRECTORY_NAME + " parameter."));
        options.addOption(new Option(
                OPTION_SHORT_SUBDIRECTORY_NAME,
                OPTION_LONG_SUBDIRECTORY_NAME,
                true,
                "subdirectory to use on both " + OPTION_LONG_SOURCE_DRIVE +
                        " and " + OPTION_LONG_TARGET_DRIVE));

        // common to both combinations
        options.addOption(new Option(
                OPTION_SHORT_COMMIT_MODE,
                OPTION_LONG_COMMIT_MODE,
                true,
                "true to indicate that directory dates in the target hierarchy should be altered. defaults to false"));
        return options;
    }

}
