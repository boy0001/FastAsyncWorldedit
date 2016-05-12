package com.boydti.fawe;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.config.BBC;
import com.boydti.fawe.object.FaweCommand;
import com.boydti.fawe.object.FawePlayer;
import java.util.List;
import java.util.Optional;
import org.spongepowered.api.command.CommandCallable;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.text.Text;

/**
 * Created by Jesse on 4/2/2016.
 */
public class SpongeCommand implements CommandCallable {

    private final FaweCommand cmd;

    public SpongeCommand(final FaweCommand cmd) {
        this.cmd = cmd;
    }

    @Override
    public CommandResult process(CommandSource source, String args) throws CommandException {
        final FawePlayer plr = Fawe.imp().wrap(source);
        if (!source.hasPermission(this.cmd.getPerm())) {
            BBC.NO_PERM.send(plr, this.cmd.getPerm());
            return CommandResult.success();
        }
        this.cmd.executeSafe(plr, args.split(" "));
        return CommandResult.success();
    }

    @Override
    public List<String> getSuggestions(CommandSource source, String arguments) throws CommandException {return null;}

    @Override
    public boolean testPermission(CommandSource source) {return true;}

    @Override
    public Optional<? extends Text> getShortDescription(final CommandSource cmd) {
        return Optional.of(Text.of("Various"));
    }

    @Override
    public Optional<? extends Text> getHelp(final CommandSource cmd) {
        return Optional.of(Text.of("/<fixlighting|stream|wea|select>"));
    }

    @Override
    public Text getUsage(final CommandSource cmd) {
        return Text.of("/<fixlighting|stream|wea|select>");
    }
}
