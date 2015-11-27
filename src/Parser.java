import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Array;
import java.util.*;

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
    static Stack stack = new Stack(5);

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
            System.out.println("====================");
            ArrayList<Command> microCode = new ArrayList<Command>();
            microCodeBlasts.add(microCode);

            // Reset the simulated hard drive for the optimized pathfinding and for the next set of user instructions.
            hdd.reset();
            currentSector = 0;
            currentTrack = 0;

            // Some organization techniques to attempt optimizations
            HashMap<Integer, ArrayList<Command>> sectorsReadWrite = new HashMap<Integer, ArrayList<Command>>();
            ArrayList<Command> readList = new ArrayList<Command>();

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

                    // Set the correct ARM status
                    if (currentTrack < desiredTrack) {
                        microCode.add(new Command(CommandType.arm, 1));
                        armStatus = 1;
                        spin();
                    } else if (currentTrack > desiredTrack) {
                        microCode.add(new Command(CommandType.arm, -1));
                        armStatus = -1;
                        spin();
                    }

                    // Add correct number of idles
                    boolean seeking = true;
                    while (seeking) {
                        if (currentTrack == desiredTrack && armStatus != 0) {
                            microCode.add(new Command(CommandType.arm, 0));
                            armStatus = 0;
                            spin();
                        }
                        if (currentTrack == desiredTrack && currentSector == desiredSector)
                            seeking = false;
                        else {
                            microCode.add(new Command(CommandType.idle));
                            spin();
                        }
                    }
                }
                else if (commandString.equals("read")) {
                    // READ
                    int readTimes = Integer.parseInt(split[1]);
                    for (int i = 0; i < readTimes; i++) {
                        Command readCommand = new Command(CommandType.read);
                        microCode.add(readCommand);

                        // Store some information for optimization
                        readList.add(readCommand);
                        if (!sectorsReadWrite.containsKey(readCommand.sectorID))
                            sectorsReadWrite.put(readCommand.sectorID, new ArrayList<Command>());
                        sectorsReadWrite.get(readCommand.sectorID).add(readCommand);

                        // Simulate a spin
                        spin();
                    }
                }
                else if (commandString.equals("write")) {
                    // WRITE
                    for (int i = 1; i < split.length; i++)
                    {
                        int value = Integer.parseInt(split[i]);
                        Command writeCommand = new Command(CommandType.write, value);
                        microCode.add(writeCommand);

                        // Store some information for optimization
                        if (!sectorsReadWrite.containsKey(writeCommand.sectorID))
                            sectorsReadWrite.put(writeCommand.sectorID, new ArrayList<Command>());
                        sectorsReadWrite.get(writeCommand.sectorID).add(writeCommand);

                        // Simulate a spin
                        spin();
                    }
                }
                else System.out.println("PROBLEM: Undefined user command '" + commandString + "'");
            }

            // The unoptimized code has been generated. Uncomment the following lines to see the pure conversion.
            System.out.println("Normal: " + microCode.size());
//            for (Command currentCommand : microCode) {
////                hdd.command(currentCommand.type, currentCommand.param);  // This is for use with my simulator. Not necessary.
//                System.out.println(currentCommand.toString().toUpperCase());
//            }

            // Now we can replace the content of 'microCode' with its optimized version and print out the optimized commands.
            microCode = optimize(microCode, sectorsReadWrite, readList);
            System.out.println("Optimized: " + microCode.size());
            for (Command currentCommand : microCode) {
//                hdd.command(currentCommand.type, currentCommand.param);  // This is for use with my simulator. Not necessary.
                System.out.println(currentCommand.toString().toUpperCase());
            }
        }
    }

    private static ArrayList<Command> optimize(ArrayList<Command> unoptimized,
                                               HashMap<Integer, ArrayList<Command>> sectorsReadWrite,
                                               ArrayList<Command> readList)
    {
        ArrayList<Command> optimized = new ArrayList<Command>();
        HashMap<Integer, Command> writeList = new HashMap<Integer, Command>();          // HashMap to efficiently find write for particular sector
        List<Command> writeOrder = new ArrayList<Command>();                   // 2D array list to attempt a sort

        // Reset the simulated hard drive for the optimized pathfinding and for the next set of user instructions.
        hdd.reset();
        currentSector = 0;
        currentTrack = 0;

        // Go through each sector's commands and figure out the minimum that needs to be done.
        for (int currentKey : sectorsReadWrite.keySet())
        {
            ArrayList<Command> sectorCommands = sectorsReadWrite.get(currentKey);
            Command lastWrite = null;  // This will hold the only write that needs to be done to this sector.
            for (int i = 0; i < sectorCommands.size(); i++)
            {
                Command currentCommand = sectorCommands.get(i);
                if (currentCommand.type == CommandType.write)
                    lastWrite = currentCommand;  // This will end holding the last write to the sector.
                else {
                    // This must be a read since this only holds reads and writes.
                    if (lastWrite != null) {
                        // If we have come across a write prior to this read, we already know what the value will be
                        // and can replace this it with a SYSTEM for the value of the last write to the sector.
                        int currentCommandIndex = readList.indexOf(currentCommand);
                        readList.set(currentCommandIndex, new Command(CommandType.system, lastWrite.param));
                    }
                }
            }
            // Add the final write for this sector to a list of everything that should be written.
            if (lastWrite != null) {
                writeList.put(lastWrite.sectorID, lastWrite);
                writeOrder.add(lastWrite);
            }
        }

        // TODO: Now build a path to all reads.
//        System.out.println ("\n\n========================\n\n");
        for (int readNumber = 0; readNumber < readList.size(); readNumber++)
        {
            // Go through each read in the list. (This list also contains SYSTEM commands.)
            Command currentCommand = readList.get(readNumber);
//            System.out.println("Found " + currentCommand);
//            if (currentCommand.type == CommandType.read) {
//                // We only want to generate seek paths for READ, not for SYSTEM.
//                ArrayList<Command> seekPath = generateNavigation(currentCommand);
//                System.out.println("\tWe need to traverse to the " + currentCommand);
//                for (Command seekingCommand : seekPath) {
//                    System.out.println("\t\tCame across a " + seekingCommand);
//                    if (seekingCommand.type == CommandType.idle)
//                    {
//                        // See if we can replace this idle with a write.
//                        int currentSectorID = seekingCommand.sectorID;
//                        boolean readyToWrite = false;
//                        if (writeList.containsKey(currentSectorID)) {
//                            // There is a write for this sector. Check to make sure it won't be overwriting a 'fresh' read later.
//                            readyToWrite = true;  // Not necessarily ready to write, but assume true until found otherwise.
//                            for (int i = 0; i < readList.size() && readyToWrite; i++) {
//                                if (readList.get(i) != null && currentSectorID == readList.get(i).sectorID)
//                                    // TODO: Maybe use the stack here...
//                                    readyToWrite = false;  // If we come across a fresh read that needs to happen later, we can't write to this sector.
//                            }
//                        }
//                        if (readyToWrite) {
//                            seekingCommand = writeList.get(currentSectorID);  // Write something if we safely can.
//                            writeList.remove(currentSectorID);
//                            System.out.println("\t\t>Replaced it with " + seekingCommand);
//                        }
//                    }
//                    optimized.add(seekingCommand);
//                }
//            } else {
//                optimized.add(currentCommand);
//            }

            if (currentCommand != null) {
                if (currentCommand.type == CommandType.system)
                    optimized.add(currentCommand);  // Leave SYSTEM commands alone.
                else {
                    // We need to get to the read point...
                    int desiredTrack = currentCommand.track;
                    int desiredSector = currentCommand.sector;

                    // TODO: refactor to remove redundant code
                    // Set the correct ARM status
                    if (currentTrack < desiredTrack) {
                        optimized.add(new Command(CommandType.arm, 1));
                        armStatus = 1;
                        spin();
                    } else if (currentTrack > desiredTrack) {
                        optimized.add(new Command(CommandType.arm, -1));
                        armStatus = -1;
                        spin();
                    }

                    // Add correct number of idles and add the proper WRITE any time we pass over a sector that needs a write.
                    // NOTICE: If writing to a sector, check that it is not overwriting any 'fresh' reads to come later.
                    boolean seeking = true;
                    while (seeking) {
                        if (currentTrack == desiredTrack && currentSector == desiredSector)
                            seeking = false;
                        else {

                            if (currentTrack == desiredTrack && armStatus != 0) {
                                optimized.add(new Command(CommandType.arm, 0));
                                armStatus = 0;
                                spin();
                            } else {
                                int currentSectorID = (currentTrack * 10) + currentSector;
                                boolean readyToWrite = false;
                                if (writeList.containsKey(currentSectorID)) {
                                    // There is a write for this sector. Check to make sure it won't be overwriting a 'fresh' read later.
                                    readyToWrite = true;  // Not necessarily ready to write, but assume true until found otherwise.
                                    for (int i = 0; i < readList.size() && readyToWrite; i++) {
                                        if (readList.get(i) != null && currentSectorID == readList.get(i).sectorID)
                                            readyToWrite = false;
                                    }
                                }
                                if (readyToWrite) {
//                                    System.out.println("Was able to sneak in a write");
                                    optimized.add(writeList.get(currentSectorID));  // Write something if we safely can.
                                    writeList.remove(currentSectorID);
                                } else optimized.add(new Command(CommandType.idle));  // Otherwise, just stick in an idle.
                                spin();
                            }
                        }
                    }
                    optimized.add(currentCommand);
                    readList.set(readList.indexOf(currentCommand), null);  // Changing to null instead of removing to avoid ConcurrentModificationException
                }
                spin();
            }
        }

        while (!writeOrder.isEmpty())
        {
            // Use a greedy algorithm based on 'distance' (based on the Command compareTo method)
            Collections.sort(writeOrder);
            Command currentCommand = writeOrder.get(0);
            writeOrder.remove(currentCommand);

            // We need to get to the correct sector to write to...
            int desiredTrack = currentCommand.track;
            int desiredSector = currentCommand.sector;

            // TODO: refactor to remove redundant code
            // Set the correct ARM status
            if (currentTrack < desiredTrack) {
                optimized.add(new Command(CommandType.arm, 1));
                armStatus = 1;
                spin();
            } else if (currentTrack > desiredTrack) {
                optimized.add(new Command(CommandType.arm, -1));
                armStatus = -1;
                spin();
            }

            // Add correct number of idles and add the proper WRITE any time we pass over a sector that needs a write.
            boolean seeking = true;
            while (seeking) {
                if (currentTrack == desiredTrack && armStatus != 0) {
                    optimized.add(new Command(CommandType.arm, 0));
                    armStatus = 0;
                    spin();
                }
                if (currentTrack == desiredTrack && currentSector == desiredSector)
                    seeking = false;
                else {
                    int currentSectorID = (currentTrack * 10) + currentSector;
                    if (writeList.containsKey(currentSectorID)) {
                        optimized.add(writeList.get(currentSectorID));  // Write something if we safely can.
                        writeOrder.remove(writeList.get(currentSectorID));
                        writeList.remove(currentSectorID);
                    }
                    else
                    optimized.add(new Command(CommandType.idle));  // Otherwise, just stick in an idle.
                    spin();
                }
            }
            optimized.add(currentCommand);
            spin();
        }

//        return optimized.size() < unoptimized.size() ? optimized : unoptimized;
        return optimized;
    }

    // Generates a group of commands to navigate to a given sector.
    private static ArrayList<Command> generateNavigation(Command currentCommand)
    {
        ArrayList<Command> navigationResults = new ArrayList<Command>();

        // We need to get to the correct sector to write to...
        int desiredTrack = currentCommand.track;
        int desiredSector = currentCommand.sector;

        // TODO: refactor to remove redundant code
        // Set the correct ARM status
        if (currentTrack < desiredTrack) {
            navigationResults.add(new Command(CommandType.arm, 1));
            armStatus = 1;
            spin();
        } else if (currentTrack > desiredTrack) {
            navigationResults.add(new Command(CommandType.arm, -1));
            armStatus = -1;
            spin();
        }

        // Add correct number of idles and add the proper WRITE any time we pass over a sector that needs a write.
        boolean seeking = true;
        while (seeking) {
            if (currentTrack == desiredTrack && armStatus != 0) {
                navigationResults.add(new Command(CommandType.arm, 0));
                armStatus = 0;
                spin();
            }
            if (currentTrack == desiredTrack && currentSector == desiredSector)
                seeking = false;
            else {
                navigationResults.add(new Command(CommandType.idle));  // Otherwise, just stick in an idle.
                spin();
            }
        }
        navigationResults.add(currentCommand);
        spin();

        return navigationResults;
    }

    // Command class to make things clean and readable.
    private static class Command implements Comparable<Command>
    {
        CommandType type;
        boolean hasParam = false;
        int param = 0, track = 0, sector = 0, sectorID = 0;
        public Command(CommandType type) {
            // For non-param commands
            this.type = type;
            this.sectorID = (currentTrack * 10) + currentSector;
            this.track = currentTrack;
            this.sector = currentSector;
        }
        public Command(CommandType type, Integer param) {
            // For commands with param.
            hasParam = true;
            this.type = type;
            this.param = param;
            this.sectorID = (currentTrack * 10) + currentSector;
            this.track = currentTrack;
            this.sector = currentSector;
        }
        public int compareTo(Command that) {
            // Calculate some general 'distance' to implement a greedy algorithm for leftover writes.
            int thisTrackDistance = Math.abs(currentTrack - this.track);
            int thatTrackDistance = Math.abs(currentTrack - that.track);
            int thisSectorDistance = (this.sector - currentSector + numSectorsPerTrack ) % numSectorsPerTrack;
            int thatSectorDistance = (that.sector - currentSector + numSectorsPerTrack) % numSectorsPerTrack;
            return (thisSectorDistance + thisTrackDistance) - (thatSectorDistance + thatTrackDistance);
        }
        public String toString() { return type.toString() + (hasParam ? (" " + param) : ""); }
    }

    // Stack class
    private static class Stack {
        Command[] elements;
        int last = 0;
        int size;
        public Stack(int size) {
            this.size = size;
            elements = new Command[size];
        };
        public void push(Command element) {
            elements[last] = element;
            last = (last + 1) % size;
        }
        public Command pop() {
            last = (last - 1) % size;
            Command element = elements[last];
            elements[last] = null;
            return element;
        }
        public Command peek() {
            return elements[last];
        }
    }

    private static void spin() {
        // Simulate spin
        currentSector = (currentSector + 1) % numSectorsPerTrack;
        currentTrack = (currentTrack + armStatus);
        if (currentTrack > numTracks) currentTrack = numTracks;
        else if (currentTrack < 0) currentTrack = 0;
    }
}
