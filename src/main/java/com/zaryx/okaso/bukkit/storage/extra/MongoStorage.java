package com.zaryx.okaso.bukkit.storage.extra;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.zaryx.okaso.bukkit.storage.core.AbstractJsonStorageContext;
import org.bson.Document;

import java.util.HashSet;
import java.util.Set;

import static com.mongodb.client.model.Filters.eq;

public class MongoStorage extends AbstractJsonStorageContext {

    private final MongoClient client;
    private final MongoCollection<Document> collection;

    public MongoStorage(String uri, String database, String collectionName) {
        this.client = MongoClients.create(uri);
        MongoDatabase db = this.client.getDatabase(database);
        this.collection = db.getCollection(collectionName);
        this.connect();
    }

    @Override
    public boolean connect() {
        return super.connect();
    }

    @Override
    public void disconnect() {
        this.client.close();
        super.disconnect();
    }

    @Override
    public boolean isConnected() {
        return super.isConnected();
    }

    @Override
    protected void writeJson(String key, String json) {
        Document document = new Document("_id", key).append("value", json);
        this.collection.replaceOne(eq("_id", key), document, new com.mongodb.client.model.ReplaceOptions().upsert(true));
    }

    @Override
    protected String readJson(String key) {
        Document doc = this.collection.find(eq("_id", key)).first();
        if (doc == null) {
            return null;
        }

        Object value = doc.get("value");
        return value == null ? null : value.toString();
    }

    @Override
    protected boolean existsJson(String key) {
        return this.collection.find(eq("_id", key)).first() != null;
    }

    @Override
    protected boolean deleteJson(String key) {
        return this.collection.deleteOne(eq("_id", key)).getDeletedCount() > 0;
    }

    @Override
    protected Set<String> snapshotKeys() {
        Set<String> keys = new HashSet<>();
        for (Document document : this.collection.find()) {
            Object id = document.get("_id");
            if (id != null) {
                keys.add(id.toString());
            }
        }
        return keys;
    }
}
