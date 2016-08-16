package com.boydti.fawe.jnbt;

import java.io.File;
import java.io.FileNotFoundException;

public class MCAFile {
    private final File file;
    byte[] header;

    public MCAFile(File regionFolder, int mcrX, int mcrZ) throws FileNotFoundException {
        // TODO load NBT
        this.file = new File(regionFolder, "r." + mcrX + "." + mcrZ + ".mca");
        if (!file.exists()) {
            throw new FileNotFoundException(file.toString());
        }
        this.header = new byte[4096];
    }
}
