package com.boydti.fawe.util.image;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.util.MainUtil;
import com.sk89q.worldedit.util.command.parametric.ParameterException;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;

public class ImageUtil {
    public static BufferedImage getScaledInstance(BufferedImage img,
                                                  int targetWidth,
                                                  int targetHeight,
                                                  Object hint,
                                                  boolean higherQuality)
    {
        if (img.getHeight() == targetHeight && img.getWidth() == targetWidth) {
            return img;
        }
        int type = (img.getTransparency() == Transparency.OPAQUE) ?
                BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;
        BufferedImage ret = (BufferedImage)img;
        int w, h;
        if (higherQuality) {
            // Use multi-step technique: start with original size, then
            // scale down in multiple passes with drawImage()
            // until the target size is reached
            w = img.getWidth();
            h = img.getHeight();
        } else {
            // Use one-step technique: scale directly from original
            // size to target size with a single drawImage() call
            w = targetWidth;
            h = targetHeight;
        }

        do {
            if (higherQuality && w > targetWidth) {
                w /= 2;
                if (w < targetWidth) {
                    w = targetWidth;
                }
            }

            if (higherQuality && h > targetHeight) {
                h /= 2;
                if (h < targetHeight) {
                    h = targetHeight;
                }
            }

            BufferedImage tmp = new BufferedImage(w, h, type);
            Graphics2D g2 = tmp.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, hint);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
            g2.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_SPEED);
            g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
            g2.drawImage(ret, 0, 0, w, h, null);
            g2.dispose();

            ret = tmp;
        } while (w != targetWidth || h != targetHeight);

        return ret;
    }

    public static int getColor(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        long totalRed = 0;
        long totalGreen = 0;
        long totalBlue = 0;
        long totalAlpha = 0;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int color = image.getRGB(x, y);
                totalRed += (color >> 16) & 0xFF;
                totalGreen += (color >> 8) & 0xFF;
                totalBlue += (color >> 0) & 0xFF;
                totalAlpha += (color >> 24) & 0xFF;
            }
        }
        int a = width * height;
        int red = (int) (totalRed / a);
        int green = (int) (totalGreen / a);
        int blue = (int) (totalBlue / a);
        int alpha = (int) (totalAlpha / a);
        return (alpha << 24) + (red << 16) + (green << 8) + (blue << 0);
    }

    public static BufferedImage getImage(String arg) throws ParameterException {
        try {
            if (arg.startsWith("http")) {
                if (arg.contains("imgur.com") && !arg.contains("i.imgur.com")) {
                    arg = "https://i.imgur.com/" + arg.split("imgur.com/")[1] + ".png";
                }
                URL url = new URL(arg);
                BufferedImage img = MainUtil.readImage(url);
                if (img == null) {
                    throw new IOException("Failed to read " + url + ", please try again later");
                }
                return img;
            } else if (arg.startsWith("file:/")) {
                arg = arg.replaceFirst("file:/+", "");
                File file = MainUtil.getFile(MainUtil.getFile(Fawe.imp().getDirectory(), com.boydti.fawe.config.Settings.IMP.PATHS.HEIGHTMAP), arg);
                return MainUtil.readImage(file);
            } else {
                throw new ParameterException("Invalid image " + arg);
            }
        } catch (IOException e) {
            throw new ParameterException(e);
        }
    }
}
