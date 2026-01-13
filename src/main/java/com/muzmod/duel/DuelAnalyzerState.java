package com.muzmod.duel;

import com.muzmod.MuzMod;
import com.muzmod.state.AbstractState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * DuelAnalyzer State - Tracks 1v1 duel statistics
 * 
 * Features:
 * - Hit tracking between two players
 * - Golden/Enchanted apple consumption
 * - Armor break tracking
 * - Sword info capture (including lore)
 * - JSON export on duel end
 * - Draggable HUD
 */
public class DuelAnalyzerState extends AbstractState {
    
    private static DuelAnalyzerState instance;
    
    private final Minecraft mc = Minecraft.getMinecraft();
    private DuelSession session;
    private DuelHudRenderer hudRenderer;
    
    // Analiz aktif mi (state'ten bağımsız)
    private boolean analyzing = false;
    
    public DuelAnalyzerState() {
        instance = this;
        this.status = "Duel Analyzer ready";
        this.hudRenderer = new DuelHudRenderer();
        this.session = new DuelSession();
    }
    
    public static DuelAnalyzerState getInstance() {
        return instance;
    }
    
    @Override
    public String getName() {
        return "duel_analyzer";
    }
    
    @Override
    public int getPriority() {
        return 1; // Düşük öncelik - diğer state'leri etkilemez
    }
    
    @Override
    public boolean shouldActivate() {
        return false; // Manuel aktivasyon
    }
    
    @Override
    public void onTick() {
        // DuelAnalyzer kendi tick event'ini kullanıyor
        // Bu metod state sistemi için gerekli ama burada bir şey yapmıyoruz
    }
    
    @Override
    public void onEnable() {
        super.onEnable();
        MinecraftForge.EVENT_BUS.register(this);
        MuzMod.LOGGER.info("[DuelAnalyzer] State enabled");
    }
    
    @Override
    public void onDisable() {
        super.onDisable();
        MinecraftForge.EVENT_BUS.unregister(this);
        stopAnalysis();
        MuzMod.LOGGER.info("[DuelAnalyzer] State disabled");
    }
    
    /**
     * Start analyzing a duel between two players
     */
    public void startAnalysis(String player1, String player2) {
        if (analyzing) {
            stopAnalysis();
        }
        
        session.start(player1, player2);
        analyzing = true;
        setStatus("Analiz: " + player1 + " vs " + player2);
        
        // Chat mesajı
        if (mc.thePlayer != null) {
            mc.thePlayer.addChatMessage(new net.minecraft.util.ChatComponentText(
                "§6[DuelAnalyzer] §aAnaliz başladı: §e" + player1 + " §avs §e" + player2
            ));
        }
    }
    
    /**
     * Stop the current analysis
     */
    public void stopAnalysis() {
        if (!analyzing) return;
        
        // Session aktifse kaydet
        if (session.isActive()) {
            session.saveDuelRecordManual(); // Manuel kayıt
            session.stop();
            
            if (mc.thePlayer != null) {
                mc.thePlayer.addChatMessage(new net.minecraft.util.ChatComponentText(
                    "§6[DuelAnalyzer] §cAnaliz durduruldu ve kaydedildi"
                ));
            }
        }
        
        analyzing = false;
        setStatus("Analiz durduruldu");
    }
    
    /**
     * Tick event - update tracking
     */
    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!analyzing || mc.thePlayer == null) return;
        
        session.onTick();
    }
    
    /**
     * Render HUD
     */
    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;
        if (!analyzing) return;
        if (session == null) return;
        
        // HUD görünürlük kontrolü
        if (!MuzMod.instance.getConfig().isDuelHudEnabled()) return;
        
        hudRenderer.render();
    }
    
    /**
     * Living hurt event - track hits
     */
    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        if (!analyzing) return;
        if (!(event.entity instanceof EntityPlayer)) return;
        
        EntityPlayer victim = (EntityPlayer) event.entity;
        
        // Saldıran kim?
        DamageSource source = event.source;
        Entity attacker = source.getEntity();
        
        if (attacker instanceof EntityPlayer) {
            EntityPlayer attackerPlayer = (EntityPlayer) attacker;
            
            // Duel'daki oyuncular mı?
            if (session.isParticipant(attackerPlayer.getName()) && 
                session.isParticipant(victim.getName())) {
                session.recordHit(attackerPlayer.getName(), victim.getName());
            }
        }
    }
    
    /**
     * Living death event - end duel
     */
    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        if (!analyzing) return;
        if (!(event.entity instanceof EntityPlayer)) return;
        if (session == null) return;
        
        EntityPlayer deadPlayer = (EntityPlayer) event.entity;
        String deadPlayerName = deadPlayer.getName();
        
        // Duel'daki oyunculardan biri mi?
        if (session.isParticipant(deadPlayerName)) {
            MuzMod.LOGGER.info("[DuelAnalyzer] Player died in duel: " + deadPlayerName);
            
            // Session'ı sonlandır (JSON kaydet)
            session.endDuel(deadPlayerName);
            
            DuelData winner = session.getOpponentData(deadPlayerName);
            String winnerName = winner != null ? winner.getPlayerName() : "Unknown";
            
            // Chat mesajları
            if (mc.thePlayer != null) {
                mc.thePlayer.addChatMessage(new net.minecraft.util.ChatComponentText(
                    "§6[DuelAnalyzer] §aDuel bitti! Kazanan: §e" + winnerName
                ));
                mc.thePlayer.addChatMessage(new net.minecraft.util.ChatComponentText(
                    "§6[DuelAnalyzer] §7Kayıt .minecraft/BananaClient/duel_records/ klasörüne kaydedildi"
                ));
            }
            
            // Analizi sonlandır
            analyzing = false;
            setStatus("Duel bitti - Kazanan: " + winnerName);
        }
    }
    
    // ================== GETTERS ==================
    
    public DuelSession getSession() { return session; }
    public DuelHudRenderer getHudRenderer() { return hudRenderer; }
    public boolean isAnalyzing() { return analyzing; }
    
    @Override
    public String getStatus() {
        if (analyzing && session != null && session.isActive()) {
            return "§a" + status + " [" + session.getSessionDurationFormatted() + "]";
        }
        return status;
    }
}
