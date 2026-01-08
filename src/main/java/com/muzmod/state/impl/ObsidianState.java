package com.muzmod.state.impl;

import com.muzmod.MuzMod;
import com.muzmod.state.AbstractState;
import com.muzmod.util.InputSimulator;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

import java.util.Random;

/**
 * Obsidian Mining State v0.6.3
 * 
 * Basit ve etkili obsidyen kazma sistemi.
 * 
 * Çalışma mantığı:
 * - Bot Y=5'te durur (obsidyenlerin üstünde)
 * - Önündeki Y=4 seviyesindeki obsidyenleri kazarak ilerler
 * - Hedef: max obsidyen sayısı - (0-10 arası rastgele) mesafe
 * - Hedefe ulaşınca sağ/sol kontrol edip çok olan tarafa döner
 * - Y düşerse yukarı doğru kazarak çıkar
 */
public class ObsidianState extends AbstractState {
    
    private final Minecraft mc = Minecraft.getMinecraft();
    private final Random random = new Random();
    
    // Obsidyen Y=4'te, player Y=5'te duruyor
    private static final int OBSIDIAN_Y = 4;
    
    // Phase
    private Phase phase = Phase.INIT;
    
    // Hedefler
    private BlockPos redTarget = null;    // Mevcut hedef (KIRMIZI)
    private BlockPos yellowTarget = null; // Sonraki hedef (SARI)
    private BlockPos miningBlock = null;  // Şu an kazılan blok (YEŞİL)
    
    // Yön (4 yön: 0=Güney, 1=Batı, 2=Kuzey, 3=Doğu)
    private int direction = 0;
    private static final int[] DX = {0, -1, 0, 1};  // Güney, Batı, Kuzey, Doğu
    private static final int[] DZ = {1, 0, -1, 0};
    private static final float[] YAWS = {0, 90, 180, -90}; // Minecraft yaw değerleri
    
    // Durum
    private String debugInfo = "";
    private long miningStartTime = 0;
    private BlockPos lastMiningBlock = null;
    
    private enum Phase {
        INIT,
        CLIMB_UP,
        FIND_TARGET,
        MINING,
        TURNING,
        DONE
    }
    
    public ObsidianState() {
        this.status = "Obsidian ready";
    }
    
    @Override
    public void onEnable() {
        super.onEnable();
        phase = Phase.INIT;
        redTarget = null;
        yellowTarget = null;
        miningBlock = null;
        direction = 0;
        miningStartTime = 0;
        lastMiningBlock = null;
        MinecraftForge.EVENT_BUS.register(this);
        MuzMod.LOGGER.info("[Obsidian] v0.6.3 enabled");
    }
    
    @Override
    public void onDisable() {
        super.onDisable();
        InputSimulator.releaseAll();
        MinecraftForge.EVENT_BUS.unregister(this);
        redTarget = null;
        yellowTarget = null;
        miningBlock = null;
        MuzMod.LOGGER.info("[Obsidian] disabled");
    }
    
    @Override
    public void onTick() {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        
        int playerY = (int) Math.floor(mc.thePlayer.posY);
        
        // Y çok düşükse CLIMB_UP moduna geç
        if (playerY < OBSIDIAN_Y && phase != Phase.CLIMB_UP && phase != Phase.INIT) {
            MuzMod.LOGGER.info("[Obsidian] Y=" + playerY + " too low, climbing up");
            phase = Phase.CLIMB_UP;
        }
        
        switch (phase) {
            case INIT:
                doInit();
                break;
            case CLIMB_UP:
                doClimbUp();
                break;
            case FIND_TARGET:
                doFindTarget();
                break;
            case MINING:
                doMining();
                break;
            case TURNING:
                doTurning();
                break;
            case DONE:
                InputSimulator.releaseAll();
                setStatus("Done!");
                break;
        }
    }
    
    // ===================== PHASE HANDLERS =====================
    
    private void doInit() {
        debugInfo = "Initializing...";
        setStatus(debugInfo);
        
        int playerY = (int) Math.floor(mc.thePlayer.posY);
        
        // Y düşükse önce yukarı çık
        if (playerY < OBSIDIAN_Y) {
            phase = Phase.CLIMB_UP;
            return;
        }
        
        // Baktığı yöne göre direction belirle
        float yaw = mc.thePlayer.rotationYaw;
        direction = getDirectionFromYaw(yaw);
        
        // Yaw'ı tam 90'a sabitle
        mc.thePlayer.rotationYaw = YAWS[direction];
        
        MuzMod.LOGGER.info("[Obsidian] Init - direction=" + direction + " yaw=" + YAWS[direction]);
        
        phase = Phase.FIND_TARGET;
    }
    
    private void doClimbUp() {
        int playerY = (int) Math.floor(mc.thePlayer.posY);
        debugInfo = "Climbing Y=" + playerY;
        setStatus(debugInfo);
        
        // Yeterli yüksekliğe ulaştık mı?
        if (playerY >= OBSIDIAN_Y) {
            MuzMod.LOGGER.info("[Obsidian] Reached Y=" + playerY);
            InputSimulator.releaseAll();
            phase = Phase.FIND_TARGET;
            return;
        }
        
        // Yukarıda obsidyen var mı?
        BlockPos playerPos = mc.thePlayer.getPosition();
        BlockPos above = playerPos.up();
        BlockPos above2 = playerPos.up(2);
        
        Block blockAbove = mc.theWorld.getBlockState(above).getBlock();
        Block blockAbove2 = mc.theWorld.getBlockState(above2).getBlock();
        
        // Yukarıya bak
        mc.thePlayer.rotationPitch = -45;
        
        if (blockAbove == Blocks.obsidian) {
            miningBlock = above;
            mc.thePlayer.rotationPitch = -60;
            InputSimulator.holdKey(mc.gameSettings.keyBindAttack, true);
        } else if (blockAbove2 == Blocks.obsidian) {
            miningBlock = above2;
            mc.thePlayer.rotationPitch = -75;
            InputSimulator.holdKey(mc.gameSettings.keyBindAttack, true);
        } else {
            miningBlock = null;
            InputSimulator.releaseKey(mc.gameSettings.keyBindAttack);
        }
        
        // Zıpla ve ilerle
        InputSimulator.holdKey(mc.gameSettings.keyBindJump, true);
        InputSimulator.holdKey(mc.gameSettings.keyBindForward, true);
    }
    
    private void doFindTarget() {
        debugInfo = "Finding target...";
        setStatus(debugInfo);
        
        int playerX = (int) Math.floor(mc.thePlayer.posX);
        int playerZ = (int) Math.floor(mc.thePlayer.posZ);
        
        // İleri yönde kaç obsidyen var?
        int count = countObsidianForward(playerX, playerZ);
        MuzMod.LOGGER.info("[Obsidian] Forward count: " + count);
        
        if (count < 2) {
            // Sağ ve sol kontrol et
            int leftCount = countObsidianInDir(playerX, playerZ, turnLeft(direction));
            int rightCount = countObsidianInDir(playerX, playerZ, turnRight(direction));
            
            MuzMod.LOGGER.info("[Obsidian] Left: " + leftCount + ", Right: " + rightCount);
            
            if (leftCount >= 2 || rightCount >= 2) {
                // Daha çok obsidyen olan tarafa dön
                if (leftCount > rightCount) {
                    direction = turnLeft(direction);
                } else {
                    direction = turnRight(direction);
                }
                mc.thePlayer.rotationYaw = YAWS[direction];
                MuzMod.LOGGER.info("[Obsidian] Turned to direction=" + direction);
                return; // Tekrar kontrol et
            } else {
                MuzMod.LOGGER.info("[Obsidian] No obsidian found, done");
                phase = Phase.DONE;
                return;
            }
        }
        
        // Hedef mesafesi: max - (0-10 arası rastgele)
        int maxDist = count;
        int minDist = Math.max(2, maxDist - 10);
        int targetDist = minDist + random.nextInt(maxDist - minDist + 1);
        
        // Kırmızı hedef belirle
        redTarget = new BlockPos(
            playerX + DX[direction] * targetDist,
            OBSIDIAN_Y,
            playerZ + DZ[direction] * targetDist
        );
        
        MuzMod.LOGGER.info("[Obsidian] Red target: " + redTarget + " dist=" + targetDist);
        
        // Sarı hedef (sonraki dönüş için önizleme)
        calculateYellowTarget();
        
        phase = Phase.MINING;
    }
    
    private void doMining() {
        if (redTarget == null) {
            phase = Phase.FIND_TARGET;
            return;
        }
        
        int playerX = (int) Math.floor(mc.thePlayer.posX);
        int playerZ = (int) Math.floor(mc.thePlayer.posZ);
        
        // Hedefe mesafe
        double dist = Math.sqrt(
            Math.pow(playerX - redTarget.getX(), 2) +
            Math.pow(playerZ - redTarget.getZ(), 2)
        );
        
        debugInfo = String.format("Mining dist=%.1f", dist);
        setStatus(debugInfo);
        
        // Hedefe ulaştık mı?
        if (dist < 1.5) {
            MuzMod.LOGGER.info("[Obsidian] Reached target!");
            InputSimulator.releaseAll();
            redTarget = yellowTarget;
            yellowTarget = null;
            phase = Phase.TURNING;
            return;
        }
        
        // Yön sabit kalsın
        mc.thePlayer.rotationYaw = YAWS[direction];
        
        // Önde obsidyen bul
        BlockPos toMine = findObsidianToMine(playerX, playerZ);
        
        if (toMine != null) {
            miningBlock = toMine;
            
            // Bloğa bak (sadece pitch değiştir, yaw sabit)
            double dy = toMine.getY() + 0.5 - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
            double dx = toMine.getX() + 0.5 - mc.thePlayer.posX;
            double dz = toMine.getZ() + 0.5 - mc.thePlayer.posZ;
            double horizontalDist = Math.sqrt(dx * dx + dz * dz);
            
            float targetPitch = (float) Math.toDegrees(Math.atan2(-dy, horizontalDist));
            targetPitch = Math.max(-90, Math.min(90, targetPitch));
            
            // Pitch'i yumuşak ayarla
            float pitchDiff = targetPitch - mc.thePlayer.rotationPitch;
            mc.thePlayer.rotationPitch += pitchDiff * 0.3f;
            
            // Kaz
            InputSimulator.holdKey(mc.gameSettings.keyBindAttack, true);
            
            // Stuck detection
            if (lastMiningBlock != null && lastMiningBlock.equals(toMine)) {
                if (System.currentTimeMillis() - miningStartTime > 5000) {
                    // 5 saniye aynı bloğu kazıyorsa ileri atla
                    MuzMod.LOGGER.info("[Obsidian] Stuck on block, moving forward");
                    InputSimulator.releaseKey(mc.gameSettings.keyBindAttack);
                    InputSimulator.holdKey(mc.gameSettings.keyBindForward, true);
                    InputSimulator.holdKey(mc.gameSettings.keyBindJump, true);
                    try { Thread.sleep(200); } catch (Exception ignored) {}
                    InputSimulator.releaseKey(mc.gameSettings.keyBindJump);
                    miningStartTime = System.currentTimeMillis();
                }
            } else {
                lastMiningBlock = toMine;
                miningStartTime = System.currentTimeMillis();
            }
        } else {
            miningBlock = null;
            InputSimulator.releaseKey(mc.gameSettings.keyBindAttack);
            
            // Düz ileri bak
            mc.thePlayer.rotationPitch = 30; // Hafif aşağı
        }
        
        // Her zaman ileri git
        InputSimulator.holdKey(mc.gameSettings.keyBindForward, true);
    }
    
    private void doTurning() {
        debugInfo = "Turning...";
        setStatus(debugInfo);
        
        InputSimulator.releaseAll();
        
        int playerX = (int) Math.floor(mc.thePlayer.posX);
        int playerZ = (int) Math.floor(mc.thePlayer.posZ);
        
        // Sağ ve sol kontrol et
        int leftDir = turnLeft(direction);
        int rightDir = turnRight(direction);
        
        int leftCount = countObsidianInDir(playerX, playerZ, leftDir);
        int rightCount = countObsidianInDir(playerX, playerZ, rightDir);
        
        MuzMod.LOGGER.info("[Obsidian] Turn - Left: " + leftCount + ", Right: " + rightCount);
        
        if (leftCount < 2 && rightCount < 2) {
            // Arkaya bak
            int backDir = (direction + 2) % 4;
            int backCount = countObsidianInDir(playerX, playerZ, backDir);
            
            if (backCount >= 2) {
                direction = backDir;
                MuzMod.LOGGER.info("[Obsidian] Turning back");
            } else {
                MuzMod.LOGGER.info("[Obsidian] No obsidian anywhere, done");
                phase = Phase.DONE;
                return;
            }
        } else if (leftCount > rightCount) {
            direction = leftDir;
            MuzMod.LOGGER.info("[Obsidian] Turning left");
        } else {
            direction = rightDir;
            MuzMod.LOGGER.info("[Obsidian] Turning right");
        }
        
        mc.thePlayer.rotationYaw = YAWS[direction];
        phase = Phase.FIND_TARGET;
    }
    
    // ===================== HELPER METHODS =====================
    
    private int getDirectionFromYaw(float yaw) {
        // Yaw'ı 0-360 arasına normalize et
        while (yaw < 0) yaw += 360;
        while (yaw >= 360) yaw -= 360;
        
        // 4 yöne yuvarla
        if (yaw >= 315 || yaw < 45) return 0;   // Güney (0)
        if (yaw >= 45 && yaw < 135) return 1;   // Batı (90)
        if (yaw >= 135 && yaw < 225) return 2;  // Kuzey (180)
        return 3;  // Doğu (270/-90)
    }
    
    private int turnLeft(int dir) {
        return (dir + 3) % 4;  // Sola dön
    }
    
    private int turnRight(int dir) {
        return (dir + 1) % 4;  // Sağa dön
    }
    
    private int countObsidianForward(int startX, int startZ) {
        return countObsidianInDir(startX, startZ, direction);
    }
    
    private int countObsidianInDir(int startX, int startZ, int dir) {
        int count = 0;
        for (int i = 1; i <= 100; i++) {
            int x = startX + DX[dir] * i;
            int z = startZ + DZ[dir] * i;
            BlockPos pos = new BlockPos(x, OBSIDIAN_Y, z);
            if (mc.theWorld.getBlockState(pos).getBlock() == Blocks.obsidian) {
                count++;
            }
        }
        return count;
    }
    
    private BlockPos findObsidianToMine(int playerX, int playerZ) {
        // Önce direkt önündeki bloklara bak (Y=4)
        for (int dist = 0; dist <= 3; dist++) {
            int x = playerX + DX[direction] * dist;
            int z = playerZ + DZ[direction] * dist;
            BlockPos pos = new BlockPos(x, OBSIDIAN_Y, z);
            if (mc.theWorld.getBlockState(pos).getBlock() == Blocks.obsidian) {
                return pos;
            }
        }
        return null;
    }
    
    private void calculateYellowTarget() {
        if (redTarget == null) {
            yellowTarget = null;
            return;
        }
        
        int leftDir = turnLeft(direction);
        int rightDir = turnRight(direction);
        
        int leftCount = countObsidianInDir(redTarget.getX(), redTarget.getZ(), leftDir);
        int rightCount = countObsidianInDir(redTarget.getX(), redTarget.getZ(), rightDir);
        
        int nextDir = leftCount > rightCount ? leftDir : rightDir;
        int nextCount = Math.max(leftCount, rightCount);
        
        if (nextCount >= 2) {
            int maxDist = nextCount;
            int minDist = Math.max(2, maxDist - 10);
            int targetDist = minDist + random.nextInt(maxDist - minDist + 1);
            
            yellowTarget = new BlockPos(
                redTarget.getX() + DX[nextDir] * targetDist,
                OBSIDIAN_Y,
                redTarget.getZ() + DZ[nextDir] * targetDist
            );
            MuzMod.LOGGER.info("[Obsidian] Yellow target: " + yellowTarget);
        } else {
            yellowTarget = null;
        }
    }
    
    // ===================== RENDERING =====================
    
    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        if (!enabled || mc.thePlayer == null) return;
        
        if (redTarget != null) {
            renderBlock(redTarget, 255, 0, 0, 150);  // KIRMIZI
        }
        if (yellowTarget != null) {
            renderBlock(yellowTarget, 255, 255, 0, 100);  // SARI
        }
        if (miningBlock != null) {
            renderBlock(miningBlock, 0, 255, 0, 120);  // YEŞİL
        }
    }
    
    private void renderBlock(BlockPos pos, int r, int g, int b, int a) {
        double x = pos.getX() - mc.getRenderManager().viewerPosX;
        double y = pos.getY() - mc.getRenderManager().viewerPosY;
        double z = pos.getZ() - mc.getRenderManager().viewerPosZ;
        
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.disableDepth();
        GlStateManager.disableLighting();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        
        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        
        // Kutu çiz
        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        
        // Alt
        wr.pos(x, y, z).color(r, g, b, a).endVertex();
        wr.pos(x+1, y, z).color(r, g, b, a).endVertex();
        wr.pos(x+1, y, z+1).color(r, g, b, a).endVertex();
        wr.pos(x, y, z+1).color(r, g, b, a).endVertex();
        
        // Üst
        wr.pos(x, y+1, z).color(r, g, b, a).endVertex();
        wr.pos(x, y+1, z+1).color(r, g, b, a).endVertex();
        wr.pos(x+1, y+1, z+1).color(r, g, b, a).endVertex();
        wr.pos(x+1, y+1, z).color(r, g, b, a).endVertex();
        
        // Ön
        wr.pos(x, y, z).color(r, g, b, a).endVertex();
        wr.pos(x, y+1, z).color(r, g, b, a).endVertex();
        wr.pos(x+1, y+1, z).color(r, g, b, a).endVertex();
        wr.pos(x+1, y, z).color(r, g, b, a).endVertex();
        
        // Arka
        wr.pos(x, y, z+1).color(r, g, b, a).endVertex();
        wr.pos(x+1, y, z+1).color(r, g, b, a).endVertex();
        wr.pos(x+1, y+1, z+1).color(r, g, b, a).endVertex();
        wr.pos(x, y+1, z+1).color(r, g, b, a).endVertex();
        
        // Sol
        wr.pos(x, y, z).color(r, g, b, a).endVertex();
        wr.pos(x, y, z+1).color(r, g, b, a).endVertex();
        wr.pos(x, y+1, z+1).color(r, g, b, a).endVertex();
        wr.pos(x, y+1, z).color(r, g, b, a).endVertex();
        
        // Sağ
        wr.pos(x+1, y, z).color(r, g, b, a).endVertex();
        wr.pos(x+1, y+1, z).color(r, g, b, a).endVertex();
        wr.pos(x+1, y+1, z+1).color(r, g, b, a).endVertex();
        wr.pos(x+1, y, z+1).color(r, g, b, a).endVertex();
        
        tess.draw();
        
        // Çerçeve
        GL11.glLineWidth(2.0f);
        wr.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        wr.pos(x, y, z).color(r, g, b, 255).endVertex();
        wr.pos(x+1, y, z).color(r, g, b, 255).endVertex();
        wr.pos(x+1, y, z+1).color(r, g, b, 255).endVertex();
        wr.pos(x, y, z+1).color(r, g, b, 255).endVertex();
        wr.pos(x, y, z).color(r, g, b, 255).endVertex();
        tess.draw();
        
        wr.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        wr.pos(x, y+1, z).color(r, g, b, 255).endVertex();
        wr.pos(x+1, y+1, z).color(r, g, b, 255).endVertex();
        wr.pos(x+1, y+1, z+1).color(r, g, b, 255).endVertex();
        wr.pos(x, y+1, z+1).color(r, g, b, 255).endVertex();
        wr.pos(x, y+1, z).color(r, g, b, 255).endVertex();
        tess.draw();
        
        GlStateManager.enableDepth();
        GlStateManager.enableTexture2D();
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }
    
    // ===================== INTERFACE =====================
    
    @Override
    public String getName() {
        return "Obsidian";
    }
    
    @Override
    public boolean shouldActivate() {
        return false;
    }
    
    @Override
    public int getPriority() {
        return 5;
    }
}
