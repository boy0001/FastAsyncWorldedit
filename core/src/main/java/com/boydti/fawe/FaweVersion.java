package com.boydti.fawe;

public class FaweVersion {
    public final int year, month, day, hash, build;

    public FaweVersion(String version) {
        String[] split = version.substring(version.indexOf('=') + 1).split("-");
        String[] date = split[0].split("\\.");
        this.year = Integer.parseInt(date[0]);
        this.month = Integer.parseInt(date[1]);
        this.day = Integer.parseInt(date[2]);
        this.hash = Integer.parseInt(split[1], 16);
        this.build = Integer.parseInt(split[2]);
    }
}
