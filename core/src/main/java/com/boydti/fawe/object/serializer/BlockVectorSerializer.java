package com.boydti.fawe.object.serializer;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.sk89q.worldedit.MutableBlockVector;
import com.sk89q.worldedit.Vector;
import java.lang.reflect.Type;

public class BlockVectorSerializer extends JsonSerializable<Vector> {

    @Override
    public JsonElement serialize(Vector src, Type typeOfSrc, JsonSerializationContext context) {
        JsonArray array = new JsonArray();
        array.add(new JsonPrimitive(src.getBlockX()));
        array.add(new JsonPrimitive(src.getBlockY()));
        array.add(new JsonPrimitive(src.getBlockZ()));
        return array;
    }


    @Override
    public Vector deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonArray jsonArray = json.getAsJsonArray();
        if (jsonArray.size() != 3) {
            throw new JsonParseException("Expected array of 3 length for Vector");
        }
        int x = jsonArray.get(0).getAsInt();
        int y = jsonArray.get(1).getAsInt();
        int z = jsonArray.get(2).getAsInt();
        return new MutableBlockVector(x, y, z);
    }
}
