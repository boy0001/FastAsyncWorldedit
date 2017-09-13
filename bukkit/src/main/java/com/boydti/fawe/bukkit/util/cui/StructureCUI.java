package com.boydti.fawe.bukkit.util.cui;

import com.boydti.fawe.FaweCache;
import com.boydti.fawe.bukkit.v0.BukkitQueue_0;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.util.cui.CUI;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.nbt.NbtCompound;
import com.comphenix.protocol.wrappers.nbt.NbtFactory;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.internal.cui.CUIEvent;
import com.sk89q.worldedit.internal.cui.SelectionPointEvent;
import com.sk89q.worldedit.internal.cui.SelectionShapeEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class StructureCUI extends CUI {
    private boolean cuboid = true;

    private Vector pos1;
    private Vector pos2;

    private Vector remove;
    private int combined;

    public StructureCUI(FawePlayer player) {
        super(player);
    }

    @Override
    public void dispatchCUIEvent(CUIEvent event) {
        if (event instanceof SelectionShapeEvent) {
            clear();
            this.cuboid = event.getParameters()[0].equalsIgnoreCase("cuboid");
        } else if (cuboid && event instanceof SelectionPointEvent) {
            SelectionPointEvent spe = (SelectionPointEvent) event;
            String[] param = spe.getParameters();
            int id = Integer.parseInt(param[0]);
            int x = Integer.parseInt(param[1]);
            int y = Integer.parseInt(param[2]);
            int z = Integer.parseInt(param[3]);
            Vector pos = new Vector(x, y, z);
            if (id == 0) {
                pos1 = pos;
            } else {
                pos2 = pos;
            }
            update();
        }
    }

    private int viewDistance() {
        Player player = this.<Player>getPlayer().parent;
        if (Bukkit.getVersion().contains("paper")) {
            return player.getViewDistance();
        } else {
            return Bukkit.getViewDistance();
        }
    }

    public void clear() {
        pos1 = null;
        pos2 = null;
        update();
    }

    public synchronized void update() {
        Player player = this.<Player>getPlayer().parent;
        Location playerLoc = player.getLocation();
        if (remove != null) {
            int cx = playerLoc.getBlockX() >> 4;
            int cz = playerLoc.getBlockZ() >> 4;
            int viewDistance = viewDistance();
            if (Math.abs(cx - (remove.getBlockX() >> 4)) <= viewDistance && Math.abs(cz - (remove.getBlockZ() >> 4)) <= viewDistance) {
                Location removeLoc = new Location(player.getWorld(), remove.getX(), remove.getY(), remove.getZ());
                player.sendBlockChange(removeLoc, FaweCache.getId(combined), (byte) FaweCache.getData(combined));
            }
            remove = null;
        }
        if (pos1 == null || pos2 == null) return;
        Vector min = Vector.getMinimum(pos1, pos2);
        Vector max = Vector.getMaximum(pos1, pos2);

        // Position
        double rotX = playerLoc.getYaw();
        double rotY = playerLoc.getPitch();
        double xz = Math.cos(Math.toRadians(rotY));
        int x = (int) (playerLoc.getX() - (-xz * Math.sin(Math.toRadians(rotX))) * 3);
        int z = (int) (playerLoc.getZ() - (xz * Math.cos(Math.toRadians(rotX))) * 3);
        int y = Math.min(Math.min(255, max.getBlockY() + 32), playerLoc.getBlockY() + 3);

        int minX = Math.max(Math.min(32, min.getBlockX() - x), -32);
        int maxX = Math.max(Math.min(32, max.getBlockX() - x + 1), -32);

        int minY = Math.max(Math.min(32, min.getBlockY() - y), -32);
        int maxY = Math.max(Math.min(32, max.getBlockY() - y + 1), -32);

        int minZ = Math.max(Math.min(32, min.getBlockZ() - z), -32);
        int maxZ = Math.max(Math.min(32, max.getBlockZ() - z + 1), -32);

        int sizeX = Math.min(32, maxX - minX);
        int sizeY = Math.min(32, maxY - minY);
        int sizeZ = Math.min(32, maxZ - minZ);
        if (sizeX == 0 || sizeY == 0 || sizeZ == 0) return;

        int posX = Math.max(minX, maxX - 48);
        int posY = Math.max(minY, maxY - 48);
        int posZ = Math.max(minZ, maxZ - 48);


        // NBT
        HashMap<String, Object> tag = new HashMap<>();
        tag.put("name", UUID.randomUUID().toString());
        tag.put("author", "Empire92"); // :D
        tag.put("metadata", "");
        tag.put("x", x);
        tag.put("y", y);
        tag.put("z", z);
        tag.put("posX", posX);
        tag.put("posY", posY);
        tag.put("posZ", posZ);
        tag.put("sizeX", sizeX);
        tag.put("sizeY", sizeY);
        tag.put("sizeZ", sizeZ);
        tag.put("rotation", "NONE");
        tag.put("mirror", "NONE");
        tag.put("mode", "SAVE");
        tag.put("ignoreEntities", true);
        tag.put("powered", false);
        tag.put("showair", false);
        tag.put("showboundingbox", true);
        tag.put("integrity", 1.0f);
        tag.put("seed", 0);
        tag.put("id", "minecraft:structure_block");
        Object nmsTag = BukkitQueue_0.fromNative(FaweCache.asTag(tag));
        NbtCompound compound = NbtFactory.fromNMSCompound(nmsTag);

        // Type
        int type = 7;

        // Packet
        PacketContainer containter = new PacketContainer(PacketType.Play.Server.TILE_ENTITY_DATA);
        containter.getBlockPositionModifier().write(0, new BlockPosition(x, y, z));
        containter.getIntegers().write(0, type);
        containter.getNbtModifier().write(0, compound);

        ProtocolManager manager = ProtocolLibrary.getProtocolManager();
        try {

            Block block = player.getWorld().getBlockAt(x, y, z);
            remove = new Vector(x, y, z);
            combined = FaweCache.getCombined(block.getTypeId(), block.getData());

            Location blockLoc = new Location(player.getWorld(), x, y, z);
            player.sendBlockChange(blockLoc, Material.STRUCTURE_BLOCK, (byte) 0);
            manager.sendServerPacket(player, containter);
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }
}
