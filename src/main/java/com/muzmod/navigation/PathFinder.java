package com.muzmod.navigation;

import com.muzmod.MuzMod;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFence;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.BlockWall;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;

import java.util.*;

/**
 * Basit A* Pathfinding
 * 
 * Minecraft dünyasında yol bulma algoritması.
 * Engelleri aşmaya çalışır, yoksa direkt yol verir.
 */
public class PathFinder {
    
    private final Minecraft mc = Minecraft.getMinecraft();
    
    // Pathfinding limitleri
    private static final int MAX_ITERATIONS = 1000;
    private static final int MAX_PATH_LENGTH = 200;
    private static final int SEARCH_RADIUS = 64;
    
    // Yürünebilir olmayan bloklar
    private static final Set<Block> BLOCKED_BLOCKS = new HashSet<>(Arrays.asList(
        Blocks.lava,
        Blocks.flowing_lava,
        Blocks.fire,
        Blocks.cactus,
        Blocks.web
    ));
    
    /**
     * İki nokta arasında yol bul (A* algoritması)
     */
    public List<BlockPos> findPath(BlockPos start, BlockPos end) {
        if (mc.theWorld == null) return null;
        
        // Çok uzaksa null dön
        double distance = getDistance(start, end);
        if (distance > SEARCH_RADIUS) {
            // Uzak hedef için ara noktalar oluştur
            return getSimplePath(start, end);
        }
        
        // Direkt yol açık mı kontrol et
        if (isDirectPathClear(start, end)) {
            List<BlockPos> directPath = new ArrayList<>();
            directPath.add(start);
            directPath.add(end);
            return directPath;
        }
        
        // A* algoritması
        PriorityQueue<Node> openSet = new PriorityQueue<>(Comparator.comparingDouble(n -> n.fCost));
        Map<BlockPos, Node> allNodes = new HashMap<>();
        Set<BlockPos> closedSet = new HashSet<>();
        
        Node startNode = new Node(start, null, 0, heuristic(start, end));
        openSet.add(startNode);
        allNodes.put(start, startNode);
        
        int iterations = 0;
        
        while (!openSet.isEmpty() && iterations < MAX_ITERATIONS) {
            iterations++;
            
            Node current = openSet.poll();
            
            // Hedefe ulaştık mı?
            if (getDistance(current.pos, end) < 2) {
                return reconstructPath(current);
            }
            
            closedSet.add(current.pos);
            
            // Komşuları kontrol et
            for (BlockPos neighbor : getNeighbors(current.pos)) {
                if (closedSet.contains(neighbor)) continue;
                
                // Maliyet: mesafe + etraftaki blok yoğunluğu (açık alan tercih edilir)
                double moveCost = getDistance(current.pos, neighbor);
                double obstaclePenalty = calculateObstaclePenalty(neighbor);
                double tentativeG = current.gCost + moveCost + obstaclePenalty;
                
                Node neighborNode = allNodes.get(neighbor);
                
                if (neighborNode == null) {
                    neighborNode = new Node(neighbor, current, tentativeG, heuristic(neighbor, end));
                    allNodes.put(neighbor, neighborNode);
                    openSet.add(neighborNode);
                } else if (tentativeG < neighborNode.gCost) {
                    openSet.remove(neighborNode);
                    neighborNode.parent = current;
                    neighborNode.gCost = tentativeG;
                    neighborNode.fCost = tentativeG + heuristic(neighbor, end);
                    openSet.add(neighborNode);
                }
            }
        }
        
        // Yol bulunamadı, basit yol dön
        MuzMod.LOGGER.warn("[PathFinder] A* failed after " + iterations + " iterations, using simple path");
        return getSimplePath(start, end);
    }
    
    /**
     * Basit düz çizgi yolu (waypoint'lerle)
     */
    public List<BlockPos> getSimplePath(BlockPos start, BlockPos end) {
        List<BlockPos> path = new ArrayList<>();
        path.add(start);
        
        double totalDistance = getDistance(start, end);
        int numWaypoints = (int) Math.ceil(totalDistance / 10.0); // Her 10 blokta bir waypoint
        
        if (numWaypoints <= 1) {
            path.add(end);
            return path;
        }
        
        double dx = (end.getX() - start.getX()) / (double) numWaypoints;
        double dy = (end.getY() - start.getY()) / (double) numWaypoints;
        double dz = (end.getZ() - start.getZ()) / (double) numWaypoints;
        
        for (int i = 1; i < numWaypoints; i++) {
            int x = start.getX() + (int) (dx * i);
            int y = start.getY() + (int) (dy * i);
            int z = start.getZ() + (int) (dz * i);
            
            // Y koordinatını zemine ayarla
            BlockPos waypoint = findGround(new BlockPos(x, y, z));
            if (waypoint != null) {
                path.add(waypoint);
            }
        }
        
        path.add(end);
        return path;
    }
    
    /**
     * Direkt yol açık mı kontrol et (engel var mı)
     * Sadece yolun kendisi değil, yolun 1 blok sağı ve solu da kontrol edilir
     */
    public boolean isDirectPathClear(BlockPos start, BlockPos end) {
        int steps = (int) Math.ceil(getDistance(start, end));
        if (steps == 0) return true;
        
        double dx = (end.getX() - start.getX()) / (double) steps;
        double dy = (end.getY() - start.getY()) / (double) steps;
        double dz = (end.getZ() - start.getZ()) / (double) steps;
        
        // Yol yönüne dik vektör (yan kontrol için)
        double perpX = -dz;
        double perpZ = dx;
        double perpLen = Math.sqrt(perpX * perpX + perpZ * perpZ);
        if (perpLen > 0) {
            perpX /= perpLen;
            perpZ /= perpLen;
        }
        
        for (int i = 0; i <= steps; i++) {
            int x = start.getX() + (int) (dx * i);
            int y = start.getY() + (int) (dy * i);
            int z = start.getZ() + (int) (dz * i);
            
            BlockPos checkPos = new BlockPos(x, y, z);
            
            // Merkez yol kontrolü
            if (!isWalkable(checkPos)) {
                return false;
            }
            
            // Yan taraf kontrolü (oyuncu genişliği için)
            // Sağ taraf
            BlockPos rightPos = new BlockPos(x + (int)Math.round(perpX), y, z + (int)Math.round(perpZ));
            if (isBlockingPath(rightPos)) {
                return false;
            }
            
            // Sol taraf
            BlockPos leftPos = new BlockPos(x - (int)Math.round(perpX), y, z - (int)Math.round(perpZ));
            if (isBlockingPath(leftPos)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Bir blok yolu engelliyor mu?
     */
    private boolean isBlockingPath(BlockPos pos) {
        if (mc.theWorld == null) return false;
        
        Block blockAtFeet = mc.theWorld.getBlockState(pos).getBlock();
        Block blockAtHead = mc.theWorld.getBlockState(pos.up()).getBlock();
        
        // Çit ve duvarlar kesinlikle engeller
        if (isFenceBlock(blockAtFeet) || isFenceBlock(blockAtHead)) {
            return true;
        }
        
        // Katı bloklar da engeller (collision box kontrolü)
        if (!mc.theWorld.isAirBlock(pos) && !blockAtFeet.isPassable(mc.theWorld, pos)) {
            return true;
        }
        if (!mc.theWorld.isAirBlock(pos.up()) && !blockAtHead.isPassable(mc.theWorld, pos.up())) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Bir pozisyona yürünebilir mi?
     */
    public boolean isWalkable(BlockPos pos) {
        if (mc.theWorld == null) return false;
        
        Block blockAtFeet = mc.theWorld.getBlockState(pos).getBlock();
        Block blockAtHead = mc.theWorld.getBlockState(pos.up()).getBlock();
        Block blockBelow = mc.theWorld.getBlockState(pos.down()).getBlock();
        
        // Çit kontrolü - çitler 1.5 blok yüksekliğinde, geçilemez
        if (isFenceBlock(blockAtFeet) || isFenceBlock(blockBelow)) {
            return false;
        }
        
        // Ayak ve baş seviyesi boş olmalı (çit hariç - zaten kontrol edildi)
        boolean feetClear = isBlockPassable(blockAtFeet, pos);
        boolean headClear = isBlockPassable(blockAtHead, pos.up());
        
        // Ayağın altında zemin olmalı
        boolean hasGround = !mc.theWorld.isAirBlock(pos.down()) && 
                           !blockBelow.isPassable(mc.theWorld, pos.down());
        
        // Tehlikeli blok kontrolü
        boolean notDangerous = !BLOCKED_BLOCKS.contains(blockAtFeet) && 
                               !BLOCKED_BLOCKS.contains(blockBelow);
        
        return feetClear && headClear && notDangerous;
    }
    
    /**
     * Çit veya duvar bloğu mu? (collision box 1.5 blok)
     */
    private boolean isFenceBlock(Block block) {
        return block instanceof BlockFence || 
               block instanceof BlockFenceGate || 
               block instanceof BlockWall;
    }
    
    /**
     * Blok geçilebilir mi? (Çit ve özel blokları da kontrol et)
     */
    private boolean isBlockPassable(Block block, BlockPos pos) {
        // Çitler geçilemez (collision box 1.5 blok)
        if (isFenceBlock(block)) {
            return false;
        }
        
        // Hava her zaman geçilebilir
        if (mc.theWorld.isAirBlock(pos)) {
            return true;
        }
        
        // Diğer bloklar için isPassable kullan
        return block.isPassable(mc.theWorld, pos);
    }
    
    /**
     * Çapraz hareket için dar geçit kontrolü
     * İki çapraz blok arasından geçerken, her iki tarafın da açık olması gerekir
     */
    private boolean canMoveDiagonally(BlockPos from, BlockPos to) {
        int dx = to.getX() - from.getX();
        int dz = to.getZ() - from.getZ();
        
        // Çapraz hareket değilse sorun yok
        if (dx == 0 || dz == 0) {
            return true;
        }
        
        // İki yan bloğu da kontrol et (köşeden geçiş)
        BlockPos side1 = from.add(dx, 0, 0);
        BlockPos side2 = from.add(0, 0, dz);
        
        // Her iki taraf da açık olmalı (ayak + baş seviyesi)
        boolean side1Clear = isBlockPassable(mc.theWorld.getBlockState(side1).getBlock(), side1) &&
                             isBlockPassable(mc.theWorld.getBlockState(side1.up()).getBlock(), side1.up());
        boolean side2Clear = isBlockPassable(mc.theWorld.getBlockState(side2).getBlock(), side2) &&
                             isBlockPassable(mc.theWorld.getBlockState(side2.up()).getBlock(), side2.up());
        
        return side1Clear && side2Clear;
    }
    
    /**
     * Zemin bul (havadaysa aşağı in)
     */
    private BlockPos findGround(BlockPos pos) {
        if (mc.theWorld == null) return pos;
        
        // Yukarı veya aşağı zemin ara
        for (int dy = 0; dy <= 10; dy++) {
            BlockPos checkDown = pos.down(dy);
            BlockPos checkUp = pos.up(dy);
            
            if (isWalkable(checkDown)) {
                return checkDown;
            }
            if (isWalkable(checkUp)) {
                return checkUp;
            }
        }
        
        return pos;
    }
    
    /**
     * Komşu blokları al (8 yön + yukarı/aşağı)
     */
    private List<BlockPos> getNeighbors(BlockPos pos) {
        List<BlockPos> neighbors = new ArrayList<>();
        
        // 4 ana yön (öncelikli - daha güvenli)
        int[][] cardinalDirs = {
            {1, 0}, {-1, 0}, {0, 1}, {0, -1}
        };
        
        // 4 çapraz yön
        int[][] diagonalDirs = {
            {1, 1}, {1, -1}, {-1, 1}, {-1, -1}
        };
        
        // Önce ana yönleri ekle
        for (int[] dir : cardinalDirs) {
            addNeighborIfValid(neighbors, pos, dir[0], dir[1]);
        }
        
        // Sonra çapraz yönleri kontrol et (dar geçit kontrolü ile)
        for (int[] dir : diagonalDirs) {
            BlockPos neighbor = pos.add(dir[0], 0, dir[1]);
            // Çapraz hareket için dar geçit kontrolü
            if (isWalkable(neighbor) && canMoveDiagonally(pos, neighbor)) {
                neighbors.add(neighbor);
            }
            
            // Bir yukarı çapraz
            BlockPos neighborUp = pos.add(dir[0], 1, dir[1]);
            if (isWalkable(neighborUp) && canClimbTo(pos, neighborUp) && canMoveDiagonally(pos, neighborUp)) {
                neighbors.add(neighborUp);
            }
            
            // Bir aşağı çapraz
            BlockPos neighborDown = pos.add(dir[0], -1, dir[1]);
            if (isWalkable(neighborDown) && canMoveDiagonally(pos, neighborDown)) {
                neighbors.add(neighborDown);
            }
        }
        
        return neighbors;
    }
    
    /**
     * Ana yönler için komşu ekle (yukarı/aşağı dahil)
     */
    private void addNeighborIfValid(List<BlockPos> neighbors, BlockPos pos, int dx, int dz) {
        // Aynı seviye
        BlockPos neighbor = pos.add(dx, 0, dz);
        if (isWalkable(neighbor)) {
            neighbors.add(neighbor);
        }
        
        // Bir yukarı (merdiven/zıplama)
        BlockPos neighborUp = pos.add(dx, 1, dz);
        if (isWalkable(neighborUp) && canClimbTo(pos, neighborUp)) {
            neighbors.add(neighborUp);
        }
        
        // Bir aşağı (düşme)
        BlockPos neighborDown = pos.add(dx, -1, dz);
        if (isWalkable(neighborDown)) {
            neighbors.add(neighborDown);
        }
    }
    
    /**
     * Yukarı tırmanılabilir mi?
     */
    private boolean canClimbTo(BlockPos from, BlockPos to) {
        // Bir blok yukarı zıplayabilir miyiz?
        int heightDiff = to.getY() - from.getY();
        if (heightDiff > 1) return false;
        
        // Baş üstü boş mu?
        return mc.theWorld.isAirBlock(from.up().up());
    }
    
    /**
     * Yolu reconstruct et (A* sonucu)
     */
    private List<BlockPos> reconstructPath(Node endNode) {
        List<BlockPos> path = new ArrayList<>();
        Node current = endNode;
        
        while (current != null) {
            path.add(0, current.pos);
            current = current.parent;
        }
        
        // Yolu sadeleştir (gereksiz waypoint'leri kaldır)
        return simplifyPath(path);
    }
    
    /**
     * Yolu sadeleştir - düz çizgideki ara noktaları kaldır
     */
    private List<BlockPos> simplifyPath(List<BlockPos> path) {
        if (path.size() <= 2) return path;
        
        List<BlockPos> simplified = new ArrayList<>();
        simplified.add(path.get(0));
        
        int i = 0;
        while (i < path.size() - 1) {
            // En uzak görünür noktayı bul
            int farthest = i + 1;
            for (int j = i + 2; j < path.size(); j++) {
                if (isDirectPathClear(path.get(i), path.get(j))) {
                    farthest = j;
                }
            }
            
            simplified.add(path.get(farthest));
            i = farthest;
        }
        
        return simplified;
    }
    
    /**
     * İki nokta arası mesafe
     */
    private double getDistance(BlockPos a, BlockPos b) {
        double dx = a.getX() - b.getX();
        double dy = a.getY() - b.getY();
        double dz = a.getZ() - b.getZ();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
    
    /**
     * Bir pozisyonun etrafındaki engel yoğunluğunu hesapla
     * Ne kadar çok engel varsa, o kadar yüksek ceza
     * Açık alanlar tercih edilir
     */
    private double calculateObstaclePenalty(BlockPos pos) {
        if (mc.theWorld == null) return 0;
        
        double penalty = 0;
        int checkRadius = 2; // 2 blok yarıçapında kontrol
        
        for (int dx = -checkRadius; dx <= checkRadius; dx++) {
            for (int dz = -checkRadius; dz <= checkRadius; dz++) {
                if (dx == 0 && dz == 0) continue;
                
                BlockPos checkPos = pos.add(dx, 0, dz);
                Block blockAtFeet = mc.theWorld.getBlockState(checkPos).getBlock();
                Block blockAtHead = mc.theWorld.getBlockState(checkPos.up()).getBlock();
                
                // Çit veya duvar varsa yüksek ceza
                if (isFenceBlock(blockAtFeet) || isFenceBlock(blockAtHead)) {
                    double distance = Math.sqrt(dx * dx + dz * dz);
                    penalty += 5.0 / distance; // Yakındaki çitler daha çok ceza
                }
                // Katı blok varsa orta ceza
                else if (!blockAtFeet.isPassable(mc.theWorld, checkPos) || 
                         !blockAtHead.isPassable(mc.theWorld, checkPos.up())) {
                    double distance = Math.sqrt(dx * dx + dz * dz);
                    penalty += 2.0 / distance;
                }
            }
        }
        
        // Ayak seviyesinde ve baş seviyesinde engel kontrolü (yürüme yolu)
        // Dar geçitlerde yüksek ceza
        int clearSides = 0;
        BlockPos[] cardinals = {pos.north(), pos.south(), pos.east(), pos.west()};
        for (BlockPos cardinal : cardinals) {
            if (isWalkable(cardinal)) {
                clearSides++;
            }
        }
        
        // 4 tarafından 2'den az açıksa dar geçit cezası
        if (clearSides < 2) {
            penalty += 10.0;
        } else if (clearSides < 3) {
            penalty += 3.0;
        }
        
        return penalty;
    }
    
    /**
     * Heuristic fonksiyonu (A*)
     */
    private double heuristic(BlockPos a, BlockPos b) {
        // Euclidean distance
        return getDistance(a, b);
    }
    
    /**
     * A* Node sınıfı
     */
    private static class Node {
        final BlockPos pos;
        Node parent;
        double gCost; // Start'tan bu noktaya maliyet
        double fCost; // gCost + heuristic
        
        Node(BlockPos pos, Node parent, double gCost, double hCost) {
            this.pos = pos;
            this.parent = parent;
            this.gCost = gCost;
            this.fCost = gCost + hCost;
        }
    }
}
