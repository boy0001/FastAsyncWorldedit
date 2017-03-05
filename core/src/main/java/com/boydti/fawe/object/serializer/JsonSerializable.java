package com.boydti.fawe.object.serializer;

import com.google.gson.JsonDeserializer;
import com.google.gson.JsonSerializer;

public abstract class JsonSerializable<T> implements JsonSerializer<T>, JsonDeserializer<T> {

    public JsonSerializable() {
    }
}
