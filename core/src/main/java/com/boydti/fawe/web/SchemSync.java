package com.boydti.fawe.web;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.util.MainUtil;
import com.sk89q.worldedit.LocalConfiguration;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.command.UtilityCommands;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class SchemSync implements Runnable {
    private final static char PORT = 62522;

    private final File tokensFile;
    private final WorldEdit worldEdit;
    private final File working;
    private Map<UUID, UUID> tokens;

    public SchemSync() {
        this.tokensFile = MainUtil.getFile(Fawe.imp().getDirectory(), Settings.IMP.PATHS.TOKENS, "TOKENS.TXT");
        this.worldEdit = WorldEdit.getInstance();
        LocalConfiguration config = worldEdit.getConfiguration();
        this.working = worldEdit.getWorkingDirectoryFile(config.saveDir);
    }

    private void loadTokens() {
        if (tokens == null) {
            String tokensDir = Settings.IMP.PATHS.TOKENS;
            tokens = new HashMap<>();
        }
    }

    @Override
    public void run() {
        try {
            byte[] header = new byte[32];
            ServerSocket serverSocket = new ServerSocket(PORT);
            while (!Thread.interrupted()) {
                Socket clientSocket = serverSocket.accept();
                try (InputStream in = clientSocket.getInputStream()) {
                    int read = in.read(header);
                    if (read != header.length) continue;

                    ByteBuffer buf = ByteBuffer.wrap(header);
                    UUID uuid = new UUID(buf.getLong(), buf.getLong());
                    UUID expectedToken = tokens.get(uuid);
                    if (expectedToken == null) continue;

                    UUID receivedToken = new UUID(buf.getLong(), buf.getLong());
                    if (!receivedToken.equals(expectedToken)) continue;

                    try (DataInputStream dis = new DataInputStream(in)) {
                        File dir = new File(working, uuid.toString());

                        int data = dis.readByte() & 0xFF;
                        switch (data) {
                            case 0: {// list
                                try (DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream())) {
                                    UtilityCommands.allFiles(dir.listFiles(), true, new Consumer<File>() {
                                        @Override
                                        public void accept(File file) {
                                            String path = dir.toURI().relativize(file.toURI()).getPath();
                                            try {
                                                out.writeUTF(path);
                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    });
                                }
                                break;
                            }
                            case 1: {// get
                                String input = dis.readUTF();
                                File file = new File(dir, input);
                                if (!MainUtil.isInSubDirectory(working, file)) {
                                    
                                }
                                if (MainUtil.isInSubDirectory(working, file) && file.exists())
                                break;
                            }
                        }
                    }
                    // list
                    // get
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
