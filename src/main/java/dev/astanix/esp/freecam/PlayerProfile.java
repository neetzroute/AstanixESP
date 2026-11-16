package dev.astanix.esp.freecam;

import java.util.UUID;

/**
 * Tracks player state and administrative bypass status for FreeCam protection.
 */
public class PlayerProfile {
    private final UUID playerId;
    private boolean bypass = false;

    public PlayerProfile(UUID playerId) {
        this.playerId = playerId;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public boolean isBypass() {
        return bypass;
    }

    public void setBypass(boolean bypass) {
        this.bypass = bypass;
    }
}
