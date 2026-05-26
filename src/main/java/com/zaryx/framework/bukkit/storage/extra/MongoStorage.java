package com.zaryx.framework.bukkit.storage.extra;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.zaryx.framework.bukkit.storage.core.StorageContext;
import org.bson.Document;

import java.lang.reflect.Type;

import static com.mongodb.client.model.Filters.eq;

public class MongoStorage implements StorageContext {

    private final MongoCollection<Document> collection;
    private final Gson gson;

    public MongoStorage(String uri, String database, String collectionName) {
        MongoClient client = MongoClients.create(uri);
        MongoDatabase db = client.getDatabase(database);
        this.collection = db.getCollection(collectionName);

        this.gson = new GsonBuilder().serializeNulls().create();
    }

    @Override
    public <T> void save(String key, T value) {
        String json = this.gson.toJson(value);

        Document document = new Document("_id", key).append("value", Document.parse(json));

        this.collection.replaceOne(eq("_id", key), document,
                new com.mongodb.client.model.ReplaceOptions().upsert(true));
    }

    @Override
    public <T> T load(String key, Class<T> clazz) {
        Document doc = this.collection.find(eq("_id", key)).first();
        if (doc == null) return null;

        Object value = doc.get("value");
        return this.gson.fromJson(value.toString(), clazz);
    }

    @Override
    public <T> T load(String key, Type type) {
        Document doc = this.collection.find(eq("_id", key)).first();
        if (doc == null) return null;

        Object value = doc.get("value");
        return this.gson.fromJson(value.toString(), type);
    }

    @Override
    public boolean exists(String key) {
        return this.collection.find(eq("_id", key)).first() != null;
    }

    @Override
    public void delete(String key) {
        this.collection.deleteOne(eq("_id", key));
    }
}