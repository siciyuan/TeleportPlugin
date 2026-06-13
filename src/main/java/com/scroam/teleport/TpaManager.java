package com.scroam.teleport;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TpaManager {

    private final TeleportPlugin plugin;
    private final Map<UUID, TpaRequest> pendingRequests;

    public TpaManager(TeleportPlugin plugin) {
        this.plugin = plugin;
        this.pendingRequests = new HashMap<>();
    }

    public void addRequest(UUID requesterId, UUID targetId, boolean isTpHere) {
        pendingRequests.put(targetId, new TpaRequest(requesterId, targetId, isTpHere));
    }

    public TpaRequest getRequest(UUID targetId) {
        return pendingRequests.get(targetId);
    }

    public void removeRequest(UUID targetId) {
        pendingRequests.remove(targetId);
    }

    public boolean hasPendingRequest(UUID targetId) {
        return pendingRequests.containsKey(targetId);
    }

    public static class TpaRequest {
        public final UUID requesterId;
        private final UUID targetId;
        public final boolean isTpHere;
        private final long timestamp;

        public TpaRequest(UUID requesterId, UUID targetId, boolean isTpHere) {
            this.requesterId = requesterId;
            this.targetId = targetId;
            this.isTpHere = isTpHere;
            this.timestamp = System.currentTimeMillis();
        }

        public UUID getRequesterId() {
            return requesterId;
        }

        public UUID getTargetId() {
            return targetId;
        }

        public boolean isTpHere() {
            return isTpHere;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }
}