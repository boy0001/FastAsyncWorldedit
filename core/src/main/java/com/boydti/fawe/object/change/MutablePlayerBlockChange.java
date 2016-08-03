package com.boydti.fawe.object.change;

import java.util.UUID;

public class MutablePlayerBlockChange extends MutableBlockChange {
    private final UUID uuid;

    public MutablePlayerBlockChange(UUID uuid, int x, int y, int z, short id, byte data) {
        super(x, y, z, id, data);
        this.uuid = uuid;
    }

    public UUID getUIID() {
        return uuid;
    }
}
