package com.boydti.fawe.util.image;

import java.awt.image.BufferedImage;
import java.io.Closeable;

public interface ImageViewer extends Closeable{
    public void view(BufferedImage image);
}
