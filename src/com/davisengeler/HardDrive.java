package com.davisengeler;

/**
 * Created by Davis on 11/19/15.
 */
public class HardDrive {
    int numSectorsPerTrack = 0;
    int numTracks = 0;
    int trackSector[][];
    int currentTrack = 0;
    int currentSector = 0;
    Arm armStatus = Arm.STILL;

    public HardDrive (int numTracks, int numSectorsPerTrack) {
        this.numTracks = numTracks;
        this.numSectorsPerTrack = numSectorsPerTrack;
        trackSector = new int[numTracks][numSectorsPerTrack];
    }

    public enum Command {
        IDLE, ARM, READ, WRITE, PUSH, POP, SYSTEM
    }

    public void command(Command command, int param) {
        switch (command) {
            case IDLE:
                // Does nothing
                break;
            case ARM:
                setArm(param);
                break;
            case READ:
                // Temporarily printing READ
                System.out.println(read());
                break;
            case WRITE:
                write(param);
                break;
            case PUSH:
                // Fuck this thing
                break;
            case POP:
                // Fuck that thing
                break;
            case SYSTEM:
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
                break;
            case 1:
                armStatus = Arm.OUT;
                break;
            default:
                armStatus = Arm.STILL;
        }
    }

    public void spin() {
        currentSector = (currentSector + 1) % numSectorsPerTrack;
        if (armStatus == Arm.OUT && currentTrack < (numTracks - 1)) {
            currentTrack++;
        } else if (armStatus == Arm.IN && currentTrack > (numTracks - 1)) {
            currentTrack--;
        }
    }

    public int read() {
        return trackSector[currentTrack][currentSector];
    }

    public void write(int value) {
        trackSector[currentTrack][currentSector] = value;
    }

    public int system() { return trackSector[currentTrack][currentSector]; } // Not totally sure if this does anything different than 'read' at this simulated hardware level.

}
