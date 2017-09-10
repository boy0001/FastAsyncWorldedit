package com.sk89q.worldedit.command;

import com.boydti.fawe.config.BBC;
import com.boydti.fawe.object.brush.BrushSettings;
import com.plotsquared.general.commands.Command;
import com.sk89q.minecraft.util.commands.CommandLocals;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.command.tool.BrushTool;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.util.command.CallableProcessor;

public class BrushProcessor implements CallableProcessor<BrushSettings> {
    private final WorldEdit worldEdit;

    public BrushProcessor(WorldEdit worldEdit) {
        this.worldEdit = worldEdit;
    }

    @Override
    public BrushSettings process(CommandLocals locals, BrushSettings settings) throws Command.CommandException, WorldEditException {
        Actor actor = locals.get(Actor.class);
        LocalSession session = worldEdit.getSessionManager().get(actor);
        session.setTool(null, (Player) actor);
        BrushTool tool = session.getBrushTool((Player) actor);
        tool.setPrimary(settings);
        tool.setSecondary(settings);
        BBC.BRUSH_EQUIPPED.send(actor, ((String) locals.get("arguments")).split(" ")[1]);
        return null;
    }
}