import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;
import com.davisengeler.HardDrive;

public class Parser {

    static int currentTrack = 0;
    static int currentSector = 0;
    static int armStatus = 0;

    public static void main(String[] args) {

        // Set up some initial stuff
        int numTracks = 10;
        int numSectorsPerTrack = 10;
        HardDrive hdd = new HardDrive(numTracks, numSectorsPerTrack);

        // Scan in the requests
        ArrayList<ArrayList<String>> commandBlasts = new ArrayList<ArrayList<String>>();
        Scanner scan;
        try {
            scan = new Scanner(new File("test.txt"));
            while (scan.hasNextLine()) {
                scan.nextLine(); // Get rid of equal sign separators
                ArrayList<String> commands = new ArrayList<String>();
                commandBlasts.add(commands);
                for (int i = 0; i < 32; i++) {
                    if (scan.hasNextLine())
                        commands.add(scan.nextLine());
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println(e.getMessage());
        }

        // Section the micro code sections by "blasts."
        ArrayList<ArrayList<String>> microCodeBlasts = new ArrayList<ArrayList<String>>();
        for ( ArrayList<String> commands : commandBlasts) {
            // Make a new micro code blast.
            System.out.println("Command Blast:");
            ArrayList<String> microCode = new ArrayList<String>();
            microCodeBlasts.add(microCode);
            for (String command : commands) {
                String split[] = command.split(" ");
                command = split[0].toLowerCase();
                // Can't use a switch for Strings?
                // Determine what should be done for each one.
                // TODO: This is currently unoptimized microcode.
                if (command.equals("seek")){

                    // For SEEK
                    int parameter = Integer.parseInt(split[1]);
                    int desiredTrack = parameter / 10;
                    int desiredSector = parameter % 10;
                    System.out.println("Seek to track " + desiredTrack + ", sector " + desiredSector + ".");

                    // Set the correct ARM status
                    if (currentTrack > desiredTrack) {
                        microCode.add("arm 1");
                        armStatus = 1;
                    } else if (currentTrack < desiredTrack) {
                        microCode.add("arm -1");
                        armStatus = -1;
                    } else armStatus = 0;

                    // Add correct number of idles
                    boolean seeking = true;
                    while (seeking) {
                        if (currentTrack == desiredTrack && currentSector == desiredSector)
                            seeking = false;
                        else {
                            microCode.add("idle");
                        }
                    }

                } else {
                    System.out.println(command);
                }

//
            }
            System.out.println("");
        }
    }

    private static void spin() {
        // Simulate spin
        currentSector = (currentSector + 1) % 10;
        currentTrack = (currentTrack + armStatus);
        if (currentTrack > 10) currentSector = 10;
        else if (currentTrack < 0) currentSector = 0;
    }
}
