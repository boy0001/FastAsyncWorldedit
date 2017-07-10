package com.thevoxelbox.voxelsniper.brush.perform;

import com.sk89q.worldedit.function.pattern.Pattern;
import com.thevoxelbox.voxelsniper.Message;
import com.thevoxelbox.voxelsniper.SnipeData;
import com.thevoxelbox.voxelsniper.brush.Brush;
import com.thevoxelbox.voxelsniper.event.SniperBrushChangedEvent;
import java.util.Arrays;
import org.bukkit.Bukkit;

public abstract class PerformBrush extends Brush implements Performer {
    protected vPerformer current = new pMaterial();

    public PerformBrush() {
    }

    public vPerformer getCurrentPerformer() {
        return this.current;
    }

    public void parse(String[] args, SnipeData v) {
        String handle = args[0];
        if(PerformerE.has(handle)) {
            vPerformer p = PerformerE.getPerformer(handle);
            if(p != null) {
                this.current = p;
                SniperBrushChangedEvent event = new SniperBrushChangedEvent(v.owner(), v.owner().getCurrentToolId(), this, this);
                Bukkit.getPluginManager().callEvent(event);
                this.info(v.getVoxelMessage());
                this.current.info(v.getVoxelMessage());
                if(args.length > 1) {
                    String[] additionalArguments = (String[])Arrays.copyOfRange(args, 1, args.length);
                    this.parameters(this.hackTheArray(additionalArguments), v);
                }
            } else {
                this.parameters(this.hackTheArray(args), v);
            }
        } else {
            this.parameters(this.hackTheArray(args), v);
        }

    }

    private String[] hackTheArray(String[] args) {
        String[] returnValue = new String[args.length + 1];
        int i = 0;

        for(int argsLength = args.length; i < argsLength; ++i) {
            String arg = args[i];
            returnValue[i + 1] = arg;
        }

        return returnValue;
    }

    public void initP(SnipeData v) {
        Pattern pattern = v.getPattern();
        if (pattern != null) {
            if (!(current instanceof PatternPerformer)) {
                current = new PatternPerformer();
            }
        } else if (current instanceof PatternPerformer) {
            current = new pMaterial();
        }
        this.current.init(v);
        this.current.setUndo();
    }

    public void showInfo(Message vm) {
        this.current.info(vm);
    }

    public static Class<?> inject() {
        return PerformBrush.class;
    }
}
