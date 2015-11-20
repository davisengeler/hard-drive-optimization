package com.davisengeler;

public class HardDrive {
    public int numSectorsPerTrack = 0;
    public int numTracks = 0;
    public int trackSector[][];
    public int currentTrack = 0;
    public int currentSector = 0;
    Arm armStatus = Arm.STILL;

    public HardDrive (int numTracks, int numSectorsPerTrack) {
        this.numTracks = numTracks;
        this.numSectorsPerTrack = numSectorsPerTrack;
        trackSector = new int[numTracks][numSectorsPerTrack];
    }

    public enum CommandType {
        idle, arm, read, write, push, pop, system
    }

    public void command(CommandType command, int param) {
//        System.out.println("\tHDD Recieved command '" + command.toString() + "' with parameter '" + param + "'.");
        switch (command) {
            case idle:
                // Does nothing
                break;
            case arm:
                setArm(param);
                break;
            case read:
                // Temporarily printing READ
                System.out.println("Value '" + read() + "' read from track " + currentTrack + ", sector " + currentSector);
                break;
            case write:
                write(param);
                System.out.println("Value '" + param + "' saved to track " + currentTrack + ", sector " + currentSector);
                break;
            case push:
                // Fuck this thing
                break;
            case pop:
                // Fuck that thing
                break;
            case system:
                system();
                break;
            default:
                System.out.println("INVALID COMMAND");
        }
        spin(); // Simulates a unit of time passing for every command sent.
    }

    public enum Arm {
        OUT, IN, STILL
    }

    public void setArm(int status) {
        switch (status) {
            case -1:
                armStatus = Arm.IN;
                System.out.println("\tThe arm is now moving in.");
                break;
            case 1:
                armStatus = Arm.OUT;
                System.out.println("\tThe arm is now moving out.");
                break;
            default:
                armStatus = Arm.STILL;
                System.out.println("\tThe arm has stopped moving.");
        }
    }

    public void spin() {
        currentSector = (currentSector + 1) % numSectorsPerTrack;
        if (armStatus == Arm.OUT && currentTrack < (numTracks - 1)) {
            currentTrack++;
        } else if (armStatus == Arm.IN && currentTrack > 0) {
            currentTrack--;
        }
//        System.out.println("\t\tThe read head is now over track " + currentTrack + ", sector " + currentSector);
    }

    public int read() {
        return trackSector[currentTrack][currentSector];
    }

    public void write(int value) {
        trackSector[currentTrack][currentSector] = value;
    }

    public int system() { return trackSector[currentTrack][currentSector]; } // Not totally sure if this does anything different than 'read' at this simulated hardware level.

}
