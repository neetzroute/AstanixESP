package dev.astanix.esp.model;

import org.bukkit.Material;
import org.bukkit.block.data.BlockData;

import java.util.Objects;

/**
 * Represents a cached storage block with its real state and disguised state.
 * Supports atomic pairing for double chests to eliminate texture desyncs.
 */
public class StorageBlock {
    private final BlockPos pos;
    private final Material originalMaterial;
    private final BlockData originalBlockData;
    private volatile BlockData disguiseBlockData;
    private StorageBlock pairedBlock;

    public StorageBlock(BlockPos pos, Material originalMaterial, BlockData originalBlockData, BlockData disguiseBlockData) {
        this.pos = Objects.requireNonNull(pos, "pos cannot be null");
        this.originalMaterial = Objects.requireNonNull(originalMaterial, "originalMaterial cannot be null");
        this.originalBlockData = Objects.requireNonNull(originalBlockData, "originalBlockData cannot be null");
        this.disguiseBlockData = Objects.requireNonNull(disguiseBlockData, "disguiseBlockData cannot be null");
    }

    public BlockPos getPos() {
        return pos;
    }

    public Material getOriginalMaterial() {
        return originalMaterial;
    }

    public BlockData getOriginalBlockData() {
        return originalBlockData;
    }

    public BlockData getDisguiseBlockData() {
        return disguiseBlockData;
    }

    public void setDisguiseBlockData(BlockData disguiseBlockData) {
        this.disguiseBlockData = Objects.requireNonNull(disguiseBlockData, "disguiseBlockData cannot be null");
    }

    public boolean isDoubleChest() {
        return pairedBlock != null;
    }

    public StorageBlock getPairedBlock() {
        return pairedBlock;
    }

    public void setPairedBlock(StorageBlock pairedBlock) {
        this.pairedBlock = pairedBlock;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StorageBlock that = (StorageBlock) o;
        return pos.equals(that.pos);
    }

    @Override
    public int hashCode() {
        return pos.hashCode();
    }

    @Override
    public String toString() {
        return "StorageBlock{" +
                "pos=" + pos +
                ", original=" + originalMaterial +
                ", disguise=" + disguiseBlockData.getMaterial() +
                ", isDoubleChest=" + isDoubleChest() +
                '}';
    }
}
