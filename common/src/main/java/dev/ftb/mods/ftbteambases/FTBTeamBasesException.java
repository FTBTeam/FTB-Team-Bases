package dev.ftb.mods.ftbteambases;

public class FTBTeamBasesException extends RuntimeException {
    public FTBTeamBasesException(String message) {
        super(message);
    }

    public FTBTeamBasesException(String message, Throwable cause) {
        super(message, cause);
    }
}
