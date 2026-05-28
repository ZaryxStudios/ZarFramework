package com.zaryx.okaso.bukkit.simulation;

import com.zaryx.okaso.bukkit.utility.Reflection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.logging.Logger;

/**
 * High-level helpers for client-side packet simulation.
 *
 * This service intentionally stays version-agnostic: callers build the packet objects
 * they need and the service handles dispatching, replaying and instance lifecycle.
 */
public class PacketSimulationService {

    private final Logger logger;
    private final Map<String, ClientSideInstance> instances;
    private final Set<String> fakeBlockInstances;
    private final Set<String> fakeStructureInstances;

    public PacketSimulationService(Logger logger) {
        this.logger = logger;
        this.instances = new ConcurrentHashMap<>();
        this.fakeBlockInstances = ConcurrentHashMap.newKeySet();
        this.fakeStructureInstances = ConcurrentHashMap.newKeySet();
    }

    public void spoofPacket(Player player, Object packet) {
        send(player, packet);
    }

    public void spoofPacket(Player player, Function<Player, ?> packetFactory) {
        Objects.requireNonNull(packetFactory, "packetFactory");
        Object packet = packetFactory.apply(player);
        if (packet != null) {
            send(player, packet);
        }
    }

    public void spoofPackets(Player player, Object... packets) {
        if (packets == null) {
            return;
        }

        for (Object packet : packets) {
            send(player, packet);
        }
    }

    public void spoofPackets(Player player, Collection<?> packets) {
        if (packets == null) {
            return;
        }

        for (Object packet : packets) {
            send(player, packet);
        }
    }

    public void spoofPacket(Collection<? extends Player> players, Object packet) {
        if (players == null || players.isEmpty()) {
            return;
        }

        for (Player player : players) {
            send(player, packet);
        }
    }

    public void spoofPacket(Collection<? extends Player> players, Function<Player, ?> packetFactory) {
        Objects.requireNonNull(packetFactory, "packetFactory");
        if (players == null || players.isEmpty()) {
            return;
        }

        for (Player player : players) {
            send(player, packetFactory.apply(player));
        }
    }

    public void spoofPackets(Collection<? extends Player> players, Object... packets) {
        if (players == null || players.isEmpty() || packets == null) {
            return;
        }

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
        if (players == null || players.isEmpty()) {
            return;
        }

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

    public void spawnClientSideInstance(Collection<? extends Player> players, String id) {
        if (players == null || players.isEmpty()) {
            return;
        }
        for (Player player : players) {
            spawnClientSideInstance(player, id);
        }
    }

    public void destroyClientSideInstance(Collection<? extends Player> players, String id) {
        if (players == null || players.isEmpty()) {
            return;
        }
        for (Player player : players) {
            destroyClientSideInstance(player, id);
        }
    }

    public Set<String> getFakeBlockInstanceIds() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(fakeBlockInstances));
    }

    public Set<String> getFakeStructureInstanceIds() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(fakeStructureInstances));
    }

    private void send(Player player, Object packet) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(packet, "packet");
        Reflection.sendPacket(player, packet);
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
            if (packet != null) {
                list.add(packet);
            }
        }
        return Collections.unmodifiableList(list);
    }

    private List<Object> asList(Collection<?> packets) {
        if (packets == null || packets.isEmpty()) {
            return Collections.emptyList();
        }

        List<Object> list = new ArrayList<>(packets.size());
        for (Object packet : packets) {
            if (packet != null) {
                list.add(packet);
            }
        }
        return Collections.unmodifiableList(list);
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

        public String getId() {
            return id;
        }

        public List<Object> getSpawnPackets() {
            return spawnPackets;
        }

        public List<Object> getDestroyPackets() {
            return destroyPackets;
        }
    }
}
