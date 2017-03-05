package com.boydti.fawe.object.brush;

import com.boydti.fawe.object.brush.scroll.ScrollAction;
import com.boydti.fawe.object.extent.ResettableExtent;
import com.sk89q.worldedit.command.tool.brush.Brush;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.pattern.Pattern;

public class BrushSettings {
    public Brush brush = null;
    public Mask mask = null;
    public Mask sourceMask = null;
    public ResettableExtent transform = null;
    public Pattern material;
    public double size = 1;
    public String permission;
    public ScrollAction scrollAction;
}
