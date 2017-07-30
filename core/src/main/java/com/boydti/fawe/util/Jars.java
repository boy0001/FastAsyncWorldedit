package com.boydti.fawe.util;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import com.boydti.fawe.Fawe;
import com.google.common.io.BaseEncoding;

public enum Jars {
    WE_B_6_1_7_2("https://addons.cursecdn.com/files/2431/372/worldedit-bukkit-6.1.7.2.jar",
            "711be37301a327aba4e347131875d0564dbfdc2f41053a12db97f0234661778b", 1726340),

    VS_B_5_171_0("https://addons-origin.cursecdn.com/files/912/511/VoxelSniper-5.171.0-SNAPSHOT.jar",
            "292c3b38238e0d8e5f036381d28bccfeb15df67cae53d28b52d066bc6238208f", 3632776);

    public final String url;
    public final int filesize;
    public final String digest;

    /**
     * @param url
     *            Where this jar can be found and downloaded
     * @param digest
     *            The SHA-256 hexadecimal digest
     * @param filesize
     *            Size of this jar in bytes
     */
    private Jars(String url, String digest, int filesize) {
        this.url = url;
        this.digest = digest;
        this.filesize = filesize;
    }

    /** download a jar, verify hash, return byte[] containing the jar */
    public byte[] download() throws IOException {
        byte[] jarBytes = new byte[this.filesize];
        URL url = new URL(this.url);
        try (DataInputStream dis = new DataInputStream(url.openConnection().getInputStream());) {
            dis.readFully(jarBytes);
            if (dis.read() != -1) { // assert that we've read everything
                throw new IllegalStateException("downloaded jar is longer than expected");
            }

            MessageDigest md;
            md = MessageDigest.getInstance("SHA-256");
            byte[] thisDigest = md.digest(jarBytes);
            byte[] realDigest = BaseEncoding.base16().decode(this.digest.toUpperCase());

            if (Arrays.equals(thisDigest, realDigest)) {
                Fawe.debug("++++ HASH CHECK ++++");
                Fawe.debug(this.url);
                Fawe.debug(BaseEncoding.base16().encode(thisDigest));
                return jarBytes;
            } else {
                throw new IllegalStateException("downloaded jar does not match the hash");
            }
        } catch (NoSuchAlgorithmException e) {
            // Shouldn't ever happen, Minecraft won't even run on such a JRE
            throw new IllegalStateException("Your JRE does not support SHA-256");
        }

    }
}
