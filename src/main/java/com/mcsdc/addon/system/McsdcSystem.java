package com.mcsdc.addon.system;

import meteordevelopment.meteorclient.systems.System;
import meteordevelopment.meteorclient.systems.Systems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.ListTag;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class McsdcSystem extends System<McsdcSystem> {
    public static final int MAX_RECENT_SERVERS = 100;

    private String token = "";
    private String username = "";
    private String lastServer = "";
    private boolean shareLocation = true;
    private final List<ServerStorage> serverQueue = new ArrayList<>();
    private int currentServerIndex = 0;

    private final LinkedHashMap<String, ServerStorage> recentServers = new LinkedHashMap<>();

    public McsdcSystem() {
        super("McsdcSystem");
    }

    public static McsdcSystem get() {
        return Systems.get(McsdcSystem.class);
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        // stray whitespace/newlines from pasting would make HttpClient reject the auth header
        this.token = token == null ? "" : token.trim();
    }

    public boolean isShareLocation() {
        return shareLocation;
    }

    public void setShareLocation(boolean shareLocation) {
        this.shareLocation = shareLocation;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public List<ServerStorage> getRecentServers() {
        return new ArrayList<>(recentServers.values());
    }

    public void addRecentServer(ServerStorage server) {
        recentServers.remove(server.ip());
        recentServers.put(server.ip(), server);

        while (recentServers.size() > MAX_RECENT_SERVERS) {
            String oldestKey = recentServers.keySet().iterator().next();
            recentServers.remove(oldestKey);
        }
    }

    public void removeRecentServer(ServerStorage server) {
        recentServers.remove(server.ip());
    }

    public void clearRecentServers() {
        recentServers.clear();
    }

    public String getLastServer() {
        return lastServer;
    }

    public void setLastServer(String lastServer) {
        this.lastServer = lastServer;
    }

    public void setServerQueue(List<ServerStorage> servers) {
        serverQueue.clear();
        serverQueue.addAll(servers);
        currentServerIndex = 0;
    }

    public ServerStorage getNextServer() {
        if (!hasServerQueue()) return null;

        while (currentServerIndex < serverQueue.size()) {
            ServerStorage server = serverQueue.get(currentServerIndex++);
            if (!server.ip().equals(lastServer)) return server;
        }

        clearServerQueue();
        return null;
    }

    public boolean hasServerQueue() {
        return !serverQueue.isEmpty() && currentServerIndex < serverQueue.size();
    }

    public void clearServerQueue() {
        serverQueue.clear();
        currentServerIndex = 0;
    }

    @Override
    public CompoundTag toTag() {
        CompoundTag compound = new CompoundTag();
        compound.putString("token", this.token);
        compound.putString("username", this.username);
        compound.putBoolean("shareLocation", this.shareLocation);

        ListTag list = new ListTag();

        recentServers.values().forEach((server) -> {
            CompoundTag compound2 = new CompoundTag();
            compound2.putString("ip", server.ip());
            compound2.putString("version", server.version());
            list.add(compound2);
        });

        compound.put("recent", list);

        ListTag queueList = new ListTag();
        serverQueue.forEach((server) -> {
            CompoundTag queueEntry = new CompoundTag();
            queueEntry.putString("ip", server.ip());
            queueEntry.putString("version", server.version());
            if (server.lastScanned() != null) queueEntry.putLong("lastScanned", server.lastScanned());
            if (server.lastSeen() != null) queueEntry.putLong("lastSeen", server.lastSeen());
            queueList.add(queueEntry);
        });
        compound.put("serverQueue", queueList);
        compound.putInt("currentServerIndex", this.currentServerIndex);

        return compound;
    }

    @Override
    public McsdcSystem fromTag(CompoundTag tag) {
        this.token = tag.getString("token").orElse("");
        this.username = tag.getString("username").orElse("");
        this.shareLocation = tag.getBoolean("shareLocation").orElse(true);

        // keep insertion order (oldest first) so eviction in addRecentServer stays correct
        ListTag list = tag.getList("recent").orElse(new ListTag());
        for (Tag element : list) {
            CompoundTag compound = (CompoundTag) element;
            String ip = compound.getString("ip").orElse("");
            String ver = compound.getString("version").orElse("");
            recentServers.put(ip, new ServerStorage(ip, ver, null, null));
        }

        serverQueue.clear();
        ListTag queueList = tag.getList("serverQueue").orElse(new ListTag());
        for (Tag element : queueList) {
            CompoundTag queueEntry = (CompoundTag) element;
            String ip = queueEntry.getString("ip").orElse("");
            String version = queueEntry.getString("version").orElse("");
            Long lastScanned = queueEntry.getLong("lastScanned").orElse(null);
            Long lastSeen = queueEntry.getLong("lastSeen").orElse(null);
            serverQueue.add(new ServerStorage(ip, version, lastScanned, lastSeen));
        }

        this.currentServerIndex = tag.getInt("currentServerIndex").orElse(0);
        if (this.currentServerIndex < 0 || this.currentServerIndex > serverQueue.size()) {
            this.currentServerIndex = 0;
        }

        return super.fromTag(tag);
    }
}
