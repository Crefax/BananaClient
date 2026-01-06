package com.muzmod.util;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;

import java.util.*;

/**
 * Simple pathfinding utility for navigating to ore locations
 */
public class PathFinder {
    
    private static final Minecraft mc = Minecraft.getMinecraft();
    
    /**
     * Find a path from start to goal using A* algorithm
     * @return List of positions to follow, or empty list if no path found
     */
    public static List<BlockPos> findPath(World world, BlockPos start, BlockPos goal, int maxIterations) {
        if (start == null || goal == null) return Collections.emptyList();
        
        Set<BlockPos> closedSet = new HashSet<>();
        PriorityQueue<PathNode> openSet = new PriorityQueue<>(Comparator.comparingDouble(n -> n.fScore));
        Map<BlockPos, BlockPos> cameFrom = new HashMap<>();
        Map<BlockPos, Double> gScore = new HashMap<>();
        
        gScore.put(start, 0.0);
        openSet.add(new PathNode(start, heuristic(start, goal)));
        
        int iterations = 0;
        
        while (!openSet.isEmpty() && iterations < maxIterations) {
            iterations++;
            
            PathNode current = openSet.poll();
            BlockPos currentPos = current.pos;
            
            if (currentPos.distanceSq(goal) < 4) {
                return reconstructPath(cameFrom, currentPos);
            }
            
            closedSet.add(currentPos);
            
            for (BlockPos neighbor : getNeighbors(world, currentPos)) {
                if (closedSet.contains(neighbor)) continue;
                
                double tentativeGScore = gScore.getOrDefault(currentPos, Double.MAX_VALUE) + 1;
                
                if (tentativeGScore < gScore.getOrDefault(neighbor, Double.MAX_VALUE)) {
                    cameFrom.put(neighbor, currentPos);
                    gScore.put(neighbor, tentativeGScore);
                    
                    double fScore = tentativeGScore + heuristic(neighbor, goal);
                    openSet.add(new PathNode(neighbor, fScore));
                }
            }
        }
        
        return Collections.emptyList();
    }
    
    private static double heuristic(BlockPos a, BlockPos b) {
        return Math.sqrt(a.distanceSq(b));
    }
    
    private static List<BlockPos> reconstructPath(Map<BlockPos, BlockPos> cameFrom, BlockPos current) {
        List<BlockPos> path = new ArrayList<>();
        path.add(current);
        
        while (cameFrom.containsKey(current)) {
            current = cameFrom.get(current);
            path.add(0, current);
        }
        
        return path;
    }
    
    private static List<BlockPos> getNeighbors(World world, BlockPos pos) {
        List<BlockPos> neighbors = new ArrayList<>();
        
        // Cardinal directions
        BlockPos[] cardinals = {
            pos.north(), pos.south(), pos.east(), pos.west()
        };
        
        for (BlockPos cardinal : cardinals) {
            // Check if walkable
            if (isWalkable(world, cardinal)) {
                neighbors.add(cardinal);
            }
            
            // Check if we can jump up
            BlockPos up = cardinal.up();
            if (isWalkable(world, up) && canStandAt(world, pos)) {
                neighbors.add(up);
            }
            
            // Check if we can drop down
            BlockPos down = cardinal.down();
            if (isWalkable(world, down)) {
                neighbors.add(down);
            }
        }
        
        return neighbors;
    }
    
    /**
     * Check if a position is walkable (can stand there)
     */
    public static boolean isWalkable(World world, BlockPos pos) {
        Block blockAtFeet = world.getBlockState(pos).getBlock();
        Block blockAtHead = world.getBlockState(pos.up()).getBlock();
        Block blockBelow = world.getBlockState(pos.down()).getBlock();
        
        // Feet and head must be passable, and there must be solid ground below
        return blockAtFeet.getMaterial().isReplaceable() &&
               blockAtHead.getMaterial().isReplaceable() &&
               blockBelow.getMaterial().isSolid();
    }
    
    /**
     * Check if entity can stand at position
     */
    public static boolean canStandAt(World world, BlockPos pos) {
        Block blockBelow = world.getBlockState(pos.down()).getBlock();
        return blockBelow.getMaterial().isSolid();
    }
    
    /**
     * Find safe spot away from a position
     */
    public static BlockPos findSafeSpot(World world, BlockPos playerPos, BlockPos avoidPos, int minDistance, int maxDistance) {
        Random random = new Random();
        
        // Calculate direction away from avoid position
        double dx = playerPos.getX() - avoidPos.getX();
        double dz = playerPos.getZ() - avoidPos.getZ();
        double length = Math.sqrt(dx * dx + dz * dz);
        
        if (length > 0) {
            dx /= length;
            dz /= length;
        } else {
            dx = random.nextDouble() * 2 - 1;
            dz = random.nextDouble() * 2 - 1;
        }
        
        // Try to find a walkable position in that direction
        for (int distance = minDistance; distance <= maxDistance; distance += 2) {
            // Add some randomness
            double offsetX = dx * distance + (random.nextDouble() - 0.5) * 5;
            double offsetZ = dz * distance + (random.nextDouble() - 0.5) * 5;
            
            BlockPos candidate = new BlockPos(
                playerPos.getX() + offsetX,
                playerPos.getY(),
                playerPos.getZ() + offsetZ
            );
            
            // Find ground level
            candidate = findGroundLevel(world, candidate);
            
            if (candidate != null && isWalkable(world, candidate)) {
                return candidate;
            }
        }
        
        return null;
    }
    
    /**
     * Find ground level at position
     */
    public static BlockPos findGroundLevel(World world, BlockPos pos) {
        // Search up and down for ground
        for (int y = 0; y <= 5; y++) {
            BlockPos up = pos.up(y);
            if (isWalkable(world, up)) {
                return up;
            }
            
            BlockPos down = pos.down(y);
            if (isWalkable(world, down)) {
                return down;
            }
        }
        
        return null;
    }
    
    /**
     * Simple direction to move towards target
     */
    public static float getDirectionTo(BlockPos from, BlockPos to) {
        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        
        return (float) (Math.atan2(dz, dx) * 180 / Math.PI) - 90;
    }
    
    private static class PathNode {
        BlockPos pos;
        double fScore;
        
        PathNode(BlockPos pos, double fScore) {
            this.pos = pos;
            this.fScore = fScore;
        }
    }
}
