package dev.astanix.esp.model;

import org.bukkit.Location;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks the anti-ESP perception state for an online player.
 */
public class PlayerState {
    private final UUID playerId;
    private final Set<BlockPos> revealedBlocks = ConcurrentHashMap.newKeySet();
    private final Set<UUID> revealedEntities = ConcurrentHashMap.newKeySet();
    private Location lastCheckLocation;
    private boolean bypass;

    public PlayerState(UUID playerId) {
        this.playerId = playerId;
        this.bypass = false;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public Set<BlockPos> getRevealedBlocks() {
        return revealedBlocks;
    }

    public Set<UUID> getRevealedEntities() {
        return revealedEntities;
    }

    public boolean isBlockRevealed(BlockPos pos) {
        return revealedBlocks.contains(pos);
    }

    public boolean markBlockRevealed(BlockPos pos) {
        return revealedBlocks.add(pos);
    }

    public boolean markBlockHidden(BlockPos pos) {
        return revealedBlocks.remove(pos);
    }

    public boolean isEntityRevealed(UUID entityId) {
        return revealedEntities.contains(entityId);
    }

    public boolean markEntityRevealed(UUID entityId) {
        return revealedEntities.add(entityId);
    }

    public boolean markEntityHidden(UUID entityId) {
        return revealedEntities.remove(entityId);
    }

    public Location getLastCheckLocation() {
        return lastCheckLocation;
    }

    public void setLastCheckLocation(Location lastCheckLocation) {
        this.lastCheckLocation = lastCheckLocation;
    }

    public boolean isBypass() {
        return bypass;
    }

    public void setBypass(boolean bypass) {
        this.bypass = bypass;
    }

    public void clear() {
        revealedBlocks.clear();
        revealedEntities.clear();
        lastCheckLocation = null;
    }
}
