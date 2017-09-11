package com.boydti.fawe.bukkit.util.cui;

import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.util.cui.CUI;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.Vector2D;
import com.sk89q.worldedit.internal.cui.CUIEvent;
import com.sk89q.worldedit.internal.cui.SelectionPointEvent;
import com.sk89q.worldedit.internal.cui.SelectionShapeEvent;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class StructureCUI extends CUI {
    private boolean cuboid;
    private Map<Vector2D, Material> chunkMap = new HashMap<>();

    public StructureCUI(FawePlayer player) {
        super(player);
    }

    @Override
    public void dispatchCUIEvent(CUIEvent event) {
        if (event instanceof SelectionShapeEvent) {
            clear();
            this.cuboid = event.getParameters()[0].equalsIgnoreCase("cuboid");
        } else if (cuboid && event instanceof SelectionPointEvent) {

        }
    }

    public void draw(Vector pos1, Vector pos2) {
        Player player = this.<Player>getPlayer().parent;
        Location position = player.getLocation();

        int view;
        if (Bukkit.getVersion().contains("paper")) {
            view = player.getViewDistance();
        } else {
            view = Bukkit.getViewDistance();
        }

    }

    public void clear() {

    }
}
