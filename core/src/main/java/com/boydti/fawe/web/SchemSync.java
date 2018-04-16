package com.boydti.fawe.web;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.util.MainUtil;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SchemSync implements Runnable {
    private final static char PORT = 62522;

    private final File tokensFile;
    private Map<UUID, UUID> tokens;

    public SchemSync() {
        this.tokensFile = MainUtil.getFile(Fawe.imp().getDirectory(), Settings.IMP.PATHS.TOKENS, "TOKENS.TXT");
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
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
