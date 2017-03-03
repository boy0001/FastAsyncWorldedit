package com.boydti.fawe.object.brush.visualization;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.util.TaskManager;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.command.tool.BrushTool;
import com.sk89q.worldedit.command.tool.Tool;
import com.sk89q.worldedit.command.tool.brush.Brush;
import com.sk89q.worldedit.entity.Player;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class VisualQueue {

    private ConcurrentHashMap<FawePlayer, Long> playerMap;

    public VisualQueue() {
        playerMap = new ConcurrentHashMap<>();
        Runnable task = new Runnable() {
            @Override
            public void run() {
                long allowedTick = Fawe.get().getTimer().getTick() - 1;
                Iterator<Map.Entry<FawePlayer, Long>> iter = playerMap.entrySet().iterator();
                while (iter.hasNext()) {
                    Map.Entry<FawePlayer, Long> entry = iter.next();
                    Long time = entry.getValue();
                    if (time < allowedTick) {
                        FawePlayer fp = entry.getKey();
                        iter.remove();
                        LocalSession session = fp.getSession();
                        Player player = fp.getPlayer();
                        Tool tool = session.getTool(player.getItemInHand());
                        Brush brush;
                        if (tool instanceof BrushTool) {
                            BrushTool brushTool = (BrushTool) tool;
                            if (brushTool.getVisualMode() != VisualMode.NONE) {
                                try {
                                    brushTool.visualize(BrushTool.BrushAction.PRIMARY, player);
                                } catch (Throwable e) {
                                    WorldEdit.getInstance().getPlatformManager().handleThrowable(e, player);
                                }
                            }
                        }
                    }
                }
                TaskManager.IMP.laterAsync(this, 3);
            }
        };
        TaskManager.IMP.laterAsync(task, 3);
    }

    public boolean dequeue(FawePlayer player) {
        return playerMap.remove(player) != null;
    }

    public void queue(FawePlayer player) {
        playerMap.put(player, Fawe.get().getTimer().getTick());
    }
}