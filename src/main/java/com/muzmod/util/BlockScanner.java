package com.muzmod.util;

import net.minecraft.block.Block;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;

import java.util.HashSet;
import java.util.Set;

/**
 * Utility class for scanning and finding blocks in the world
 */
public class BlockScanner {
    
    /**
     * Find all blocks of a specific type within radius
     */
    public static Set<BlockPos> findBlocks(World world, BlockPos center, int radius, Block targetBlock) {
        Set<BlockPos> found = new HashSet<>();
        
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = center.add(x, y, z);
                    Block block = world.getBlockState(pos).getBlock();
                    
                    if (block == targetBlock) {
                        found.add(pos);
                    }
                }
            }
        }
        
        return found;
    }
    
    /**
     * Find all blocks of specific types within radius
     */
    public static Set<BlockPos> findBlocks(World world, BlockPos center, int radius, Block... targetBlocks) {
        Set<BlockPos> found = new HashSet<>();
        Set<Block> targets = new HashSet<>();
        for (Block b : targetBlocks) {
            targets.add(b);
        }
        
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = center.add(x, y, z);
                    Block block = world.getBlockState(pos).getBlock();
                    
                    if (targets.contains(block)) {
                        found.add(pos);
                    }
                }
            }
        }
        
        return found;
    }
    
    /**
     * Find the closest block of a type
     */
    public static BlockPos findClosestBlock(World world, BlockPos center, int radius, Block targetBlock) {
        BlockPos closest = null;
        double closestDist = Double.MAX_VALUE;
        
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = center.add(x, y, z);
                    Block block = world.getBlockState(pos).getBlock();
                    
                    if (block == targetBlock) {
                        double dist = center.distanceSq(pos);
                        if (dist < closestDist) {
                            closestDist = dist;
                            closest = pos;
                        }
                    }
                }
            }
        }
        
        return closest;
    }
    
    /**
     * Check if a position has a specific block
     */
    public static boolean isBlockAt(World world, BlockPos pos, Block targetBlock) {
        return world.getBlockState(pos).getBlock() == targetBlock;
    }
    
    /**
     * Get blocks below player within horizontal radius
     */
    public static Set<BlockPos> getBlocksBelow(World world, BlockPos center, int horizontalRadius, int depth) {
        Set<BlockPos> found = new HashSet<>();
        
        for (int x = -horizontalRadius; x <= horizontalRadius; x++) {
            for (int z = -horizontalRadius; z <= horizontalRadius; z++) {
                for (int y = -1; y >= -depth; y--) {
                    BlockPos pos = center.add(x, y, z);
                    Block block = world.getBlockState(pos).getBlock();
                    
                    if (!block.getMaterial().isReplaceable()) {
                        found.add(pos);
                    }
                }
            }
        }
        
        return found;
    }
}
