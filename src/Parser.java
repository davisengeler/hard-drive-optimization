import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import com.davisengeler.HardDrive;
import com.davisengeler.HardDrive.CommandType;

public class Parser {

    // Set up some initial stuff
    static int numTracks = 10;
    static int numSectorsPerTrack = 10;
    static int currentTrack = 0;
    static int currentSector = 0;
    static int armStatus = 0;
    static HardDrive hdd = new HardDrive(numTracks, numSectorsPerTrack);

    public static void main(String[] args)
    {
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

        // Divide the micro code into sections for each 'blast.'
        ArrayList<ArrayList<Command>> microCodeBlasts = new ArrayList<ArrayList<Command>>();
        for ( ArrayList<String> commandStrings : commandBlasts)
        {
            // For each group of commands in each 'command blast.'
            // Make a new micro code blast.
            System.out.println("\n====================\n");
            ArrayList<Command> microCode = new ArrayList<Command>();
            microCodeBlasts.add(microCode);

            // Some organization techniques to attempt optimizations
            HashMap<String, ArrayList<Command>> readWrite = new HashMap<String, ArrayList<Command>>();

            for (String commandString : commandStrings)
            {
                // For each individual commands inside a command blast.
                // This section converts individual commands into their unoptimized microcode.
                String split[] = commandString.split(" ");
                commandString = split[0].toLowerCase();

                // Can't use a switch for Strings?
                // Determine what should be done for each one.
                // TODO: This is currently unoptimized microcode.
                if (commandString.equals("seek")) {
                    // SEEK
                    int parameter = Integer.parseInt(split[1]);
                    int desiredTrack = parameter / numTracks;
                    int desiredSector = parameter % numSectorsPerTrack;
//                    System.out.println("Seek to track " + desiredTrack + ", sector " + desiredSector + ".");

                    // Set the correct ARM status
                    if (currentTrack < desiredTrack) {
                        microCode.add(new Command(CommandType.arm, 1));
//                        System.out.println("arm 1");
                        armStatus = 1;
                        spin();
                    } else if (currentTrack > desiredTrack) {
                        microCode.add(new Command(CommandType.arm, -1));
//                        System.out.println("arm -1");
                        armStatus = -1;
                        spin();
                    }

                    // Add correct number of idles
                    boolean seeking = true;
                    while (seeking) {
                        if (currentTrack == desiredTrack && armStatus != 0) {
//                            System.out.println("arm 0");
                            microCode.add(new Command(CommandType.arm, 0));
                            armStatus = 0;
                            spin();
                        }
                        if (currentTrack == desiredTrack && currentSector == desiredSector){
                            seeking = false;
//                            System.out.println("Currently over track " + currentTrack + ", sector " + currentSector + ".");
                        }
                        else {
                            microCode.add(new Command(CommandType.idle));
//                            System.out.println("idle");
                            spin();
                        }
                    }
                }
                else if (commandString.equals("read")) {
                    // READ
                    int readTimes = Integer.parseInt(split[1]);
                    for (int i = 0; i < readTimes; i++) {
//                        System.out.println("read track " + currentTrack + ", sector " + currentSector + ".");
                        microCode.add(new Command(CommandType.read));

                        // Store some information for optimization
                        Command readCommand = new Command(CommandType.read);
                        if (!readWrite.containsKey(readCommand.sectorID))
                            readWrite.put(readCommand.sectorID, new ArrayList<Command>());
                        readWrite.get(readCommand.sectorID).add(readCommand);

                        // Simulate a spin
                        spin();
                    }
                }
                else if (commandString.equals("write")) {
                    // WRITE
                    for (int i = 1; i < split.length; i++)
                    {
                        int value = Integer.parseInt(split[i]);
//                        System.out.println("write value " + value + " to track " + currentTrack + ", sector " + currentSector);
                        microCode.add(new Command(CommandType.write, value));

                        // Store some information for optimization
                        Command writeCommand = new Command(CommandType.write, value);
                        if (!readWrite.containsKey(writeCommand.sectorID))
                            readWrite.put(writeCommand.sectorID, new ArrayList<Command>());
                        readWrite.get(writeCommand.sectorID).add(writeCommand);

                        // Simulate a spin
                        spin();
                    }
                }
                else System.out.println("PROBLEM: Undefined user command '" + commandString + "'");
            }
            System.out.println("");

            // At this point, 'microCode' holds all of the unoptimized commands for this blast.
            // So let's replace the content of 'microCode' with its optimized version.
            microCode = optimize(microCode, readWrite);

            // Print out the optimized microcode.
            for (Command currentCommand : microCode) {
//                hdd.command(currentCommand.type, currentCommand.param);
                System.out.println(currentCommand.toString().toUpperCase());
            }
        }
    }

    private static ArrayList<Command> optimize(ArrayList<Command> unoptimized, HashMap<String, ArrayList<Command>> readWriteRelationships) {
        ArrayList<Command> optimized = new ArrayList<Command>();
        HashMap<Integer, ArrayList<Integer>> systemRead = new HashMap<Integer, ArrayList<Integer>>();
        HashMap<Integer, Integer> systemWrite = new HashMap<Integer, Integer>();


        // TODO: This is just returning the unoptimized code for now.
        return unoptimized;
    }

    // Command class to make things clean and readable.
    private static class Command
    {
        CommandType type;
        boolean hasParam = false;
        int param = 0;
        String sectorID = "00";
        public Command(CommandType type) {
            // For non-param commands
            this.type = type;
            this.sectorID = "" + currentTrack + currentSector;
//            System.out.println(type.toString() + "()");
        }
        public Command(CommandType type, Integer param) {
            // For commands with param.
            hasParam = true;
            this.type = type;
            this.param = param;
            this.sectorID = "" + currentTrack + currentSector;
//            System.out.println(type.toString() + "(" + param + ")");
        }
        public void setSectorID (String newSectorID) {
            this.sectorID = newSectorID;
        }
        public String toString() { return type.toString() + (hasParam ? (" " + param) : ""); }
    }

    private static void spin() {
        // Simulate spin
        currentSector = (currentSector + 1) % numSectorsPerTrack;
        currentTrack = (currentTrack + armStatus);
        if (currentTrack > numTracks) currentTrack = numTracks;
        else if (currentTrack < 0) currentTrack = 0;
//        System.out.println("Head is currently on track " + currentTrack + " over sector " + currentSector);
    }
}
