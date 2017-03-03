package com.boydti.fawe.object.brush.scroll;

import com.boydti.fawe.util.MathMan;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.command.tool.BrushTool;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.session.ClipboardHolder;

public class ScrollClipboard extends ScrollAction {
    private final ClipboardHolder[] clipboards;
    private final LocalSession session;
    int index = 0;

    public ScrollClipboard(BrushTool tool, LocalSession session, ClipboardHolder[] clipboards) {
        super(tool);
        this.clipboards = clipboards;
        this.session = session;
    }

    @Override
    public boolean increment(Player player, int amount) {
        index = MathMan.wrap(index + amount, 0, clipboards.length - 1);
        ClipboardHolder clipboard = clipboards[index];
        session.setClipboard(clipboard);
        return true;
    }
}
