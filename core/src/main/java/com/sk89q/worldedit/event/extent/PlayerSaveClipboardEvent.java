package com.sk89q.worldedit.event.extent;

import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.event.Cancellable;
import com.sk89q.worldedit.event.Event;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.session.ClipboardHolder;

import java.net.URI;

public class PlayerSaveClipboardEvent extends FaweEvent {
    private final Player player;
    private final Clipboard clipboard;
    private final URI uri;
    private boolean cancelled;

    public PlayerSaveClipboardEvent(Player player, Clipboard clipboard, URI destination) {
        this.player = player;
        this.clipboard = clipboard;
        this.uri = destination;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public URI getUri() {
        return uri;
    }

    public Clipboard getClipboard() {
        return clipboard;
    }

    public Player getPlayer() {
        return player;
    }
}
