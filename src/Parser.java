import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import com.davisengeler.HardDrive;

public class Parser {

    public static void main(String[] args) {

        // Set up some initial stuff
        int numTracks = 10;
        int numSectorsPerTrack = 10;
        HardDrive hdd = new HardDrive(numTracks, numSectorsPerTrack);

        // Scan in the requests
        Scanner scan;
        try {
            scan = new Scanner(new File("test.txt"));
            hdd.command(HardDrive.Command.ARM, 1);
            while (scan.hasNext()) {
                scan.next();
                hdd.command(HardDrive.Command.READ, 0);
                System.out.println("Track: " + hdd.currentTrack + "\nSector: " + hdd.currentSector + "\n");
            }
        } catch (FileNotFoundException e) {
            System.out.println(e.getMessage());
        }
    }
}
