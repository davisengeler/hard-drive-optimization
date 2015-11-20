package com.davisengeler;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {

        // Set up some initial stuff
        int numTracks = 10;
        int numSectorsPerTrack = 10;
        HardDrive hdd = new HardDrive(numTracks, numSectorsPerTrack);

        // Scan in the requests
        Scanner scan = null;
        try {
            scan = new Scanner(new File("test.txt"));
        } catch (FileNotFoundException e) {
            System.out.println(e.getMessage());
        }
    }
}
