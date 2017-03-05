package com.boydti.fawe.object.serializer;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.sk89q.worldedit.entity.Player;
import java.lang.reflect.Type;

public class PlayerSerializer extends JsonSerializable<Player> {
    private final Player player;

    public PlayerSerializer(Player player) {
        this.player = player;
    }
    @Override
    public Player deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        return player;
    }

    @Override
    public JsonElement serialize(Player src, Type typeOfSrc, JsonSerializationContext context) {
        return null;
    }
}
