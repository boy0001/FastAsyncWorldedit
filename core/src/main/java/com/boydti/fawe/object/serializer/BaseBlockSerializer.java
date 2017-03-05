package com.boydti.fawe.object.serializer;

import com.boydti.fawe.FaweCache;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.sk89q.worldedit.blocks.BaseBlock;
import java.lang.reflect.Type;

public class BaseBlockSerializer extends JsonSerializable<BaseBlock> {
    @Override
    public BaseBlock deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonArray jsonArray = json.getAsJsonArray();
        if (jsonArray.size() != 2) {
            throw new JsonParseException("Expected array of 3 length for Vector");
        }
        return FaweCache.getBlock(jsonArray.get(0).getAsInt(), jsonArray.get(1).getAsInt());
    }

    @Override
    public JsonElement serialize(BaseBlock src, Type typeOfSrc, JsonSerializationContext context) {
        JsonArray array = new JsonArray();
        array.add(new JsonPrimitive(src.getId()));
        array.add(new JsonPrimitive(src.getData()));
        return array;
    }
}
