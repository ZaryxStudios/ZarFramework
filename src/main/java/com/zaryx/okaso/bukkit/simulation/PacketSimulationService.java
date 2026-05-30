package com.zaryx.okaso.bukkit.simulation;

import com.zaryx.okaso.bukkit.utility.Reflection;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Logger;

public class PacketSimulationService {

    // Manages client-side fake entities, blocks, structures, and visual effects.
    private final Logger logger;
    private final Map<String, ClientSideInstance> instances;
    private final Set<String> fakeBlockInstances;
    private final Set<String> fakeStructureInstances;
    private final Map<Integer, UUID> trackedEntities;
    private final Map<UUID, Object> activeBossBars;
    private final AtomicInteger entityIdCounter;
    private final Map<String, FakeTeam> fakeTeams;
    private final Map<String, FakeScoreboard> fakeScoreboards;

    private Consumer<String> errorHandler;

    public PacketSimulationService(Logger logger) {
        this.logger = logger;
        this.instances = new ConcurrentHashMap<>();
        this.fakeBlockInstances = ConcurrentHashMap.newKeySet();
        this.fakeStructureInstances = ConcurrentHashMap.newKeySet();
        this.trackedEntities = new ConcurrentHashMap<>();
        this.activeBossBars = new ConcurrentHashMap<>();
        this.entityIdCounter = new AtomicInteger(1000000);
        this.fakeTeams = new ConcurrentHashMap<>();
        this.fakeScoreboards = new ConcurrentHashMap<>();
    }

    // Sets a custom handler for errors during packet simulation.
    public PacketSimulationService errorHandler(Consumer<String> handler) {
        this.errorHandler = handler;
        return this;
    }

    // Returns the next available entity ID for fake entities.
    public int nextEntityId() {
        return entityIdCounter.incrementAndGet();
    }

    // Sends a single pre-built packet to a player.
    public void spoofPacket(Player player, Object packet) {
        send(player, packet);
    }

    // Builds a packet on demand for a specific player and sends it.
    public void spoofPacket(Player player, Function<Player, ?> packetFactory) {
        Objects.requireNonNull(packetFactory, "packetFactory");
        Object packet = packetFactory.apply(player);
        if (packet != null) {
            send(player, packet);
        }
    }

    // Sends multiple pre-built packets to a player.
    public void spoofPackets(Player player, Object... packets) {
        if (packets == null) return;
        for (Object packet : packets) {
            send(player, packet);
        }
    }

    // Sends a collection of packets to a player.
    public void spoofPackets(Player player, Collection<?> packets) {
        if (packets == null) return;
        for (Object packet : packets) {
            send(player, packet);
        }
    }

    public void spoofPacket(Collection<? extends Player> players, Object packet) {
        if (players == null || players.isEmpty()) return;
        for (Player player : players) {
            send(player, packet);
        }
    }

    public void spoofPacket(Collection<? extends Player> players, Function<Player, ?> packetFactory) {
        Objects.requireNonNull(packetFactory, "packetFactory");
        if (players == null || players.isEmpty()) return;
        for (Player player : players) {
            send(player, packetFactory.apply(player));
        }
    }

    public void spoofPackets(Collection<? extends Player> players, Object... packets) {
        if (players == null || players.isEmpty() || packets == null) return;
        for (Player player : players) {
            spoofPackets(player, packets);
        }
    }

    public void simulate(Player player, Collection<?> packets) {
        spoofPackets(player, packets);
    }

    public void simulate(Player player, Object... packets) {
        spoofPackets(player, packets);
    }

    public void simulate(Collection<? extends Player> players, Collection<?> packets) {
        if (players == null || players.isEmpty()) return;
        for (Player player : players) {
            spoofPackets(player, packets);
        }
    }

    public void simulate(Collection<? extends Player> players, Object... packets) {
        spoofPackets(players, packets);
    }

    public ClientSideInstance registerClientSideInstance(String id, Object... spawnPackets) {
        return registerClientSideInstance(id, asList(spawnPackets), Collections.<Object>emptyList());
    }

    public ClientSideInstance registerClientSideInstance(String id, Collection<?> spawnPackets, Collection<?> destroyPackets) {
        String key = normalizeId(id);
        ClientSideInstance instance = new ClientSideInstance(key, asList(spawnPackets), asList(destroyPackets));
        fakeBlockInstances.remove(key);
        fakeStructureInstances.remove(key);
        instances.put(key, instance);
        return instance;
    }

    public boolean hasClientSideInstance(String id) {
        return id != null && instances.containsKey(id);
    }

    public ClientSideInstance getClientSideInstance(String id) {
        return id == null ? null : instances.get(id);
    }

    public Set<String> getClientSideInstanceIds() {
        return Collections.unmodifiableSet(instances.keySet());
    }

    public boolean spawnClientSideInstance(Player player, String id) {
        ClientSideInstance instance = getClientSideInstance(id);
        if (instance == null) {
            warn("Client-side instance not found: " + id);
            return false;
        }
        spoofPackets(player, instance.getSpawnPackets());
        return true;
    }

    public boolean destroyClientSideInstance(Player player, String id) {
        ClientSideInstance instance = getClientSideInstance(id);
        if (instance == null) {
            warn("Client-side instance not found: " + id);
            return false;
        }
        spoofPackets(player, instance.getDestroyPackets());
        return true;
    }

    public void unregisterClientSideInstance(String id) {
        if (id != null) {
            String key = normalizeId(id);
            instances.remove(key);
            fakeBlockInstances.remove(key);
            fakeStructureInstances.remove(key);
        }
    }

    public void unregisterFakeBlock(String id) {
        unregisterClientSideInstance(id);
    }

    public void unregisterFakeStructure(String id) {
        unregisterClientSideInstance(id);
    }

    public void clearClientSideInstances() {
        instances.clear();
        fakeBlockInstances.clear();
        fakeStructureInstances.clear();
    }

    public void spawnClientSideInstance(Collection<? extends Player> players, String id) {
        if (players == null || players.isEmpty()) return;
        for (Player player : players) {
            spawnClientSideInstance(player, id);
        }
    }

    public void destroyClientSideInstance(Collection<? extends Player> players, String id) {
        if (players == null || players.isEmpty()) return;
        for (Player player : players) {
            destroyClientSideInstance(player, id);
        }
    }

    public ClientSideInstance registerFakeBlock(String id, Object placePacket, Object destroyPacket) {
        return registerFakeBlock(id, asList(new Object[]{placePacket}), asList(new Object[]{destroyPacket}));
    }

    public ClientSideInstance registerFakeBlock(String id, Collection<?> placePackets, Collection<?> destroyPackets) {
        ClientSideInstance instance = registerClientSideInstance(id, placePackets, destroyPackets);
        fakeBlockInstances.add(instance.getId());
        return instance;
    }

    public boolean placeFakeBlock(Player player, String id) {
        if (!fakeBlockInstances.contains(id)) {
            warn("Fake block instance not found: " + id);
            return false;
        }
        return spawnClientSideInstance(player, id);
    }

    public boolean removeFakeBlock(Player player, String id) {
        if (!fakeBlockInstances.contains(id)) {
            warn("Fake block instance not found: " + id);
            return false;
        }
        return destroyClientSideInstance(player, id);
    }

    public void placeFakeBlock(Collection<? extends Player> players, String id) {
        spawnClientSideInstance(players, id);
    }

    public void removeFakeBlock(Collection<? extends Player> players, String id) {
        destroyClientSideInstance(players, id);
    }

    public void spoofBlockPackets(Player player, Collection<?> packets) {
        spoofPackets(player, packets);
    }

    public Set<String> getFakeBlockInstanceIds() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(fakeBlockInstances));
    }

    public ClientSideInstance registerFakeStructure(String id, Collection<?> buildPackets, Collection<?> clearPackets) {
        ClientSideInstance instance = registerClientSideInstance(id, buildPackets, clearPackets);
        fakeStructureInstances.add(instance.getId());
        return instance;
    }

    public ClientSideInstance registerFakeStructure(String id, Object... buildPackets) {
        ClientSideInstance instance = registerClientSideInstance(id, buildPackets);
        fakeStructureInstances.add(instance.getId());
        return instance;
    }

    public boolean spawnFakeStructure(Player player, String id) {
        if (!fakeStructureInstances.contains(id)) {
            warn("Fake structure instance not found: " + id);
            return false;
        }
        return spawnClientSideInstance(player, id);
    }

    public boolean destroyFakeStructure(Player player, String id) {
        if (!fakeStructureInstances.contains(id)) {
            warn("Fake structure instance not found: " + id);
            return false;
        }
        return destroyClientSideInstance(player, id);
    }

    public void spawnFakeStructure(Collection<? extends Player> players, String id) {
        spawnClientSideInstance(players, id);
    }

    public void destroyFakeStructure(Collection<? extends Player> players, String id) {
        destroyClientSideInstance(players, id);
    }

    public void spoofStructurePackets(Player player, Collection<?> packets) {
        spoofPackets(player, packets);
    }

    public Set<String> getFakeStructureInstanceIds() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(fakeStructureInstances));
    }

    public boolean spawnFakeArmorStand(Player player, int entityId, UUID uuid, Location location,
            String name, boolean small, boolean marker, boolean invisible, boolean gravity) {
        try {
            Object packet = createArmorStandPacket(entityId, uuid, location, name, small, marker, invisible, gravity);
            send(player, packet);
            trackedEntities.put(entityId, uuid);
            return true;
        } catch (Exception e) {
            warn("Failed to spawn fake armor stand: " + e.getMessage());
            return false;
        }
    }

    public int spawnFakeArmorStand(Player player, UUID uuid, Location location,
            String name, boolean small, boolean marker, boolean invisible, boolean gravity) {
        int entityId = nextEntityId();
        spawnFakeArmorStand(player, entityId, uuid, location, name, small, marker, invisible, gravity);
        return entityId;
    }

    public void spawnFakeArmorStand(Collection<? extends Player> players, int entityId, UUID uuid,
            Location location, String name, boolean small, boolean marker, boolean invisible, boolean gravity) {
        for (Player p : players) {
            spawnFakeArmorStand(p, entityId, uuid, location, name, small, marker, invisible, gravity);
        }
    }

    public boolean spawnFakeLivingEntity(Player player, int entityId, UUID uuid, Location location,
            int entityTypeId, String customName, boolean customNameVisible) {
        try {
            Object packet = createSpawnEntityLivingPacket(entityId, uuid, location, entityTypeId);
            send(player, packet);
            trackedEntities.put(entityId, uuid);

            if (customName != null) {
                Object metadataPacket = createEntityMetadataPacket(entityId, customName, customNameVisible);
                send(player, metadataPacket);
            }
            return true;
        } catch (Exception e) {
            warn("Failed to spawn fake living entity: " + e.getMessage());
            return false;
        }
    }

    public int spawnFakeLivingEntity(Player player, UUID uuid, Location location,
            int entityTypeId, String customName, boolean customNameVisible) {
        int entityId = nextEntityId();
        spawnFakeLivingEntity(player, entityId, uuid, location, entityTypeId, customName, customNameVisible);
        return entityId;
    }

    public void equipFakeEntity(Player player, int entityId, int slot, Object nmsItemStack) {
        try {
            Object packet = createEntityEquipmentPacket(entityId, slot, nmsItemStack);
            send(player, packet);
        } catch (Exception e) {
            warn("Failed to equip fake entity: " + e.getMessage());
        }
    }

    public void equipFakeEntity(Player player, int entityId, Object[] armorSlots) {
        try {
            for (int slot = 0; slot < armorSlots.length; slot++) {
                if (armorSlots[slot] != null) {
                    equipFakeEntity(player, entityId, slot, armorSlots[slot]);
                }
            }
        } catch (Exception e) {
            warn("Failed to equip full armor for fake entity: " + e.getMessage());
        }
    }

    public void animateFakeEntity(Player player, int entityId, int animationId) {
        try {
            Object packet = createEntityAnimationPacket(entityId, animationId);
            send(player, packet);
        } catch (Exception e) {
            warn("Failed to animate fake entity: " + e.getMessage());
        }
    }

    public void statusEffectFakeEntity(Player player, int entityId, byte statusId) {
        try {
            Object packet = createEntityStatusPacket(entityId, statusId);
            send(player, packet);
        } catch (Exception e) {
            warn("Failed to send status effect for fake entity: " + e.getMessage());
        }
    }

    public void updateFakeEntityMetadata(Player player, int entityId, String customName, boolean customNameVisible) {
        try {
            Object packet = createEntityMetadataPacket(entityId, customName, customNameVisible);
            send(player, packet);
        } catch (Exception e) {
            warn("Failed to update fake entity metadata: " + e.getMessage());
        }
    }

    public void leashFakeEntity(Player player, int entityId, int leashHolderId) {
        try {
            Object packet = createEntityLeashPacket(entityId, leashHolderId);
            send(player, packet);
        } catch (Exception e) {
            warn("Failed to leash fake entity: " + e.getMessage());
        }
    }

    public void unleashFakeEntity(Player player, int entityId) {
        try {
            Object packet = createEntityLeashPacket(entityId, 0);
            send(player, packet);
        } catch (Exception e) {
            warn("Failed to unleash fake entity: " + e.getMessage());
        }
    }

    public void setFakeEntityPassengers(Player player, int vehicleId, int... passengerIds) {
        try {
            Object packet = createEntityPassengersPacket(vehicleId, passengerIds);
            send(player, packet);
        } catch (Exception e) {
            warn("Failed to set fake entity passengers: " + e.getMessage());
        }
    }

    public boolean destroyFakeEntity(Player player, int entityId) {
        try {
            Object packet = createEntityDestroyPacket(entityId);
            send(player, packet);
            trackedEntities.remove(entityId);
            return true;
        } catch (Exception e) {
            warn("Failed to destroy fake entity: " + e.getMessage());
            return false;
        }
    }

    public void destroyFakeEntity(Collection<? extends Player> players, int entityId) {
        for (Player p : players) {
            destroyFakeEntity(p, entityId);
        }
    }

    public boolean destroyFakeEntities(Player player, int... entityIds) {
        try {
            Object packet = createEntityDestroyPacket(entityIds);
            send(player, packet);
            for (int id : entityIds) {
                trackedEntities.remove(id);
            }
            return true;
        } catch (Exception e) {
            warn("Failed to destroy fake entities: " + e.getMessage());
            return false;
        }
    }

    public boolean teleportFakeEntity(Player player, int entityId, Location location) {
        try {
            Object packet = createEntityTeleportPacket(entityId, location);
            send(player, packet);
            return true;
        } catch (Exception e) {
            warn("Failed to teleport fake entity: " + e.getMessage());
            return false;
        }
    }

    public void teleportFakeEntity(Collection<? extends Player> players, int entityId, Location location) {
        for (Player p : players) {
            teleportFakeEntity(p, entityId, location);
        }
    }

    public boolean moveFakeEntity(Player player, int entityId, double deltaX, double deltaY, double deltaZ, boolean onGround) {
        try {
            Object packet = createEntityMovePacket(entityId, deltaX, deltaY, deltaZ, onGround);
            send(player, packet);
            return true;
        } catch (Exception e) {
            warn("Failed to move fake entity: " + e.getMessage());
            return false;
        }
    }

    public void moveFakeEntity(Collection<? extends Player> players, int entityId, double deltaX, double deltaY, double deltaZ, boolean onGround) {
        for (Player p : players) {
            moveFakeEntity(p, entityId, deltaX, deltaY, deltaZ, onGround);
        }
    }

    public boolean moveAndRotateFakeEntity(Player player, int entityId, double deltaX, double deltaY, double deltaZ,
            float yaw, float pitch, boolean onGround) {
        try {
            Object packet = createEntityMoveLookPacket(entityId, deltaX, deltaY, deltaZ, yaw, pitch, onGround);
            send(player, packet);
            return true;
        } catch (Exception e) {
            warn("Failed to move and rotate fake entity: " + e.getMessage());
            return false;
        }
    }

    public void rotateFakeEntity(Player player, int entityId, float yaw) {
        try {
            Object packet = createEntityHeadRotationPacket(entityId, yaw);
            send(player, packet);
        } catch (Exception e) {
            warn("Failed to rotate fake entity: " + e.getMessage());
        }
    }

    public void rotateFakeEntity(Collection<? extends Player> players, int entityId, float yaw) {
        for (Player p : players) {
            rotateFakeEntity(p, entityId, yaw);
        }
    }

    public boolean spawnFakeItem(Player player, int entityId, UUID uuid, Location location, int itemTypeId) {
        try {
            Object packet = createSpawnEntityPacket(entityId, uuid, location, 2, itemTypeId);
            send(player, packet);
            trackedEntities.put(entityId, uuid);
            return true;
        } catch (Exception e) {
            warn("Failed to spawn fake item: " + e.getMessage());
            return false;
        }
    }

    public int spawnFakeItem(Player player, UUID uuid, Location location, int itemTypeId) {
        int entityId = nextEntityId();
        spawnFakeItem(player, entityId, uuid, location, itemTypeId);
        return entityId;
    }

    public void spawnFakeItem(Collection<? extends Player> players, int entityId, UUID uuid, Location location, int itemTypeId) {
        for (Player p : players) {
            spawnFakeItem(p, entityId, uuid, location, itemTypeId);
        }
    }

    public void collectFakeEntity(Player player, int collectedId, int collectorId) {
        try {
            Object packet = createCollectItemPacket(collectedId, collectorId);
            send(player, packet);
        } catch (Exception e) {
            warn("Failed to send collect effect: " + e.getMessage());
        }
    }

    public void spawnParticle(Player player, String particleType, Location location,
            int count, float offsetX, float offsetY, float offsetZ, float speed) {
        try {
            Object packet = createParticlePacket(particleType, location, count, offsetX, offsetY, offsetZ, speed);
            send(player, packet);
        } catch (Exception e) {
            warn("Failed to spawn particle: " + e.getMessage());
        }
    }

    public void spawnParticle(Collection<? extends Player> players, String particleType, Location location,
            int count, float offsetX, float offsetY, float offsetZ, float speed) {
        for (Player p : players) {
            spawnParticle(p, particleType, location, count, offsetX, offsetY, offsetZ, speed);
        }
    }

    public void spawnParticleWorld(String worldName, String particleType, Location location,
            int count, float offsetX, float offsetY, float offsetZ, float speed) {
        for (Player p : Bukkit.getWorld(worldName).getPlayers()) {
            spawnParticle(p, particleType, location, count, offsetX, offsetY, offsetZ, speed);
        }
    }

    public void spawnParticleLine(Player player, String particleType, Location from, Location to,
            int points, float speed) {
        for (int i = 0; i <= points; i++) {
            double t = (double) i / points;
            Location point = from.clone().add(
                (to.getX() - from.getX()) * t,
                (to.getY() - from.getY()) * t,
                (to.getZ() - from.getZ()) * t
            );
            spawnParticle(player, particleType, point, 1, 0, 0, 0, speed);
        }
    }

    public void spawnParticleCircle(Player player, String particleType, Location center, double radius,
            int count, float speed) {
        for (int i = 0; i < count; i++) {
            double angle = 2 * Math.PI * i / count;
            double x = center.getX() + radius * Math.cos(angle);
            double z = center.getZ() + radius * Math.sin(angle);
            Location point = new Location(center.getWorld(), x, center.getY(), z);
            spawnParticle(player, particleType, point, 1, 0, 0, 0, speed);
        }
    }

    public void spawnParticleSphere(Player player, String particleType, Location center, double radius,
            int count, float speed) {
        for (int i = 0; i < count; i++) {
            double theta = Math.random() * 2 * Math.PI;
            double phi = Math.acos(2 * Math.random() - 1);
            double x = center.getX() + radius * Math.sin(phi) * Math.cos(theta);
            double y = center.getY() + radius * Math.sin(phi) * Math.sin(theta);
            double z = center.getZ() + radius * Math.cos(phi);
            Location point = new Location(center.getWorld(), x, y, z);
            spawnParticle(player, particleType, point, 1, 0, 0, 0, speed);
        }
    }

    public void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        try {

            Object timesPacket = createTitleTimesPacket(fadeIn, stay, fadeOut);
            send(player, timesPacket);

            if (title != null && !title.isEmpty()) {
                Object titlePacket = createTitleTextPacket(title);
                send(player, titlePacket);
            }

            if (subtitle != null && !subtitle.isEmpty()) {
                Object subtitlePacket = createSubtitleTextPacket(subtitle);
                send(player, subtitlePacket);
            }
        } catch (Exception e) {
            warn("Failed to send title: " + e.getMessage());
        }
    }

    public void sendTitle(Collection<? extends Player> players, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        for (Player p : players) {
            sendTitle(p, title, subtitle, fadeIn, stay, fadeOut);
        }
    }

    public void sendActionBar(Player player, String message) {
        try {
            Object packet = createActionBarPacket(message);
            send(player, packet);
        } catch (Exception e) {
            warn("Failed to send actionbar: " + e.getMessage());
        }
    }

    public void sendActionBar(Collection<? extends Player> players, String message) {
        for (Player p : players) {
            sendActionBar(p, message);
        }
    }

    public void clearTitle(Player player) {
        try {
            Object clearPacket = createClearTitlePacket();
            send(player, clearPacket);
        } catch (Exception e) {
            warn("Failed to clear title: " + e.getMessage());
        }
    }

    public void clearTitle(Collection<? extends Player> players) {
        for (Player p : players) {
            clearTitle(p);
        }
    }

    public void sendTitleAnimated(Player player, String title, String subtitle) {
        sendTitle(player, title, subtitle, 10, 70, 20);
    }

    public void playSound(Player player, String soundName, Location location, float volume, float pitch) {
        try {
            Object packet = createSoundPacket(soundName, location, volume, pitch);
            send(player, packet);
        } catch (Exception e) {
            warn("Failed to play sound: " + e.getMessage());
        }
    }

    public void playSound(Collection<? extends Player> players, String soundName, Location location, float volume, float pitch) {
        for (Player p : players) {
            playSound(p, soundName, location, volume, pitch);
        }
    }

    public void playSoundWorld(String worldName, String soundName, Location location, float volume, float pitch) {
        for (Player p : Bukkit.getWorld(worldName).getPlayers()) {
            playSound(p, soundName, location, volume, pitch);
        }
    }

    public void playSoundWithCategory(Player player, String soundName, Location location,
            float volume, float pitch, String category) {
        try {
            Object packet = createSoundPacketWithCategory(soundName, location, volume, pitch, category);
            send(player, packet);
        } catch (Exception e) {
            warn("Failed to play sound with category: " + e.getMessage());
        }
    }

    public void stopSound(Player player, String soundName) {
        try {
            Object packet = createStopSoundPacket(soundName);
            send(player, packet);
        } catch (Exception e) {
            warn("Failed to stop sound: " + e.getMessage());
        }
    }

    public void sendFakeChat(Player player, String senderName, String message) {
        try {
            Object packet = createChatPacket(senderName, message);
            send(player, packet);
        } catch (Exception e) {
            warn("Failed to send fake chat: " + e.getMessage());
        }
    }

    public void sendFakeChat(Collection<? extends Player> players, String senderName, String message) {
        for (Player p : players) {
            sendFakeChat(p, senderName, message);
        }
    }

    public void sendSystemMessage(Player player, String message) {
        try {
            Object packet = createSystemChatPacket(message);
            send(player, packet);
        } catch (Exception e) {
            warn("Failed to send system message: " + e.getMessage());
        }
    }

    public void sendSystemMessage(Collection<? extends Player> players, String message) {
        for (Player p : players) {
            sendSystemMessage(p, message);
        }
    }

    public void addFakePlayerInfo(Player player, String displayName, int ping) {
        try {
            Object packet = createPlayerInfoPacket(displayName, ping, true);
            send(player, packet);
        } catch (Exception e) {
            warn("Failed to add fake player info: " + e.getMessage());
        }
    }

    public void addFakePlayerInfo(Collection<? extends Player> players, String displayName, int ping) {
        for (Player p : players) {
            addFakePlayerInfo(p, displayName, ping);
        }
    }

    public void removeFakePlayerInfo(Player player, String displayName) {
        try {
            Object packet = createPlayerInfoPacket(displayName, 0, false);
            send(player, packet);
        } catch (Exception e) {
            warn("Failed to remove fake player info: " + e.getMessage());
        }
    }

    public void removeFakePlayerInfo(Collection<? extends Player> players, String displayName) {
        for (Player p : players) {
            removeFakePlayerInfo(p, displayName);
        }
    }

    public void setWorldBorder(Player player, double centerX, double centerZ, double radius, int warningDistance) {
        try {
            Object packet = createWorldBorderPacket(centerX, centerZ, radius, warningDistance);
            send(player, packet);
        } catch (Exception e) {
            warn("Failed to set world border: " + e.getMessage());
        }
    }

    public void setWorldBorder(Collection<? extends Player> players, double centerX, double centerZ, double radius, int warningDistance) {
        for (Player p : players) {
            setWorldBorder(p, centerX, centerZ, radius, warningDistance);
        }
    }

    public void removeWorldBorder(Player player) {
        setWorldBorder(player, 0, 0, 29999984, 5);
    }

    public void removeWorldBorder(Collection<? extends Player> players) {
        for (Player p : players) {
            removeWorldBorder(p);
        }
    }

    public void setWorldBorderSize(Player player, double centerX, double centerZ, double radius) {
        setWorldBorder(player, centerX, centerZ, radius, 5);
    }

    public void showBossBar(Player player, String title, float progress, String color) {
        try {
            UUID uuid = UUID.randomUUID();
            Object packet = createBossBarPacket(uuid, title, progress, color, (byte) 0);
            send(player, packet);
            activeBossBars.put(uuid, packet);
        } catch (Exception e) {
            warn("Failed to show boss bar: " + e.getMessage());
        }
    }

    public void showBossBar(Collection<? extends Player> players, String title, float progress, String color) {
        for (Player p : players) {
            showBossBar(p, title, progress, color);
        }
    }

    public void updateBossBar(Player player, String title, Float progress, String color) {
        try {
            Object packet = createBossBarPacket(null, title, progress != null ? progress : -1, color, (byte) 2);
            send(player, packet);
        } catch (Exception e) {
            warn("Failed to update boss bar: " + e.getMessage());
        }
    }

    public void updateBossBar(Collection<? extends Player> players, String title, Float progress, String color) {
        for (Player p : players) {
            updateBossBar(p, title, progress, color);
        }
    }

    public void hideBossBar(Player player, UUID uuid) {
        try {
            if (uuid != null) {
                Object packet = createBossBarPacket(uuid, null, 0, null, (byte) 1);
                send(player, packet);
                activeBossBars.remove(uuid);
            } else {
                for (UUID id : activeBossBars.keySet()) {
                    Object packet = createBossBarPacket(id, null, 0, null, (byte) 1);
                    send(player, packet);
                }
                activeBossBars.clear();
            }
        } catch (Exception e) {
            warn("Failed to hide boss bar: " + e.getMessage());
        }
    }

    public void hideBossBar(Collection<? extends Player> players, UUID uuid) {
        for (Player p : players) {
            hideBossBar(p, uuid);
        }
    }

    public boolean spawnFakeHologram(Player player, int entityId, UUID uuid, Location location, List<String> lines) {
        if (lines == null || lines.isEmpty()) return false;

        Location currentLoc = location.clone();
        boolean first = true;

        for (int i = lines.size() - 1; i >= 0; i--) {
            String line = lines.get(i);
            Location lineLoc = currentLoc.clone();
            lineLoc.setY(currentLoc.getY() + (lines.size() - 1 - i) * 0.25);

            if (first) {
                spawnFakeArmorStand(player, entityId + i, uuid, lineLoc, line, false, false, true, false);
                first = false;
            } else {
                spawnFakeArmorStand(player, entityId + i, UUID.randomUUID(), lineLoc, line, false, false, true, false);
            }
        }
        return true;
    }

    public int spawnFakeHologram(Player player, Location location, List<String> lines) {
        int baseId = nextEntityId();
        spawnFakeHologram(player, baseId, UUID.randomUUID(), location, lines);
        return baseId;
    }

    public boolean spawnFakeHologram(Collection<? extends Player> players, int entityId, UUID uuid, Location location, List<String> lines) {
        for (Player p : players) {
            spawnFakeHologram(p, entityId, uuid, location, lines);
        }
        return true;
    }

    public void destroyFakeHologram(Player player, int baseEntityId, int lineCount) {
        for (int i = 0; i < lineCount; i++) {
            destroyFakeEntity(player, baseEntityId + i);
        }
    }

    public void destroyFakeHologram(Collection<? extends Player> players, int baseEntityId, int lineCount) {
        for (Player p : players) {
            destroyFakeHologram(p, baseEntityId, lineCount);
        }
    }

    public void createFakeTeam(Player player, String teamName, String prefix, String suffix,
            String color, boolean friendlyFire, boolean seeInvisible) {
        try {
            FakeTeam team = new FakeTeam(teamName, prefix, suffix, color);
            fakeTeams.put(teamName, team);
            Object packet = createScoreboardTeamPacket(teamName, prefix, suffix, color,
                    friendlyFire, seeInvisible, (byte) 0);
            send(player, packet);
        } catch (Exception e) {
            warn("Failed to create fake team: " + e.getMessage());
        }
    }

    public void removeFakeTeam(Player player, String teamName) {
        try {
            fakeTeams.remove(teamName);
            Object packet = createScoreboardTeamPacket(teamName, "", "", "white",
                    false, false, (byte) 1);
            send(player, packet);
        } catch (Exception e) {
            warn("Failed to remove fake team: " + e.getMessage());
        }
    }

    public void addPlayersToFakeTeam(Player player, String teamName, String... playerNames) {
        try {
            Object packet = createTeamPlayersPacket(teamName, playerNames, (byte) 3);
            send(player, packet);
        } catch (Exception e) {
            warn("Failed to add players to fake team: " + e.getMessage());
        }
    }

    public void removePlayersFromFakeTeam(Player player, String teamName, String... playerNames) {
        try {
            Object packet = createTeamPlayersPacket(teamName, playerNames, (byte) 4);
            send(player, packet);
        } catch (Exception e) {
            warn("Failed to remove players from fake team: " + e.getMessage());
        }
    }

    public void setFakeExperience(Player player, float progress, int level, int totalExperience) {
        try {
            Object packet = createExperiencePacket(progress, level, totalExperience);
            send(player, packet);
        } catch (Exception e) {
            warn("Failed to set fake experience: " + e.getMessage());
        }
    }

    public void setFakeGameState(Player player, int state, float value) {
        try {
            Object packet = createGameStatePacket(state, value);
            send(player, packet);
        } catch (Exception e) {
            warn("Failed to set fake game state: " + e.getMessage());
        }
    }

    public void setFakeCamera(Player player, int entityId) {
        try {
            Object packet = createCameraPacket(entityId);
            send(player, packet);
        } catch (Exception e) {
            warn("Failed to set fake camera: " + e.getMessage());
        }
    }

    public void resetFakeCamera(Player player) {
        setFakeCamera(player, player.getEntityId());
    }

    public void sendFakeBlockChange(Player player, Location location, int blockId, byte blockData) {
        try {
            Object packet = createBlockChangePacket(location, blockId, blockData);
            send(player, packet);
        } catch (Exception e) {
            warn("Failed to send fake block change: " + e.getMessage());
        }
    }

    public void sendFakeBlockChanges(Player player, Map<Location, int[]> blockChanges) {
        for (Map.Entry<Location, int[]> entry : blockChanges.entrySet()) {
            Location loc = entry.getKey();
            int[] idAndData = entry.getValue();
            sendFakeBlockChange(player, loc, idAndData[0], (byte) idAndData[1]);
        }
    }

    public void sendFakeBlockChange(Player player, Location location, org.bukkit.Material material) {
        sendFakeBlockChange(player, location, material.getId(), (byte) 0);
    }

    public void sendFakeLightUpdate(Player player, int chunkX, int chunkZ, int[] skyLight, int[] blockLight) {
        try {
            Object packet = createLightUpdatePacket(chunkX, chunkZ, skyLight, blockLight);
            send(player, packet);
        } catch (Exception e) {
            warn("Failed to send fake light update: " + e.getMessage());
        }
    }

    public void sendCollectItem(Player player, int collectedEntityId, int collectorEntityId) {
        try {
            Object packet = createCollectItemPacket(collectedEntityId, collectorEntityId);
            send(player, packet);
        } catch (Exception e) {
            warn("Failed to send collect item effect: " + e.getMessage());
        }
    }

    public UUID getTrackedEntityUUID(int entityId) {
        return trackedEntities.get(entityId);
    }

    public boolean isEntityTracked(int entityId) {
        return trackedEntities.containsKey(entityId);
    }

    public Set<Integer> getTrackedEntityIds() {
        return Collections.unmodifiableSet(trackedEntities.keySet());
    }

    public void clearTrackedEntities() {
        trackedEntities.clear();
    }

    public int getCurrentEntityIdCounter() {
        return entityIdCounter.get();
    }

    private Object createArmorStandPacket(int entityId, UUID uuid, Location loc, String name,
            boolean small, boolean marker, boolean invisible, boolean noGravity) throws Exception {
        Class<?> entityArmorStand = Reflection.getNMS("EntityArmorStand");
        Class<?> worldClass = Reflection.getNMS("World");
        Class<?> packetPlayOutSpawnEntity = Reflection.getNMS("PacketPlayOutSpawnEntity");

        Object worldServer = Reflection.get(Reflection.invoke(loc.getWorld(), "getHandle"), "world");
        Object entity = entityArmorStand.getDeclaredConstructor(worldClass).newInstance(worldServer);

        Reflection.set(entity, "yaw", (float) loc.getYaw());
        Reflection.set(entity, "pitch", (float) loc.getPitch());
        Reflection.set(entity, "locX", loc.getX());
        Reflection.set(entity, "locY", loc.getY());
        Reflection.set(entity, "locZ", loc.getZ());
        Reflection.set(entity, "aI", !noGravity);

        Object packet = packetPlayOutSpawnEntity.getDeclaredConstructor().newInstance();
        Reflection.set(packet, "a", entityId);
        Reflection.set(packet, "b", uuid);
        Reflection.set(packet, "c", loc.getX());
        Reflection.set(packet, "d", loc.getY());
        Reflection.set(packet, "e", loc.getZ());

        return packet;
    }

    private Object createSpawnEntityLivingPacket(int entityId, UUID uuid, Location loc, int entityTypeId) throws Exception {
        Class<?> packetClass = Reflection.getNMS("PacketPlayOutSpawnEntityLiving");
        Object packet = packetClass.getDeclaredConstructor().newInstance();
        Reflection.set(packet, "a", entityId);
        Reflection.set(packet, "b", uuid);
        Reflection.set(packet, "c", entityTypeId);
        Reflection.set(packet, "d", (int) (loc.getX() * 32));
        Reflection.set(packet, "e", (int) (loc.getY() * 32));
        Reflection.set(packet, "f", (int) (loc.getZ() * 32));
        Reflection.set(packet, "g", (int) (loc.getYaw() * 256.0 / 360.0) & 0xFF);
        Reflection.set(packet, "h", (int) (loc.getPitch() * 256.0 / 360.0) & 0xFF);
        Reflection.set(packet, "i", (byte) loc.getYaw());
        Reflection.set(packet, "j", (byte) loc.getPitch());
        return packet;
    }

    private Object createEntityDestroyPacket(int... entityIds) throws Exception {
        Class<?> packetClass = Reflection.getNMS("PacketPlayOutEntityDestroy");
        Object packet = packetClass.getDeclaredConstructor().newInstance();
        Reflection.set(packet, "a", entityIds);
        return packet;
    }

    private Object createEntityTeleportPacket(int entityId, Location loc) throws Exception {
        Class<?> packetClass = Reflection.getNMS("PacketPlayOutEntityTeleport");
        Object packet = packetClass.getDeclaredConstructor().newInstance();
        Reflection.set(packet, "a", entityId);
        Reflection.set(packet, "b", (int) (loc.getX() * 32));
        Reflection.set(packet, "c", (int) (loc.getY() * 32));
        Reflection.set(packet, "d", (int) (loc.getZ() * 32));
        Reflection.set(packet, "e", (byte) ((int) (loc.getYaw() * 256.0 / 360.0) & 0xFF));
        Reflection.set(packet, "f", (byte) ((int) (loc.getPitch() * 256.0 / 360.0) & 0xFF));
        Reflection.set(packet, "g", false);
        return packet;
    }

    private Object createEntityMovePacket(int entityId, double deltaX, double deltaY, double deltaZ, boolean onGround) throws Exception {
        Class<?> packetClass = Reflection.getNMS("PacketPlayOutRelEntityMove");
        Object packet = packetClass.getDeclaredConstructor().newInstance();
        Reflection.set(packet, "a", entityId);
        Reflection.set(packet, "b", (short) (deltaX * 32));
        Reflection.set(packet, "c", (short) (deltaY * 32));
        Reflection.set(packet, "d", (short) (deltaZ * 32));
        Reflection.set(packet, "e", onGround);
        return packet;
    }

    private Object createEntityMoveLookPacket(int entityId, double deltaX, double deltaY, double deltaZ,
            float yaw, float pitch, boolean onGround) throws Exception {
        Class<?> packetClass = Reflection.getNMS("PacketPlayOutRelEntityMoveLook");
        Object packet = packetClass.getDeclaredConstructor().newInstance();
        Reflection.set(packet, "a", entityId);
        Reflection.set(packet, "b", (short) (deltaX * 32));
        Reflection.set(packet, "c", (short) (deltaY * 32));
        Reflection.set(packet, "d", (short) (deltaZ * 32));
        Reflection.set(packet, "e", (byte) ((int) (yaw * 256.0 / 360.0) & 0xFF));
        Reflection.set(packet, "f", (byte) ((int) (pitch * 256.0 / 360.0) & 0xFF));
        Reflection.set(packet, "g", onGround);
        return packet;
    }

    private Object createEntityHeadRotationPacket(int entityId, float yaw) throws Exception {
        Class<?> packetClass = Reflection.getNMS("PacketPlayOutEntityHeadRotation");
        Object packet = packetClass.getDeclaredConstructor().newInstance();
        Reflection.set(packet, "a", entityId);
        Reflection.set(packet, "b", (byte) ((int) (yaw * 256.0 / 360.0) & 0xFF));
        return packet;
    }

    private Object createSpawnEntityPacket(int entityId, UUID uuid, Location loc, int type, int itemTypeId) throws Exception {
        Class<?> packetClass = Reflection.getNMS("PacketPlayOutSpawnEntity");
        Object packet = packetClass.getDeclaredConstructor().newInstance();
        Reflection.set(packet, "a", entityId);
        Reflection.set(packet, "b", uuid);
        Reflection.set(packet, "c", (int) (loc.getX() * 32));
        Reflection.set(packet, "d", (int) (loc.getY() * 32));
        Reflection.set(packet, "e", (int) (loc.getZ() * 32));
        Reflection.set(packet, "f", (int) (loc.getYaw() * 256.0 / 360.0) & 0xFF);
        Reflection.set(packet, "g", (int) (loc.getPitch() * 256.0 / 360.0) & 0xFF);
        Reflection.set(packet, "h", type);
        return packet;
    }

    private Object createEntityEquipmentPacket(int entityId, int slot, Object nmsItemStack) throws Exception {
        Class<?> packetClass = Reflection.getNMS("PacketPlayOutEntityEquipment");
        Object packet = packetClass.getDeclaredConstructor().newInstance();
        Reflection.set(packet, "a", entityId);
        Reflection.set(packet, "b", slot);
        Reflection.set(packet, "c", nmsItemStack);
        return packet;
    }

    private Object createEntityAnimationPacket(int entityId, int animationId) throws Exception {
        Class<?> packetClass = Reflection.getNMS("PacketPlayOutAnimation");
        Object packet = packetClass.getDeclaredConstructor().newInstance();
        Reflection.set(packet, "a", entityId);
        Reflection.set(packet, "b", animationId);
        return packet;
    }

    private Object createEntityStatusPacket(int entityId, byte statusId) throws Exception {
        Class<?> packetClass = Reflection.getNMS("PacketPlayOutEntityStatus");
        Object packet = packetClass.getDeclaredConstructor().newInstance();
        Reflection.set(packet, "a", entityId);
        Reflection.set(packet, "b", statusId);
        return packet;
    }

    private Object createEntityMetadataPacket(int entityId, String customName, boolean customNameVisible) throws Exception {
        Class<?> packetClass = Reflection.getNMS("PacketPlayOutEntityMetadata");
        Object packet = packetClass.getDeclaredConstructor().newInstance();
        Reflection.set(packet, "a", entityId);

        return packet;
    }

    private Object createEntityLeashPacket(int entityId, int leashHolderId) throws Exception {
        Class<?> packetClass = Reflection.getNMS("PacketPlayOutEntityAttach");
        Object packet = packetClass.getDeclaredConstructor().newInstance();
        Reflection.set(packet, "a", leashHolderId);
        Reflection.set(packet, "b", entityId);
        return packet;
    }

    private Object createEntityPassengersPacket(int vehicleId, int... passengerIds) throws Exception {
        Class<?> packetClass = Reflection.getNMS("PacketPlayOutMount");
        Object packet = packetClass.getDeclaredConstructor().newInstance();
        Reflection.set(packet, "a", vehicleId);
        Reflection.set(packet, "b", passengerIds);
        return packet;
    }

    private Object createParticlePacket(String particleType, Location loc, int count,
            float offsetX, float offsetY, float offsetZ, float speed) throws Exception {
        Class<?> packetClass = Reflection.getNMS("PacketPlayOutWorldParticles");
        Object packet = packetClass.getDeclaredConstructor().newInstance();
        Reflection.set(packet, "a", 0);
        Reflection.set(packet, "b", (float) loc.getX());
        Reflection.set(packet, "c", (float) loc.getY());
        Reflection.set(packet, "d", (float) loc.getZ());
        Reflection.set(packet, "e", offsetX);
        Reflection.set(packet, "f", offsetY);
        Reflection.set(packet, "g", offsetZ);
        Reflection.set(packet, "h", speed);
        Reflection.set(packet, "i", count);
        return packet;
    }

    private Object createTitleTimesPacket(int fadeIn, int stay, int fadeOut) throws Exception {
        Class<?> packetClass = Reflection.getNMS("PacketPlayOutTitle");
        Object packet = packetClass.getDeclaredConstructor().newInstance();
        Reflection.set(packet, "a", fadeIn);
        Reflection.set(packet, "b", stay);
        Reflection.set(packet, "c", fadeOut);
        return packet;
    }

    private Object createTitleTextPacket(String title) throws Exception {
        Class<?> packetClass = Reflection.getNMS("PacketPlayOutTitle");
        Class<?> chatComponentClass = Reflection.getNMS("IChatBaseComponent");
        Object packet = packetClass.getDeclaredConstructor().newInstance();
        Object component = Reflection.createChatComponent("{\"text\":\"" + escapeJson(title) + "\"}");
        Reflection.set(packet, "a", component);
        return packet;
    }

    private Object createSubtitleTextPacket(String subtitle) throws Exception {
        Class<?> packetClass = Reflection.getNMS("PacketPlayOutTitle");
        Class<?> chatComponentClass = Reflection.getNMS("IChatBaseComponent");
        Object packet = packetClass.getDeclaredConstructor().newInstance();
        Object component = Reflection.createChatComponent("{\"text\":\"" + escapeJson(subtitle) + "\"}");
        Reflection.set(packet, "a", component);
        return packet;
    }

    private Object createClearTitlePacket() throws Exception {
        Class<?> packetClass = Reflection.getNMS("PacketPlayOutTitle");
        Object packet = packetClass.getDeclaredConstructor().newInstance();
        Reflection.set(packet, "a", 0);
        Reflection.set(packet, "b", 0);
        Reflection.set(packet, "c", 0);
        return packet;
    }

    private Object createActionBarPacket(String message) throws Exception {
        Class<?> chatComponentClass = Reflection.getNMS("IChatBaseComponent");
        Class<?> packetClass = Reflection.getNMS("PacketPlayOutChat");
        Object component = Reflection.createChatComponent("{\"text\":\"" + escapeJson(message) + "\"}");
        Object packet = packetClass.getDeclaredConstructor(chatComponentClass, byte.class).newInstance(component, (byte) 2);
        return packet;
    }

    private Object createSoundPacket(String soundName, Location loc, float volume, float pitch) throws Exception {
        Class<?> packetClass = Reflection.getNMS("PacketPlayOutNamedSoundEffect");
        Object packet = packetClass.getDeclaredConstructor().newInstance();
        Reflection.set(packet, "a", soundName);
        Reflection.set(packet, "b", (int) (loc.getX() * 8));
        Reflection.set(packet, "c", (int) (loc.getY() * 8));
        Reflection.set(packet, "d", (int) (loc.getZ() * 8));
        Reflection.set(packet, "e", (int) (volume * 63.0));
        Reflection.set(packet, "f", (int) (pitch * 63.0));
        return packet;
    }

    private Object createSoundPacketWithCategory(String soundName, Location loc, float volume, float pitch, String category) throws Exception {
        Class<?> packetClass = Reflection.getNMS("PacketPlayOutNamedSoundEffect");
        Object packet = packetClass.getDeclaredConstructor().newInstance();
        Reflection.set(packet, "a", soundName);
        Reflection.set(packet, "b", (int) (loc.getX() * 8));
        Reflection.set(packet, "c", (int) (loc.getY() * 8));
        Reflection.set(packet, "d", (int) (loc.getZ() * 8));
        Reflection.set(packet, "e", (int) (volume * 63.0));
        Reflection.set(packet, "f", (int) (pitch * 63.0));
        Reflection.set(packet, "g", category);
        return packet;
    }

    private Object createStopSoundPacket(String soundName) throws Exception {
        Class<?> packetClass = Reflection.getNMS("PacketPlayOutStopSound");
        Object packet = packetClass.getDeclaredConstructor().newInstance();
        Reflection.set(packet, "a", soundName);
        return packet;
    }

    private Object createChatPacket(String senderName, String message) throws Exception {
        Class<?> chatComponentClass = Reflection.getNMS("IChatBaseComponent");
        Class<?> packetClass = Reflection.getNMS("PacketPlayOutChat");
        Object component = Reflection.createChatComponent(
            "{\"text\":\"" + escapeJson(message) + "\",\"extra\":[{\"text\":\"" + escapeJson(senderName) + "\"}]}");
        Object packet = packetClass.getDeclaredConstructor(chatComponentClass, byte.class).newInstance(component, (byte) 0);
        return packet;
    }

    private Object createSystemChatPacket(String message) throws Exception {
        Class<?> chatComponentClass = Reflection.getNMS("IChatBaseComponent");
        Class<?> packetClass = Reflection.getNMS("PacketPlayOutChat");
        Object component = Reflection.createChatComponent("{\"text\":\"" + escapeJson(message) + "\"}");
        Object packet = packetClass.getDeclaredConstructor(chatComponentClass, byte.class).newInstance(component, (byte) 1);
        return packet;
    }

    private Object createPlayerInfoPacket(String displayName, int ping, boolean add) throws Exception {
        Class<?> packetClass = Reflection.getNMS("PacketPlayOutPlayerInfo");
        Object packet = packetClass.getDeclaredConstructor().newInstance();
        Reflection.set(packet, "b", displayName);
        Reflection.set(packet, "c", ping);
        Reflection.set(packet, "d", add ? 0 : 4);
        return packet;
    }

    private Object createWorldBorderPacket(double centerX, double centerZ, double radius, int warningDistance) throws Exception {
        Class<?> packetClass = Reflection.getNMS("PacketPlayOutWorldBorder");
        Object packet = packetClass.getDeclaredConstructor().newInstance();
        Reflection.set(packet, "a", centerX);
        Reflection.set(packet, "b", centerZ);
        Reflection.set(packet, "c", radius);
        Reflection.set(packet, "d", warningDistance);
        return packet;
    }

    private Object createBossBarPacket(UUID uuid, String title, float progress, String color, byte action) throws Exception {
        Class<?> packetClass = Reflection.getNMS("PacketPlayOutBoss");
        Class<?> chatComponentClass = Reflection.getNMS("IChatBaseComponent");
        Object packet = packetClass.getDeclaredConstructor().newInstance();

        if (uuid != null) {
            java.lang.reflect.Field uuidField = packetClass.getDeclaredField("a");
            uuidField.setAccessible(true);
            uuidField.set(packet, uuid);
        }

        if (title != null) {
            Object titleComponent = Reflection.createChatComponent("{\"text\":\"" + escapeJson(title) + "\"}");
            Reflection.set(packet, "c", titleComponent);
        }

        if (progress >= 0) {
            Reflection.set(packet, "d", progress);
        }

        if (color != null) {
            Reflection.set(packet, "e", Reflection.getEnum("EnumDifficulty", color));
        }

        Reflection.set(packet, "f", action);
        return packet;
    }

    private Object createScoreboardTeamPacket(String teamName, String prefix, String suffix,
            String color, boolean friendlyFire, boolean seeInvisible, byte mode) throws Exception {
        Class<?> packetClass = Reflection.getNMS("PacketPlayOutScoreboardTeam");
        Object packet = packetClass.getDeclaredConstructor().newInstance();
        Reflection.set(packet, "a", teamName);
        Reflection.set(packet, "b", prefix);
        Reflection.set(packet, "c", suffix);
        Reflection.set(packet, "d", color);
        Reflection.set(packet, "e", mode);
        return packet;
    }

    private Object createTeamPlayersPacket(String teamName, String[] playerNames, byte mode) throws Exception {
        Class<?> packetClass = Reflection.getNMS("PacketPlayOutScoreboardTeam");
        Object packet = packetClass.getDeclaredConstructor().newInstance();
        Reflection.set(packet, "a", teamName);
        Reflection.set(packet, "b", playerNames);
        Reflection.set(packet, "e", mode);
        return packet;
    }

    private Object createExperiencePacket(float progress, int level, int totalExperience) throws Exception {
        Class<?> packetClass = Reflection.getNMS("PacketPlayOutExperience");
        Object packet = packetClass.getDeclaredConstructor().newInstance();
        Reflection.set(packet, "a", progress);
        Reflection.set(packet, "b", level);
        Reflection.set(packet, "c", totalExperience);
        return packet;
    }

    private Object createGameStatePacket(int state, float value) throws Exception {
        Class<?> packetClass = Reflection.getNMS("PacketPlayOutGameStateChange");
        Object packet = packetClass.getDeclaredConstructor().newInstance();
        Reflection.set(packet, "a", state);
        Reflection.set(packet, "b", value);
        return packet;
    }

    private Object createCameraPacket(int entityId) throws Exception {
        Class<?> packetClass = Reflection.getNMS("PacketPlayOutCamera");
        Object packet = packetClass.getDeclaredConstructor().newInstance();
        Reflection.set(packet, "a", entityId);
        return packet;
    }

    private Object createBlockChangePacket(Location location, int blockId, byte blockData) throws Exception {
        Class<?> packetClass = Reflection.getNMS("PacketPlayOutBlockChange");
        Object packet = packetClass.getDeclaredConstructor().newInstance();
        Reflection.set(packet, "a", location.getBlockX());
        Reflection.set(packet, "b", location.getBlockY());
        Reflection.set(packet, "c", location.getBlockZ());
        Reflection.set(packet, "d", blockId);
        Reflection.set(packet, "e", blockData);
        return packet;
    }

    private Object createLightUpdatePacket(int chunkX, int chunkZ, int[] skyLight, int[] blockLight) throws Exception {
        Class<?> packetClass = Reflection.getNMS("PacketPlayOutLightUpdate");
        Object packet = packetClass.getDeclaredConstructor().newInstance();
        Reflection.set(packet, "a", chunkX);
        Reflection.set(packet, "b", chunkZ);
        if (skyLight != null) Reflection.set(packet, "c", skyLight);
        if (blockLight != null) Reflection.set(packet, "d", blockLight);
        return packet;
    }

    private Object createCollectItemPacket(int collectedId, int collectorId) throws Exception {
        Class<?> packetClass = Reflection.getNMS("PacketPlayOutCollect");
        Object packet = packetClass.getDeclaredConstructor().newInstance();
        Reflection.set(packet, "a", collectedId);
        Reflection.set(packet, "b", collectorId);
        Reflection.set(packet, "c", 1);
        return packet;
    }

    private void send(Player player, Object packet) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(packet, "packet");
        try {
            Reflection.sendPacket(player, packet);
        } catch (Exception e) {
            String msg = "Failed to send packet to " + player.getName() + ": " + e.getMessage();
            warn(msg);
            if (errorHandler != null) {
                errorHandler.accept(msg);
            }
        }
    }

    private void warn(String message) {
        if (logger != null) {
            logger.warning(message);
        }
    }

    private String normalizeId(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        return id;
    }

    private List<Object> asList(Object[] packets) {
        if (packets == null || packets.length == 0) {
            return Collections.emptyList();
        }
        List<Object> list = new ArrayList<>(packets.length);
        for (Object packet : packets) {
            if (packet != null) list.add(packet);
        }
        return Collections.unmodifiableList(list);
    }

    private List<Object> asList(Collection<?> packets) {
        if (packets == null || packets.isEmpty()) {
            return Collections.emptyList();
        }
        List<Object> list = new ArrayList<>(packets.size());
        for (Object packet : packets) {
            if (packet != null) list.add(packet);
        }
        return Collections.unmodifiableList(list);
    }

    private static String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }

    public static final class ClientSideInstance {

        private final String id;
        private final List<Object> spawnPackets;
        private final List<Object> destroyPackets;

        private ClientSideInstance(String id, List<Object> spawnPackets, List<Object> destroyPackets) {
            this.id = id;
            this.spawnPackets = spawnPackets;
            this.destroyPackets = destroyPackets;
        }

        public String getId() { return id; }
        public List<Object> getSpawnPackets() { return spawnPackets; }
        public List<Object> getDestroyPackets() { return destroyPackets; }
    }

    public static final class FakeTeam {
        private final String name;
        private final String prefix;
        private final String suffix;
        private final String color;

        public FakeTeam(String name, String prefix, String suffix, String color) {
            this.name = name;
            this.prefix = prefix;
            this.suffix = suffix;
            this.color = color;
        }

        public String getName() { return name; }
        public String getPrefix() { return prefix; }
        public String getSuffix() { return suffix; }
        public String getColor() { return color; }
    }

    public static final class FakeScoreboard {
        private final String name;
        private final String displayName;
        private final Map<Integer, String> lines;

        public FakeScoreboard(String name, String displayName) {
            this.name = name;
            this.displayName = displayName;
            this.lines = new LinkedHashMap<>();
        }

        public FakeScoreboard line(int score, String text) {
            this.lines.put(score, text);
            return this;
        }

        public String getName() { return name; }
        public String getDisplayName() { return displayName; }
        public Map<Integer, String> getLines() { return Collections.unmodifiableMap(lines); }
    }
}
