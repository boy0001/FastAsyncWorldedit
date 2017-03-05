package com.boydti.fawe.object.serializer;

import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.util.EditSessionBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.entity.Player;
import java.lang.reflect.Type;

public class EditSessionSerializer extends JsonSerializable<EditSession> {

    private final Player player;

    public EditSessionSerializer(Player player) {
        this.player = player;
    }

    @Override
    public EditSession deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        return new EditSessionBuilder(player.getWorld()).player(FawePlayer.wrap(player)).build();
    }

    @Override
    public JsonElement serialize(EditSession src, Type typeOfSrc, JsonSerializationContext context) {
        return null;
    }
}
