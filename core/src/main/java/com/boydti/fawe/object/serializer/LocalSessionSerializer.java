package com.boydti.fawe.object.serializer;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.entity.Player;
import java.lang.reflect.Type;

public class LocalSessionSerializer extends JsonSerializable<LocalSession> {
    private final Player player;

    public LocalSessionSerializer(Player player) {
        this.player = player;
    }

    @Override
    public LocalSession deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        return WorldEdit.getInstance().getSessionManager().get(player);
    }

    @Override
    public JsonElement serialize(LocalSession src, Type typeOfSrc, JsonSerializationContext context) {
        return null;
    }
}
