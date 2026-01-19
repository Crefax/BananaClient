package com.muzmod.gui.modern;

import com.muzmod.MuzMod;
import com.muzmod.config.ModConfig;
import com.muzmod.duel.DuelAnalyzerState;
import com.muzmod.duel.DuelSession;
import com.muzmod.gui.modern.components.*;
import com.muzmod.schedule.ScheduleEntry;
import com.muzmod.schedule.ScheduleManager;
import com.muzmod.state.IState;
import com.muzmod.state.impl.SafeState;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * BananaGui - Modern Apple LiquidGlass inspired GUI for MuzMod v0.8.0
 * Translucent, dynamic, and fully responsive design
 */
public class BananaGui extends GuiScreen {
    
    // Layout
    private int guiX, guiY;
    private int guiWidth = 550;
    private int guiHeight = 450;
    
    // Tab bar
    private ModernTabBar tabBar;
    private int currentTab = 0;
    
    // Content panels for each tab
    private ModernScrollPanel generalPanel;
    private ModernScrollPanel schedulePanel;
    private ModernScrollPanel settingsPanel;
    
    // Settings sub-tab bar
    private ModernTabBar settingsSubTabBar;
    private int settingsSubTab = 0;
    
    // Components
    private List<ModernButton> buttons = new ArrayList<>();
    private List<ModernToggle> toggles = new ArrayList<>();
    private List<ModernSlider> sliders = new ArrayList<>();
    private List<ModernTextField> textFields = new ArrayList<>();
    
    // General tab components
    private ModernToggle botToggle;
    private ModernToggle scheduleToggle;
    private ModernToggle hudToggle;
    private ModernToggle autoAfkToggle;
    
    // State buttons
    private ModernButton idleButton;
    private ModernButton miningButton;
    private ModernButton afkButton;
    private ModernButton repairButton;
    private ModernButton oxButton;
    private ModernButton obsidianButton;
    
    // Action buttons
    private ModernButton saveButton;
    private ModernButton closeButton;
    
    // Settings components
    // General settings
    private ModernTextField defaultMiningWarpField;
    private ModernTextField defaultAfkWarpField;
    private ModernTextField discordWebhookField;
    private ModernSlider repairThresholdSlider;
    private ModernSlider timeOffsetSlider;
    private ModernSlider detectRadiusSlider;
    private ModernSlider repairClickDelaySlider;
    
    // Mining settings
    private ModernSlider initialWalkMinSlider;
    private ModernSlider initialWalkMaxSlider;
    private ModernSlider walkYawVariationSlider;
    private ModernSlider maxDistFromCenterSlider;
    private ModernToggle secondWalkToggle;
    private ModernSlider secondWalkMinSlider;
    private ModernSlider secondWalkMaxSlider;
    private ModernSlider secondWalkAngleSlider;
    private ModernToggle strafeToggle;
    private ModernSlider strafeIntervalSlider;
    private ModernSlider strafeDurationSlider;
    
    // Mining jitter
    private ModernToggle miningJitterToggle;
    private ModernSlider miningJitterYawSlider;
    private ModernSlider miningJitterPitchSlider;
    private ModernSlider miningJitterIntervalSlider;
    
    // Player detection
    private ModernToggle instantFleeToggle;
    
    // OX settings
    private ModernSlider oxMinPlayersSlider;
    
    // Obsidian settings
    private ModernToggle obsidianJitterToggle;
    private ModernSlider obsidianJitterYawSlider;
    private ModernSlider obsidianJitterPitchSlider;
    private ModernSlider obsidianJitterIntervalSlider;
    private ModernSlider obsidianAimSpeedSlider;
    private ModernSlider obsidianTurnSpeedSlider;
    private ModernToggle obsidianSellToggle;
    private ModernToggle obsidianPlayerDetectionToggle;
    private ModernTextField obsidianSellCommandField;
    private ModernSlider obsidianSellDelaySlider;
    private ModernSlider obsidianTargetMinSlider;
    private ModernSlider obsidianTargetMaxSlider;
    
    // Duel settings
    private ModernToggle duelHudToggle;
    private ModernTextField duelPlayer1Field;
    private ModernTextField duelPlayer2Field;
    private ModernButton startAnalysisButton;
    private ModernButton stopAnalysisButton;
    
    // Config import/export
    private ModernButton exportConfigButton;
    private ModernButton importConfigButton;
    private ModernButton exportScheduleButton;
    private ModernButton importScheduleButton;
    private String configStatusMessage = "";
    private long configStatusTime = 0;
    
    // Schedule tab
    private int selectedDay = 0; // 0=Pzt, 6=Paz
    private long selectedEntryId = -1;
    private int scheduleScrollOffset = 0;
    private boolean addingNewEntry = false;
    private int newEntryType = 0; // 0=Mining, 1=AFK, 2=Repair, 3=OX
    
    // Settings scroll offsets
    private int miningScrollOffset = 0;
    private int obsidianScrollOffset = 0;
    private int configScrollOffset = 0;
    
    // Schedule input fields
    private ModernTextField fieldStartTime;
    private ModernTextField fieldEndTime;
    private ModernTextField fieldWarpCommand;
    
    // Schedule action buttons
    private ModernButton addEntryButton;
    private ModernButton newEntryButton;
    private ModernButton editSaveButton;
    private ModernButton deleteEntryButton;
    private ModernButton cancelEditButton;
    
    // Animation
    private float openAnimation = 0f;
    private boolean closing = false;
    
    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        
        // Center GUI
        guiX = (width - guiWidth) / 2;
        guiY = (height - guiHeight) / 2;
        
        initComponents();
        openAnimation = 0f;
    }
    
    private void initComponents() {
        ModConfig config = MuzMod.instance.getConfig();
        ScheduleManager schedule = MuzMod.instance.getScheduleManager();
        
        // Clear previous components
        buttons.clear();
        toggles.clear();
        sliders.clear();
        textFields.clear();
        
        // Tab bar
        tabBar = new ModernTabBar(guiX + 20, guiY + 50, guiWidth - 40, 36);
        tabBar.addTab("Genel");
        tabBar.addTab("Zamanlama");
        tabBar.addTab("Ayarlar");
        tabBar.setOnTabChange(index -> {
            currentTab = index;
        });
        
        // ==================== GENERAL TAB ====================
        int contentX = guiX + 30;
        int contentY = guiY + 100;
        int contentWidth = guiWidth - 60;
        
        // Bot toggle
        botToggle = new ModernToggle(contentX, contentY, "Bot", MuzMod.instance.isBotEnabled());
        toggles.add(botToggle);
        
        // Schedule toggle
        scheduleToggle = new ModernToggle(contentX + 150, contentY, "Zamanlama", schedule.isScheduleEnabled());
        toggles.add(scheduleToggle);
        
        // HUD toggle
        hudToggle = new ModernToggle(contentX + 320, contentY, "HUD", config.isShowOverlay());
        toggles.add(hudToggle);
        
        // Auto AFK toggle
        autoAfkToggle = new ModernToggle(contentX, contentY + 40, "Boşta AFK", schedule.isAutoAfkWhenIdle());
        toggles.add(autoAfkToggle);
        
        // State buttons row
        int buttonY = contentY + 90;
        int buttonWidth = 70;
        int buttonGap = 10;
        
        idleButton = new ModernButton(contentX, buttonY, buttonWidth, 28, "Idle", ModernButton.ButtonStyle.SECONDARY);
        buttons.add(idleButton);
        
        miningButton = new ModernButton(contentX + (buttonWidth + buttonGap), buttonY, buttonWidth, 28, "Mine", ModernButton.ButtonStyle.PRIMARY);
        buttons.add(miningButton);
        
        afkButton = new ModernButton(contentX + (buttonWidth + buttonGap) * 2, buttonY, buttonWidth, 28, "AFK", ModernButton.ButtonStyle.PRIMARY);
        buttons.add(afkButton);
        
        repairButton = new ModernButton(contentX + (buttonWidth + buttonGap) * 3, buttonY, buttonWidth, 28, "Repair", ModernButton.ButtonStyle.PRIMARY);
        buttons.add(repairButton);
        
        oxButton = new ModernButton(contentX + (buttonWidth + buttonGap) * 4, buttonY, buttonWidth, 28, "OX", ModernButton.ButtonStyle.PRIMARY);
        buttons.add(oxButton);
        
        obsidianButton = new ModernButton(contentX + (buttonWidth + buttonGap) * 5, buttonY, buttonWidth, 28, "Obsidyen", ModernButton.ButtonStyle.SECONDARY);
        buttons.add(obsidianButton);
        
        // ==================== SETTINGS TAB ====================
        // Settings sub-tab bar
        settingsSubTabBar = new ModernTabBar(guiX + 20, guiY + 95, guiWidth - 40, 32);
        settingsSubTabBar.addTab("Genel");
        settingsSubTabBar.addTab("Mining");
        settingsSubTabBar.addTab("OX");
        settingsSubTabBar.addTab("Obsidyen");
        settingsSubTabBar.addTab("Duel");
        settingsSubTabBar.addTab("Config");
        settingsSubTabBar.setOnTabChange(index -> {
            settingsSubTab = index;
        });
        
        // General settings
        int settingsY = guiY + 145;
        int fieldWidth = 200;
        
        defaultMiningWarpField = new ModernTextField(contentX + 130, settingsY, fieldWidth, 24, "Warp komutu");
        defaultMiningWarpField.setText(schedule.getDefaultMiningWarp());
        textFields.add(defaultMiningWarpField);
        
        defaultAfkWarpField = new ModernTextField(contentX + 130, settingsY + 35, fieldWidth, 24, "AFK warp");
        defaultAfkWarpField.setText(schedule.getDefaultAfkWarp());
        textFields.add(defaultAfkWarpField);
        
        discordWebhookField = new ModernTextField(contentX, settingsY + 70, 420, 24, "Discord Webhook URL");
        discordWebhookField.setText(config.getDiscordWebhookUrl());
        discordWebhookField.setMaxLength(200);
        textFields.add(discordWebhookField);
        
        repairThresholdSlider = new ModernSlider(contentX, settingsY + 110, 200, 20, "Tamir Eşiği", 50, 2000, config.getRepairDurabilityThreshold());
        repairThresholdSlider.setIntegerMode(true);
        repairThresholdSlider.setSuffix(" dur");
        sliders.add(repairThresholdSlider);
        
        timeOffsetSlider = new ModernSlider(contentX + 220, settingsY + 90, 200, 20, "Saat Farkı", -12, 12, config.getTimeOffsetHours());
        timeOffsetSlider.setIntegerMode(true);
        timeOffsetSlider.setSuffix("h");
        sliders.add(timeOffsetSlider);
        
        detectRadiusSlider = new ModernSlider(contentX, settingsY + 140, 200, 20, "Oyuncu Algılama", 1.0, 5.0, config.getPlayerDetectionRadius());
        detectRadiusSlider.setStep(0.1);
        detectRadiusSlider.setSuffix(" blok");
        sliders.add(detectRadiusSlider);
        
        repairClickDelaySlider = new ModernSlider(contentX + 220, settingsY + 140, 200, 20, "Tamir Bekleme", 0.5, 5, config.getRepairClickDelay());
        repairClickDelaySlider.setStep(0.5);
        repairClickDelaySlider.setSuffix(" sn");
        sliders.add(repairClickDelaySlider);
        
        // Mining settings
        initialWalkMinSlider = new ModernSlider(contentX, settingsY, 180, 20, "İlk Yürüme Min", 10, 100, config.getInitialWalkDistanceMin());
        initialWalkMinSlider.setIntegerMode(true);
        sliders.add(initialWalkMinSlider);
        
        initialWalkMaxSlider = new ModernSlider(contentX + 200, settingsY, 180, 20, "İlk Yürüme Max", 10, 100, config.getInitialWalkDistanceMax());
        initialWalkMaxSlider.setIntegerMode(true);
        sliders.add(initialWalkMaxSlider);
        
        walkYawVariationSlider = new ModernSlider(contentX, settingsY + 50, 180, 20, "Yaw Varyasyonu", 0, 90, config.getWalkYawVariation());
        walkYawVariationSlider.setIntegerMode(true);
        walkYawVariationSlider.setSuffix("°");
        sliders.add(walkYawVariationSlider);
        
        maxDistFromCenterSlider = new ModernSlider(contentX + 200, settingsY + 50, 180, 20, "Max Merkez Mesafesi", 10, 100, config.getMaxDistanceFromCenter());
        maxDistFromCenterSlider.setIntegerMode(true);
        sliders.add(maxDistFromCenterSlider);
        
        secondWalkToggle = new ModernToggle(contentX, settingsY + 100, "İkinci Yürüme", config.isSecondWalkEnabled());
        toggles.add(secondWalkToggle);
        
        secondWalkMinSlider = new ModernSlider(contentX, settingsY + 130, 180, 20, "2. Yürüme Min", 10, 100, config.getSecondWalkDistanceMin());
        secondWalkMinSlider.setIntegerMode(true);
        sliders.add(secondWalkMinSlider);
        
        secondWalkMaxSlider = new ModernSlider(contentX + 200, settingsY + 130, 180, 20, "2. Yürüme Max", 10, 100, config.getSecondWalkDistanceMax());
        secondWalkMaxSlider.setIntegerMode(true);
        sliders.add(secondWalkMaxSlider);
        
        secondWalkAngleSlider = new ModernSlider(contentX, settingsY + 180, 180, 20, "2. Yürüme Açısı", 0, 180, config.getSecondWalkAngleVariation());
        secondWalkAngleSlider.setIntegerMode(true);
        secondWalkAngleSlider.setSuffix("°");
        sliders.add(secondWalkAngleSlider);
        
        strafeToggle = new ModernToggle(contentX, settingsY + 220, "Strafe", config.isStrafeEnabled());
        toggles.add(strafeToggle);
        
        strafeIntervalSlider = new ModernSlider(contentX, settingsY + 250, 180, 20, "Strafe Aralığı", 1, 30, config.getStrafeInterval() / 1000);
        strafeIntervalSlider.setIntegerMode(true);
        strafeIntervalSlider.setSuffix("s");
        sliders.add(strafeIntervalSlider);
        
        strafeDurationSlider = new ModernSlider(contentX + 200, settingsY + 250, 180, 20, "Strafe Süresi", 100, 2000, config.getStrafeDuration());
        strafeDurationSlider.setIntegerMode(true);
        strafeDurationSlider.setSuffix("ms");
        sliders.add(strafeDurationSlider);
        
        // Mining jitter
        miningJitterToggle = new ModernToggle(contentX, settingsY + 300, "Mining Jitter", config.isMiningJitterEnabled());
        toggles.add(miningJitterToggle);
        
        miningJitterYawSlider = new ModernSlider(contentX, settingsY + 330, 120, 20, "Jitter Yaw", 0, 10, config.getMiningJitterYaw());
        miningJitterYawSlider.setStep(0.5);
        sliders.add(miningJitterYawSlider);
        
        miningJitterPitchSlider = new ModernSlider(contentX + 140, settingsY + 330, 120, 20, "Jitter Pitch", 0, 10, config.getMiningJitterPitch());
        miningJitterPitchSlider.setStep(0.5);
        sliders.add(miningJitterPitchSlider);
        
        miningJitterIntervalSlider = new ModernSlider(contentX + 280, settingsY + 330, 120, 20, "Jitter Interval", 50, 1000, config.getMiningJitterInterval());
        miningJitterIntervalSlider.setIntegerMode(true);
        miningJitterIntervalSlider.setSuffix("ms");
        sliders.add(miningJitterIntervalSlider);
        
        // Player detection - Instant Flee
        instantFleeToggle = new ModernToggle(contentX, settingsY + 380, "Anında Kaç", config.isInstantFlee());
        toggles.add(instantFleeToggle);
        
        // OX settings
        oxMinPlayersSlider = new ModernSlider(contentX, settingsY, 200, 20, "Min Oyuncu", 1, 20, config.getOxMinPlayers());
        oxMinPlayersSlider.setIntegerMode(true);
        sliders.add(oxMinPlayersSlider);
        
        // Obsidian settings
        obsidianJitterToggle = new ModernToggle(contentX, settingsY, "Obsidyen Jitter", config.isObsidianJitterEnabled());
        toggles.add(obsidianJitterToggle);
        
        obsidianJitterYawSlider = new ModernSlider(contentX, settingsY + 30, 120, 20, "Jitter Yaw", 0, 10, config.getObsidianJitterYaw());
        obsidianJitterYawSlider.setStep(0.5);
        sliders.add(obsidianJitterYawSlider);
        
        obsidianJitterPitchSlider = new ModernSlider(contentX + 140, settingsY + 30, 120, 20, "Jitter Pitch", 0, 10, config.getObsidianJitterPitch());
        obsidianJitterPitchSlider.setStep(0.5);
        sliders.add(obsidianJitterPitchSlider);
        
        obsidianJitterIntervalSlider = new ModernSlider(contentX + 280, settingsY + 30, 120, 20, "Interval", 50, 1000, config.getObsidianJitterInterval());
        obsidianJitterIntervalSlider.setIntegerMode(true);
        obsidianJitterIntervalSlider.setSuffix("ms");
        sliders.add(obsidianJitterIntervalSlider);
        
        obsidianAimSpeedSlider = new ModernSlider(contentX, settingsY + 80, 200, 20, "Aim Hızı", 0.01, 1.0, config.getObsidianAimSpeed());
        obsidianAimSpeedSlider.setStep(0.01);
        sliders.add(obsidianAimSpeedSlider);
        
        obsidianTurnSpeedSlider = new ModernSlider(contentX + 220, settingsY + 80, 200, 20, "Dönüş Hızı", 0.0, 1.0, config.getObsidianTurnSpeed());
        obsidianTurnSpeedSlider.setStep(0.01);
        sliders.add(obsidianTurnSpeedSlider);
        
        obsidianSellToggle = new ModernToggle(contentX, settingsY + 130, "Otomatik Satış", config.isObsidianSellEnabled());
        toggles.add(obsidianSellToggle);
        
        obsidianPlayerDetectionToggle = new ModernToggle(contentX + 150, settingsY + 130, "Oyuncu Algılama", config.isObsidianPlayerDetectionEnabled());
        toggles.add(obsidianPlayerDetectionToggle);
        
        obsidianSellCommandField = new ModernTextField(contentX + 130, settingsY + 160, 150, 24, "/sell all");
        obsidianSellCommandField.setText(config.getObsidianSellCommand());
        textFields.add(obsidianSellCommandField);
        
        obsidianSellDelaySlider = new ModernSlider(contentX + 300, settingsY + 160, 120, 20, "Satış Gecikmesi", 1, 60, config.getObsidianSellDelay() / 1000.0);
        obsidianSellDelaySlider.setStep(1);
        obsidianSellDelaySlider.setSuffix("s");
        sliders.add(obsidianSellDelaySlider);
        
        obsidianTargetMinSlider = new ModernSlider(contentX, settingsY + 210, 180, 20, "Hedef Min Offset", 0, 30, config.getObsidianTargetMinOffset());
        obsidianTargetMinSlider.setIntegerMode(true);
        sliders.add(obsidianTargetMinSlider);
        
        obsidianTargetMaxSlider = new ModernSlider(contentX + 200, settingsY + 210, 180, 20, "Hedef Max Offset", 0, 30, config.getObsidianTargetMaxOffset());
        obsidianTargetMaxSlider.setIntegerMode(true);
        sliders.add(obsidianTargetMaxSlider);
        
        // Duel settings
        duelHudToggle = new ModernToggle(contentX, settingsY, "Duel HUD", config.isDuelHudEnabled());
        toggles.add(duelHudToggle);
        
        duelPlayer1Field = new ModernTextField(contentX + 100, settingsY + 40, 150, 24, "Oyuncu 1");
        textFields.add(duelPlayer1Field);
        
        duelPlayer2Field = new ModernTextField(contentX + 100, settingsY + 75, 150, 24, "Oyuncu 2");
        textFields.add(duelPlayer2Field);
        
        startAnalysisButton = new ModernButton(contentX, settingsY + 120, 130, 28, "Analizi Başlat", ModernButton.ButtonStyle.SUCCESS);
        buttons.add(startAnalysisButton);
        
        stopAnalysisButton = new ModernButton(contentX + 150, settingsY + 120, 130, 28, "Analizi Durdur", ModernButton.ButtonStyle.DANGER);
        buttons.add(stopAnalysisButton);
        
        // ==================== CONFIG IMPORT/EXPORT ====================
        exportConfigButton = new ModernButton(contentX, settingsY, 180, 32, "Config → Default", ModernButton.ButtonStyle.PRIMARY);
        buttons.add(exportConfigButton);
        
        importConfigButton = new ModernButton(contentX + 200, settingsY, 180, 32, "Default → Config", ModernButton.ButtonStyle.SECONDARY);
        buttons.add(importConfigButton);
        
        exportScheduleButton = new ModernButton(contentX, settingsY + 90, 180, 32, "Schedule → Default", ModernButton.ButtonStyle.PRIMARY);
        buttons.add(exportScheduleButton);
        
        importScheduleButton = new ModernButton(contentX + 200, settingsY + 90, 180, 32, "Default → Schedule", ModernButton.ButtonStyle.SECONDARY);
        buttons.add(importScheduleButton);
        
        // ==================== SCHEDULE TAB COMPONENTS ====================
        int schedPanelX = guiX + 260;
        int schedPanelY = guiY + 110;
        
        fieldStartTime = new ModernTextField(schedPanelX + 70, schedPanelY + 25, 60, 20, "10:00");
        fieldStartTime.setText("10:00");
        textFields.add(fieldStartTime);
        
        fieldEndTime = new ModernTextField(schedPanelX + 70, schedPanelY + 55, 60, 20, "12:00");
        fieldEndTime.setText("12:00");
        textFields.add(fieldEndTime);
        
        fieldWarpCommand = new ModernTextField(schedPanelX + 10, schedPanelY + 115, 170, 20, "/warp maden");
        fieldWarpCommand.setText(schedule.getDefaultMiningWarp());
        textFields.add(fieldWarpCommand);
        
        newEntryButton = new ModernButton(schedPanelX + 30, schedPanelY + 60, 130, 26, "+ Yeni Etkinlik", ModernButton.ButtonStyle.SUCCESS);
        buttons.add(newEntryButton);
        
        addEntryButton = new ModernButton(schedPanelX + 10, schedPanelY + 145, 80, 24, "Ekle", ModernButton.ButtonStyle.SUCCESS);
        buttons.add(addEntryButton);
        
        editSaveButton = new ModernButton(schedPanelX + 10, schedPanelY + 145, 55, 24, "Kaydet", ModernButton.ButtonStyle.SUCCESS);
        buttons.add(editSaveButton);
        
        deleteEntryButton = new ModernButton(schedPanelX + 70, schedPanelY + 145, 55, 24, "Sil", ModernButton.ButtonStyle.DANGER);
        buttons.add(deleteEntryButton);
        
        cancelEditButton = new ModernButton(schedPanelX + 100, schedPanelY + 145, 80, 24, "İptal", ModernButton.ButtonStyle.SECONDARY);
        buttons.add(cancelEditButton);
        
        // ==================== BOTTOM BUTTONS ====================
        saveButton = new ModernButton(guiX + guiWidth - 170, guiY + guiHeight - 45, 75, 30, "Kaydet", ModernButton.ButtonStyle.SUCCESS);
        buttons.add(saveButton);
        
        closeButton = new ModernButton(guiX + guiWidth - 85, guiY + guiHeight - 45, 75, 30, "Kapat", ModernButton.ButtonStyle.DANGER);
        buttons.add(closeButton);
    }
    
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // Update open animation
        if (!closing) {
            openAnimation = Math.min(1f, openAnimation + 0.1f);
        } else {
            openAnimation = Math.max(0f, openAnimation - 0.15f);
            if (openAnimation <= 0f) {
                mc.displayGuiScreen(null);
                return;
            }
        }
        
        // Draw dark background overlay
        int bgAlpha = (int)(0xC0 * openAnimation);
        drawRect(0, 0, width, height, (bgAlpha << 24) | 0x000000);
        
        // Apply scale animation
        float scale = 0.9f + 0.1f * openAnimation;
        int centerX = width / 2;
        int centerY = height / 2;
        
        // Draw main panel with glass effect
        int panelAlpha = (int)(0xE0 * openAnimation);
        GuiRenderUtils.drawFrostedPanel(guiX, guiY, guiWidth, guiHeight);
        
        // Draw header
        drawHeader(mouseX, mouseY);
        
        // Draw tab bar
        tabBar.render(mouseX, mouseY);
        
        // Draw content based on current tab
        switch (currentTab) {
            case 0: drawGeneralTab(mouseX, mouseY); break;
            case 1: drawScheduleTab(mouseX, mouseY); break;
            case 2: drawSettingsTab(mouseX, mouseY); break;
        }
        
        // Draw footer
        drawFooter(mouseX, mouseY);
        
        // Draw save/close buttons
        saveButton.render(mouseX, mouseY);
        closeButton.render(mouseX, mouseY);
        
        // Draw safe state warning if applicable
        drawSafeStateWarning();
        
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
    
    private void drawHeader(int mouseX, int mouseY) {
        // Title
        String title = "§l" + MuzMod.MOD_NAME + " §r§7v" + MuzMod.VERSION;
        GuiRenderUtils.drawText(title, guiX + 20, guiY + 18, GuiTheme.TEXT_PRIMARY);
        
        // Time display
        Calendar cal = Calendar.getInstance();
        ModConfig config = MuzMod.instance.getConfig();
        int hour = (cal.get(Calendar.HOUR_OF_DAY) + config.getTimeOffsetHours() + 24) % 24;
        String timeStr = String.format("§e%02d:%02d", hour, cal.get(Calendar.MINUTE));
        int timeWidth = GuiRenderUtils.getTextWidth(timeStr);
        GuiRenderUtils.drawText(timeStr, guiX + guiWidth - timeWidth - 20, guiY + 18, GuiTheme.TEXT_SECONDARY);
        
        // Header gradient line
        GuiRenderUtils.drawGradientRectH(guiX + 10, guiY + 42, guiWidth - 20, 2, GuiTheme.ACCENT_PRIMARY, GuiTheme.ACCENT_SECONDARY);
    }
    
    private void drawFooter(int mouseX, int mouseY) {
        // Footer gradient line
        GuiRenderUtils.drawGradientRectH(guiX + 10, guiY + guiHeight - 55, guiWidth - 20, 2, GuiTheme.ACCENT_PRIMARY, GuiTheme.ACCENT_SECONDARY);
        
        // GitHub URL
        GuiRenderUtils.drawText("§8" + MuzMod.GITHUB_URL, guiX + 20, guiY + guiHeight - 38, GuiTheme.TEXT_MUTED);
    }
    
    private void drawGeneralTab(int mouseX, int mouseY) {
        int contentX = guiX + 30;
        int contentY = guiY + 100;
        
        // Update toggle positions and render
        botToggle.setPosition(contentX, contentY);
        botToggle.render(mouseX, mouseY);
        
        scheduleToggle.setPosition(contentX + 150, contentY);
        scheduleToggle.render(mouseX, mouseY);
        
        hudToggle.setPosition(contentX + 320, contentY);
        hudToggle.render(mouseX, mouseY);
        
        autoAfkToggle.setPosition(contentX, contentY + 40);
        autoAfkToggle.render(mouseX, mouseY);
        
        // Section label
        GuiRenderUtils.drawText("§7Manuel Durum:", contentX, contentY + 70, GuiTheme.TEXT_SECONDARY);
        
        // State buttons
        int buttonY = contentY + 90;
        int buttonWidth = 70;
        int buttonGap = 10;
        
        idleButton.setPosition(contentX, buttonY);
        idleButton.render(mouseX, mouseY);
        
        miningButton.setPosition(contentX + (buttonWidth + buttonGap), buttonY);
        miningButton.render(mouseX, mouseY);
        
        afkButton.setPosition(contentX + (buttonWidth + buttonGap) * 2, buttonY);
        afkButton.render(mouseX, mouseY);
        
        repairButton.setPosition(contentX + (buttonWidth + buttonGap) * 3, buttonY);
        repairButton.render(mouseX, mouseY);
        
        oxButton.setPosition(contentX + (buttonWidth + buttonGap) * 4, buttonY);
        oxButton.render(mouseX, mouseY);
        
        obsidianButton.setPosition(contentX + (buttonWidth + buttonGap) * 5, buttonY);
        obsidianButton.render(mouseX, mouseY);
        
        // Current state info
        int infoY = contentY + 135;
        IState state = MuzMod.instance.getStateManager().getCurrentState();
        String stateName = state != null ? state.getName() : "Idle";
        String status = state != null ? state.getStatus() : "Bekleniyor...";
        
        GuiRenderUtils.drawText("§7Mevcut Durum: §f" + stateName, contentX, infoY, GuiTheme.TEXT_PRIMARY);
        GuiRenderUtils.drawText("§8" + truncate(status, 60), contentX, infoY + 15, GuiTheme.TEXT_MUTED);
        
        // Scheduled event info
        ScheduleManager schedule = MuzMod.instance.getScheduleManager();
        Calendar cal = Calendar.getInstance();
        ModConfig config = MuzMod.instance.getConfig();
        int hour = (cal.get(Calendar.HOUR_OF_DAY) + config.getTimeOffsetHours() + 24) % 24;
        int minute = cal.get(Calendar.MINUTE);
        int javaDow = cal.get(Calendar.DAY_OF_WEEK);
        int dayOfWeek = (javaDow == Calendar.SUNDAY) ? 6 : javaDow - 2;
        
        ScheduleEntry currentEntry = schedule.getCurrentEntry(dayOfWeek, hour, minute);
        int schedY = infoY + 45;
        if (currentEntry != null) {
            GuiRenderUtils.drawText("§7Aktif Etkinlik: §f" + currentEntry.getEventType().getDisplayName(), contentX, schedY, GuiTheme.TEXT_PRIMARY);
            GuiRenderUtils.drawText("§8" + currentEntry.getTimeRange(), contentX, schedY + 15, GuiTheme.TEXT_MUTED);
        } else {
            ScheduleEntry.EventType type = schedule.getCurrentScheduledType(dayOfWeek, hour, minute);
            GuiRenderUtils.drawText("§7Zamanlanan: §f" + type.getDisplayName(), contentX, schedY, GuiTheme.TEXT_PRIMARY);
        }
    }
    
    private void drawScheduleTab(int mouseX, int mouseY) {
        ScheduleManager schedule = MuzMod.instance.getScheduleManager();
        
        // Day selector bar
        int dayY = guiY + 95;
        String[] shortDays = {"Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz"};
        int dayWidth = 70;
        
        for (int i = 0; i < 7; i++) {
            int dx = guiX + 20 + i * dayWidth;
            boolean hovered = mouseX >= dx && mouseX < dx + dayWidth - 4 && mouseY >= dayY && mouseY < dayY + 24;
            boolean selected = selectedDay == i;
            
            int bg = selected ? 0xFF6366F1 : (hovered ? 0xFF4B5563 : 0xFF374151);
            GuiRenderUtils.drawRoundedRect(dx, dayY, dayWidth - 4, 24, 4, bg);
            
            int entryCount = schedule.getEntryCountForDay(i);
            String dayText = shortDays[i] + (entryCount > 0 ? " §7(" + entryCount + ")" : "");
            int textW = GuiRenderUtils.getTextWidth(dayText);
            GuiRenderUtils.drawText(dayText, dx + (dayWidth - 4 - textW) / 2, dayY + 8, selected ? 0xFFFFFFFF : 0xFFD1D5DB);
        }
        
        // Entry list for selected day
        int listX = guiX + 20;
        int listY = guiY + 130;
        int listW = 225;
        int listH = 200;
        
        GuiRenderUtils.drawRoundedRect(listX, listY, listW, listH, 6, 0xE0202020);
        GuiRenderUtils.drawText("§f" + ScheduleEntry.getDayName(selectedDay) + " Etkinlikleri", listX + 10, listY + 8, 0xFFFFFFFF);
        
        List<ScheduleEntry> entries = schedule.getEntriesForDay(selectedDay);
        int itemH = 32;
        int visibleItems = 5;
        int itemY = listY + 28;
        
        // Scrollable entry list
        for (int i = scheduleScrollOffset; i < Math.min(entries.size(), scheduleScrollOffset + visibleItems); i++) {
            ScheduleEntry entry = entries.get(i);
            boolean itemHover = mouseX >= listX + 5 && mouseX < listX + listW - 5 && mouseY >= itemY && mouseY < itemY + itemH - 2;
            boolean itemSelected = entry.getId() == selectedEntryId;
            
            int itemBg = itemSelected ? 0xFF3B3B5A : (itemHover ? 0xFF353545 : 0xFF2A2A35);
            GuiRenderUtils.drawRoundedRect(listX + 5, itemY, listW - 10, itemH - 2, 4, itemBg);
            
            // Color indicator
            GuiRenderUtils.drawRoundedRect(listX + 5, itemY, 4, itemH - 2, 2, entry.getEventType().getColor());
            
            // Time & Type
            String timeStr = entry.getTimeRange();
            String typeStr = entry.getEventType().getDisplayName();
            GuiRenderUtils.drawText("§f" + timeStr, listX + 14, itemY + 5, 0xFFFFFFFF);
            GuiRenderUtils.drawText("§7" + typeStr, listX + 14, itemY + 17, 0xFFAAAAAA);
            
            // Enabled indicator
            if (!entry.isEnabled()) {
                GuiRenderUtils.drawText("§c✗", listX + listW - 22, itemY + 10, 0xFFEF4444);
            }
            
            itemY += itemH;
        }
        
        // Scroll indicators
        if (scheduleScrollOffset > 0) {
            GuiRenderUtils.drawText("§7▲", listX + listW / 2, listY + 26, 0xFF888888);
        }
        if (entries.size() > scheduleScrollOffset + visibleItems) {
            GuiRenderUtils.drawText("§7▼", listX + listW / 2, listY + listH - 14, 0xFF888888);
        }
        
        // Empty state
        if (entries.isEmpty()) {
            GuiRenderUtils.drawCenteredText("§8Etkinlik yok", listX + listW / 2, listY + 90, 0xFF666666);
        }
        
        // Right panel - Add/Edit entry
        int panelX = guiX + 260;
        int panelY = guiY + 130;
        int panelW = 200;
        int panelH = 200;
        
        GuiRenderUtils.drawRoundedRect(panelX, panelY, panelW, panelH, 6, 0xE0202020);
        
        if (addingNewEntry || selectedEntryId != -1) {
            String title = addingNewEntry ? "§aYeni Etkinlik" : "§eDüzenle";
            GuiRenderUtils.drawText(title, panelX + 10, panelY + 8, 0xFFFFFFFF);
            
            // Time inputs
            GuiRenderUtils.drawText("§7Başlangıç:", panelX + 10, panelY + 32, 0xFFAAAAAA);
            fieldStartTime.setPosition(panelX + 80, panelY + 28);
            fieldStartTime.render(mouseX, mouseY);
            
            GuiRenderUtils.drawText("§7Bitiş:", panelX + 10, panelY + 60, 0xFFAAAAAA);
            fieldEndTime.setPosition(panelX + 80, panelY + 56);
            fieldEndTime.render(mouseX, mouseY);
            
            // Event type selector
            GuiRenderUtils.drawText("§7Tip:", panelX + 10, panelY + 88, 0xFFAAAAAA);
            String[] types = {"§6Maden", "§bAFK", "§eTamir", "§dOX"};
            int[] typeColors = {0xFFFF9800, 0xFF00BCD4, 0xFFFFEB3B, 0xFF9B59B6};
            for (int i = 0; i < 4; i++) {
                int tx = panelX + 10 + i * 46;
                boolean tHover = mouseX >= tx && mouseX < tx + 44 && mouseY >= panelY + 100 && mouseY < panelY + 118;
                boolean tSel = newEntryType == i;
                int btnBg = tSel ? typeColors[i] : (tHover ? 0xFF404050 : 0xFF303040);
                GuiRenderUtils.drawRoundedRect(tx, panelY + 100, 44, 18, 4, btnBg);
                GuiRenderUtils.drawCenteredText(types[i], tx + 22, panelY + 105, 0xFFFFFFFF);
            }
            
            // Warp command
            GuiRenderUtils.drawText("§7Warp (opsiyonel):", panelX + 10, panelY + 125, 0xFFAAAAAA);
            fieldWarpCommand.setPosition(panelX + 10, panelY + 140);
            fieldWarpCommand.setWidth(panelW - 20);
            fieldWarpCommand.render(mouseX, mouseY);
            
            // Action buttons
            if (addingNewEntry) {
                addEntryButton.setPosition(panelX + 10, panelY + 170);
                addEntryButton.render(mouseX, mouseY);
                cancelEditButton.setPosition(panelX + 100, panelY + 170);
                cancelEditButton.render(mouseX, mouseY);
            } else {
                editSaveButton.setPosition(panelX + 10, panelY + 170);
                editSaveButton.render(mouseX, mouseY);
                deleteEntryButton.setPosition(panelX + 70, panelY + 170);
                deleteEntryButton.render(mouseX, mouseY);
                cancelEditButton.setPosition(panelX + 130, panelY + 170);
                cancelEditButton.render(mouseX, mouseY);
            }
        } else {
            GuiRenderUtils.drawText("§7Etkinlik Ekle", panelX + 10, panelY + 8, 0xFFAAAAAA);
            newEntryButton.setPosition(panelX + 35, panelY + 80);
            newEntryButton.render(mouseX, mouseY);
            
            // Quick actions info
            GuiRenderUtils.drawText("§8Listeden bir etkinlik", panelX + 20, panelY + 130, 0xFF666666);
            GuiRenderUtils.drawText("§8seçerek düzenleyin", panelX + 25, panelY + 145, 0xFF666666);
        }
    }
    
    private void drawSettingsTab(int mouseX, int mouseY) {
        // Settings sub-tab bar
        settingsSubTabBar.render(mouseX, mouseY);
        
        int contentX = guiX + 30;
        int contentY = guiY + 145;
        
        switch (settingsSubTab) {
            case 0: drawGeneralSettings(contentX, contentY, mouseX, mouseY); break;
            case 1: drawMiningSettings(contentX, contentY, mouseX, mouseY); break;
            case 2: drawOXSettings(contentX, contentY, mouseX, mouseY); break;
            case 3: drawObsidianSettings(contentX, contentY, mouseX, mouseY); break;
            case 4: drawDuelSettings(contentX, contentY, mouseX, mouseY); break;
            case 5: drawConfigSettings(contentX, contentY, mouseX, mouseY); break;
        }
    }
    
    private void drawGeneralSettings(int x, int y, int mouseX, int mouseY) {
        GuiRenderUtils.drawText("§7Default Mining Warp:", x, y + 5, GuiTheme.TEXT_SECONDARY);
        defaultMiningWarpField.setPosition(x + 130, y);
        defaultMiningWarpField.render(mouseX, mouseY);
        
        GuiRenderUtils.drawText("§7Default AFK Warp:", x, y + 40, GuiTheme.TEXT_SECONDARY);
        defaultAfkWarpField.setPosition(x + 130, y + 35);
        defaultAfkWarpField.render(mouseX, mouseY);
        
        GuiRenderUtils.drawText("§7Discord Webhook:", x, y + 75, GuiTheme.TEXT_SECONDARY);
        discordWebhookField.setPosition(x + 120, y + 70);
        discordWebhookField.render(mouseX, mouseY);
        
        repairThresholdSlider.setPosition(x, y + 110);
        repairThresholdSlider.render(mouseX, mouseY);
        
        timeOffsetSlider.setPosition(x + 220, y + 110);
        timeOffsetSlider.render(mouseX, mouseY);
        
        detectRadiusSlider.setPosition(x, y + 160);
        detectRadiusSlider.render(mouseX, mouseY);
        
        repairClickDelaySlider.setPosition(x + 220, y + 160);
        repairClickDelaySlider.render(mouseX, mouseY);
    }
    
    private void drawMiningSettings(int x, int y, int mouseX, int mouseY) {
        // Use scissor for scrollable content
        GuiRenderUtils.enableScissor(guiX, guiY + 135, guiWidth, guiHeight - 200);
        
        // Apply scroll offset
        int scrollY = y - miningScrollOffset;
        int sliderWidth = 180;
        int sliderGap = 20;
        
        // === İLK YÜRÜME ===
        GuiRenderUtils.drawText("§6§lİlk Yürüme", x, scrollY, 0xFFFF9800);
        
        initialWalkMinSlider.setPosition(x, scrollY + 25);
        initialWalkMinSlider.render(mouseX, mouseY);
        
        initialWalkMaxSlider.setPosition(x + sliderWidth + sliderGap, scrollY + 25);
        initialWalkMaxSlider.render(mouseX, mouseY);
        
        walkYawVariationSlider.setPosition(x, scrollY + 55);
        walkYawVariationSlider.render(mouseX, mouseY);
        
        maxDistFromCenterSlider.setPosition(x + sliderWidth + sliderGap, scrollY + 55);
        maxDistFromCenterSlider.render(mouseX, mouseY);
        
        // === İKİNCİ YÜRÜME ===
        GuiRenderUtils.drawText("§b§lİkinci Yürüme", x, scrollY + 100, 0xFF00BCD4);
        
        secondWalkToggle.setPosition(x + 110, scrollY + 98);
        secondWalkToggle.render(mouseX, mouseY);
        
        secondWalkMinSlider.setPosition(x, scrollY + 125);
        secondWalkMinSlider.render(mouseX, mouseY);
        
        secondWalkMaxSlider.setPosition(x + sliderWidth + sliderGap, scrollY + 125);
        secondWalkMaxSlider.render(mouseX, mouseY);
        
        secondWalkAngleSlider.setPosition(x, scrollY + 155);
        secondWalkAngleSlider.render(mouseX, mouseY);
        
        // === STRAFE ANTI-AFK ===
        GuiRenderUtils.drawText("§e§lStrafe Anti-AFK", x, scrollY + 200, 0xFFFFEB3B);
        
        strafeToggle.setPosition(x + 130, scrollY + 198);
        strafeToggle.render(mouseX, mouseY);
        
        strafeIntervalSlider.setPosition(x, scrollY + 225);
        strafeIntervalSlider.render(mouseX, mouseY);
        
        strafeDurationSlider.setPosition(x + sliderWidth + sliderGap, scrollY + 225);
        strafeDurationSlider.render(mouseX, mouseY);
        
        // === JITTER ANTI-AFK ===
        GuiRenderUtils.drawText("§c§lJitter Anti-AFK", x, scrollY + 270, 0xFFEF4444);
        
        miningJitterToggle.setPosition(x + 115, scrollY + 268);
        miningJitterToggle.render(mouseX, mouseY);
        
        miningJitterYawSlider.setPosition(x, scrollY + 295);
        miningJitterYawSlider.render(mouseX, mouseY);
        
        miningJitterPitchSlider.setPosition(x + 130, scrollY + 295);
        miningJitterPitchSlider.render(mouseX, mouseY);
        
        miningJitterIntervalSlider.setPosition(x + 260, scrollY + 295);
        miningJitterIntervalSlider.render(mouseX, mouseY);
        
        // === OYUNCU TESPİTİ ===
        GuiRenderUtils.drawText("§d§lOyuncu Tespiti", x, scrollY + 340, 0xFFE91E63);
        
        instantFleeToggle.setPosition(x + 120, scrollY + 338);
        instantFleeToggle.render(mouseX, mouseY);
        
        GuiRenderUtils.drawText("§7Oyuncu görünce hemen kaç", x, scrollY + 365, GuiTheme.TEXT_MUTED);
        
        GuiRenderUtils.disableScissor();
        
        // Scroll indicator
        int contentHeight = 420;
        int visibleHeight = guiHeight - 200;
        if (contentHeight > visibleHeight) {
            int scrollBarHeight = Math.max(20, visibleHeight * visibleHeight / contentHeight);
            int maxScroll = contentHeight - visibleHeight;
            int scrollBarY = guiY + 135 + (miningScrollOffset * (visibleHeight - scrollBarHeight) / maxScroll);
            GuiRenderUtils.drawRect(guiX + guiWidth - 8, guiY + 135, 4, visibleHeight, 0x40FFFFFF);
            GuiRenderUtils.drawRect(guiX + guiWidth - 8, scrollBarY, 4, scrollBarHeight, 0x80FFFFFF);
        }
    }
    
    private void drawOXSettings(int x, int y, int mouseX, int mouseY) {
        oxMinPlayersSlider.setPosition(x, y);
        oxMinPlayersSlider.render(mouseX, mouseY);
    }
    
    private void drawObsidianSettings(int x, int y, int mouseX, int mouseY) {
        GuiRenderUtils.enableScissor(guiX, guiY + 135, guiWidth, guiHeight - 200);
        
        int scrollY = y - obsidianScrollOffset;
        
        obsidianJitterToggle.setPosition(x, scrollY);
        obsidianJitterToggle.render(mouseX, mouseY);
        
        obsidianJitterYawSlider.setPosition(x, scrollY + 30);
        obsidianJitterYawSlider.render(mouseX, mouseY);
        
        obsidianJitterPitchSlider.setPosition(x + 140, scrollY + 30);
        obsidianJitterPitchSlider.render(mouseX, mouseY);
        
        obsidianJitterIntervalSlider.setPosition(x + 280, scrollY + 30);
        obsidianJitterIntervalSlider.render(mouseX, mouseY);
        
        obsidianAimSpeedSlider.setPosition(x, scrollY + 80);
        obsidianAimSpeedSlider.render(mouseX, mouseY);
        
        obsidianTurnSpeedSlider.setPosition(x + 220, scrollY + 80);
        obsidianTurnSpeedSlider.render(mouseX, mouseY);
        
        obsidianSellToggle.setPosition(x, scrollY + 130);
        obsidianSellToggle.render(mouseX, mouseY);
        
        obsidianPlayerDetectionToggle.setPosition(x + 150, scrollY + 130);
        obsidianPlayerDetectionToggle.render(mouseX, mouseY);
        
        GuiRenderUtils.drawText("§7Satış Komutu:", x, scrollY + 165, GuiTheme.TEXT_SECONDARY);
        obsidianSellCommandField.setPosition(x + 100, scrollY + 160);
        obsidianSellCommandField.render(mouseX, mouseY);
        
        obsidianSellDelaySlider.setPosition(x + 280, scrollY + 160);
        obsidianSellDelaySlider.render(mouseX, mouseY);
        
        obsidianTargetMinSlider.setPosition(x, scrollY + 210);
        obsidianTargetMinSlider.render(mouseX, mouseY);
        
        obsidianTargetMaxSlider.setPosition(x + 200, scrollY + 210);
        obsidianTargetMaxSlider.render(mouseX, mouseY);
        
        GuiRenderUtils.disableScissor();
    }
    
    private void drawDuelSettings(int x, int y, int mouseX, int mouseY) {
        duelHudToggle.setPosition(x, y);
        duelHudToggle.render(mouseX, mouseY);
        
        GuiRenderUtils.drawText("§7Oyuncu 1:", x, y + 45, GuiTheme.TEXT_SECONDARY);
        duelPlayer1Field.setPosition(x + 70, y + 40);
        duelPlayer1Field.render(mouseX, mouseY);
        
        GuiRenderUtils.drawText("§7Oyuncu 2:", x, y + 80, GuiTheme.TEXT_SECONDARY);
        duelPlayer2Field.setPosition(x + 70, y + 75);
        duelPlayer2Field.render(mouseX, mouseY);
        
        startAnalysisButton.setPosition(x, y + 120);
        startAnalysisButton.render(mouseX, mouseY);
        
        stopAnalysisButton.setPosition(x + 150, y + 120);
        stopAnalysisButton.render(mouseX, mouseY);
        
        // Show analysis status
        DuelAnalyzerState analyzer = MuzMod.instance.getDuelAnalyzerState();
        if (analyzer != null && analyzer.isAnalyzing()) {
            GuiRenderUtils.drawText("§a● Analiz Aktif", x, y + 165, GuiTheme.SUCCESS);
            
            DuelSession session = analyzer.getSession();
            if (session != null) {
                GuiRenderUtils.drawText("§7Süre: §f" + session.getSessionDurationFormatted(), x, y + 180, GuiTheme.TEXT_SECONDARY);
            }
        } else {
            GuiRenderUtils.drawText("§8○ Analiz Beklemede", x, y + 165, GuiTheme.TEXT_MUTED);
        }
    }
    
    private void drawConfigSettings(int x, int y, int mouseX, int mouseY) {
        String playerName = MuzMod.instance.getCurrentPlayerName();
        if (playerName == null) playerName = "Bilinmiyor";
        
        // Header
        GuiRenderUtils.drawText("§6§lConfig Yönetimi", x, y - 5, 0xFFFF9800);
        GuiRenderUtils.drawText("§7Aktif Hesap: §f" + playerName, x, y + 15, GuiTheme.TEXT_SECONDARY);
        
        // Config section
        GuiRenderUtils.drawText("§e§lAyarlar (Config)", x, y + 50, 0xFFFFEB3B);
        GuiRenderUtils.drawText("§7Kendi ayarlarını varsayılan olarak kaydet veya", x, y + 70, GuiTheme.TEXT_MUTED);
        GuiRenderUtils.drawText("§7varsayılan ayarları kendi hesabına aktar.", x, y + 82, GuiTheme.TEXT_MUTED);
        
        exportConfigButton.setPosition(x, y + 100);
        exportConfigButton.render(mouseX, mouseY);
        
        importConfigButton.setPosition(x + 200, y + 100);
        importConfigButton.render(mouseX, mouseY);
        
        // Schedule section
        GuiRenderUtils.drawText("§b§lZamanlama (Schedule)", x, y + 155, 0xFF00BCD4);
        GuiRenderUtils.drawText("§7Haftalık programı varsayılan olarak kaydet veya", x, y + 175, GuiTheme.TEXT_MUTED);
        GuiRenderUtils.drawText("§7varsayılan programı kendi hesabına aktar.", x, y + 187, GuiTheme.TEXT_MUTED);
        
        exportScheduleButton.setPosition(x, y + 205);
        exportScheduleButton.render(mouseX, mouseY);
        
        importScheduleButton.setPosition(x + 200, y + 205);
        importScheduleButton.render(mouseX, mouseY);
        
        // Status message
        if (System.currentTimeMillis() - configStatusTime < 3000 && !configStatusMessage.isEmpty()) {
            GuiRenderUtils.drawText(configStatusMessage, x, y + 260, GuiTheme.TEXT_PRIMARY);
        }
        
        // File info
        GuiRenderUtils.drawText("§8Dosya Yapısı:", x, y + 290, GuiTheme.TEXT_MUTED);
        GuiRenderUtils.drawText("§8BananaClient/default_config.cfg", x + 10, y + 305, GuiTheme.TEXT_MUTED);
        GuiRenderUtils.drawText("§8BananaClient/configs/" + playerName + ".cfg", x + 10, y + 318, GuiTheme.TEXT_MUTED);
    }
    
    private void drawSafeStateWarning() {
        IState state = MuzMod.instance.getStateManager().getCurrentState();
        if (state instanceof SafeState) {
            SafeState safe = (SafeState) state;
            int warningY = guiY + guiHeight - 75;
            GuiRenderUtils.drawRect(guiX + 10, warningY, guiWidth - 20, 18, GuiTheme.withAlpha(GuiTheme.DANGER, 0.8f));
            GuiRenderUtils.drawCenteredText("§c§l⚠ " + safe.getReason().getMessage(), 
                guiX + guiWidth / 2, warningY + 5, GuiTheme.TEXT_PRIMARY);
        }
    }
    
    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        
        // Tab bar
        tabBar.mouseClicked(mouseX, mouseY, mouseButton);
        
        // Settings sub-tab bar (only when on settings tab)
        if (currentTab == 2) {
            settingsSubTabBar.mouseClicked(mouseX, mouseY, mouseButton);
        }
        
        // General tab components
        if (currentTab == 0) {
            if (botToggle.mouseClicked(mouseX, mouseY, mouseButton)) {
                MuzMod.instance.setBotEnabled(botToggle.isEnabled());
            }
            if (scheduleToggle.mouseClicked(mouseX, mouseY, mouseButton)) {
                MuzMod.instance.getScheduleManager().setScheduleEnabled(scheduleToggle.isEnabled());
                MuzMod.instance.getStateManager().setUseScheduleBasedTransition(scheduleToggle.isEnabled());
            }
            if (hudToggle.mouseClicked(mouseX, mouseY, mouseButton)) {
                MuzMod.instance.getConfig().setShowOverlay(hudToggle.isEnabled());
            }
            if (autoAfkToggle.mouseClicked(mouseX, mouseY, mouseButton)) {
                MuzMod.instance.getScheduleManager().setAutoAfkWhenIdle(autoAfkToggle.isEnabled());
            }
            
            // State buttons
            if (idleButton.mouseClicked(mouseX, mouseY, mouseButton)) {
                MuzMod.instance.getStateManager().changeToState("idle");
            }
            if (miningButton.mouseClicked(mouseX, mouseY, mouseButton)) {
                MuzMod.instance.getStateManager().changeToState("mining");
            }
            if (afkButton.mouseClicked(mouseX, mouseY, mouseButton)) {
                MuzMod.instance.getStateManager().changeToState("afk");
            }
            if (repairButton.mouseClicked(mouseX, mouseY, mouseButton)) {
                MuzMod.instance.getStateManager().changeToState("repair");
            }
            if (oxButton.mouseClicked(mouseX, mouseY, mouseButton)) {
                MuzMod.instance.getStateManager().changeToState("ox");
            }
            if (obsidianButton.mouseClicked(mouseX, mouseY, mouseButton)) {
                MuzMod.instance.getStateManager().changeToState("obsidian");
            }
        }
        
        // Schedule tab components
        if (currentTab == 1) {
            handleScheduleClick(mouseX, mouseY, mouseButton);
        }
        
        // Settings tab components
        if (currentTab == 2) {
            handleSettingsClick(mouseX, mouseY, mouseButton);
        }
        
        // Save/Close buttons
        if (saveButton.mouseClicked(mouseX, mouseY, mouseButton)) {
            saveSettings();
        }
        if (closeButton.mouseClicked(mouseX, mouseY, mouseButton)) {
            closing = true;
        }
    }
    
    private void handleScheduleClick(int mouseX, int mouseY, int button) {
        if (button != 0) return;
        
        ScheduleManager schedule = MuzMod.instance.getScheduleManager();
        
        // Day selector
        int dayY = guiY + 95;
        int dayWidth = 70;
        for (int i = 0; i < 7; i++) {
            int dx = guiX + 20 + i * dayWidth;
            if (mouseX >= dx && mouseX < dx + dayWidth - 4 && mouseY >= dayY && mouseY < dayY + 24) {
                selectedDay = i;
                selectedEntryId = -1;
                scheduleScrollOffset = 0;
                return;
            }
        }
        
        // Entry list clicks
        int listX = guiX + 20;
        int listY = guiY + 130;
        int listW = 225;
        int itemH = 32;
        int visibleItems = 5;
        int itemY = listY + 28;
        
        List<ScheduleEntry> entries = schedule.getEntriesForDay(selectedDay);
        for (int i = scheduleScrollOffset; i < Math.min(entries.size(), scheduleScrollOffset + visibleItems); i++) {
            if (mouseX >= listX + 5 && mouseX < listX + listW - 5 && mouseY >= itemY && mouseY < itemY + itemH - 2) {
                ScheduleEntry entry = entries.get(i);
                selectedEntryId = entry.getId();
                addingNewEntry = false;
                
                // Load entry data into fields
                fieldStartTime.setText(entry.getStartTimeStr());
                fieldEndTime.setText(entry.getEndTimeStr());
                newEntryType = entry.getEventType().ordinal();
                String warp = entry.getCustomWarpCommand();
                fieldWarpCommand.setText(warp != null ? warp : "");
                return;
            }
            itemY += itemH;
        }
        
        // Right panel clicks
        int panelX = guiX + 260;
        int panelY = guiY + 130;
        
        if (addingNewEntry || selectedEntryId != -1) {
            // Text field clicks
            fieldStartTime.mouseClicked(mouseX, mouseY, button);
            fieldEndTime.mouseClicked(mouseX, mouseY, button);
            fieldWarpCommand.mouseClicked(mouseX, mouseY, button);
            
            // Event type selector
            for (int i = 0; i < 4; i++) {
                int tx = panelX + 10 + i * 46;
                if (mouseX >= tx && mouseX < tx + 44 && mouseY >= panelY + 100 && mouseY < panelY + 118) {
                    newEntryType = i;
                    return;
                }
            }
            
            // Action buttons
            if (addingNewEntry) {
                if (addEntryButton.mouseClicked(mouseX, mouseY, button)) {
                    addNewScheduleEntry();
                }
                if (cancelEditButton.mouseClicked(mouseX, mouseY, button)) {
                    addingNewEntry = false;
                    resetScheduleFields();
                }
            } else {
                if (editSaveButton.mouseClicked(mouseX, mouseY, button)) {
                    saveScheduleEntry();
                }
                if (deleteEntryButton.mouseClicked(mouseX, mouseY, button)) {
                    deleteScheduleEntry();
                }
                if (cancelEditButton.mouseClicked(mouseX, mouseY, button)) {
                    selectedEntryId = -1;
                    resetScheduleFields();
                }
            }
        } else {
            // New entry button
            if (newEntryButton.mouseClicked(mouseX, mouseY, button)) {
                addingNewEntry = true;
                selectedEntryId = -1;
                resetScheduleFields();
            }
        }
    }
    
    private void addNewScheduleEntry() {
        ScheduleManager schedule = MuzMod.instance.getScheduleManager();
        
        String startTime = fieldStartTime.getText().trim();
        String endTime = fieldEndTime.getText().trim();
        String warpCmd = fieldWarpCommand.getText().trim();
        
        if (!isValidTime(startTime) || !isValidTime(endTime)) {
            MuzMod.instance.sendChat("§c§lHata: §7Geçersiz saat formatı! (HH:MM veya HH:MM:SS)");
            return;
        }
        
        ScheduleEntry.EventType type = ScheduleEntry.EventType.values()[newEntryType];
        
        int[] start = parseTime(startTime);
        int[] end = parseTime(endTime);
        
        ScheduleEntry entry = new ScheduleEntry(selectedDay, start[0], start[1], start[2], end[0], end[1], end[2], type);
        if (!warpCmd.isEmpty()) {
            entry.setCustomWarpCommand(warpCmd);
        }
        
        schedule.addEntry(entry);
        schedule.saveSchedule();
        
        addingNewEntry = false;
        resetScheduleFields();
        MuzMod.instance.sendChat("§a§lEtkinlik eklendi!");
    }
    
    private void saveScheduleEntry() {
        ScheduleManager schedule = MuzMod.instance.getScheduleManager();
        ScheduleEntry entry = schedule.getEntryById(selectedEntryId);
        
        if (entry == null) return;
        
        String startTime = fieldStartTime.getText().trim();
        String endTime = fieldEndTime.getText().trim();
        String warpCmd = fieldWarpCommand.getText().trim();
        
        if (!isValidTime(startTime) || !isValidTime(endTime)) {
            MuzMod.instance.sendChat("§c§lHata: §7Geçersiz saat formatı! (HH:MM veya HH:MM:SS)");
            return;
        }
        
        int[] start = parseTime(startTime);
        int[] end = parseTime(endTime);
        
        entry.setStartHour(start[0]);
        entry.setStartMinute(start[1]);
        entry.setStartSecond(start[2]);
        entry.setEndHour(end[0]);
        entry.setEndMinute(end[1]);
        entry.setEndSecond(end[2]);
        entry.setEventType(ScheduleEntry.EventType.values()[newEntryType]);
        entry.setCustomWarpCommand(warpCmd.isEmpty() ? null : warpCmd);
        
        schedule.saveSchedule();
        selectedEntryId = -1;
        resetScheduleFields();
        MuzMod.instance.sendChat("§a§lEtkinlik güncellendi!");
    }
    
    private void deleteScheduleEntry() {
        ScheduleManager schedule = MuzMod.instance.getScheduleManager();
        schedule.removeEntry(selectedEntryId);
        schedule.saveSchedule();
        selectedEntryId = -1;
        resetScheduleFields();
        MuzMod.instance.sendChat("§c§lEtkinlik silindi!");
    }
    
    private void resetScheduleFields() {
        fieldStartTime.setText("10:00");
        fieldEndTime.setText("12:00");
        fieldWarpCommand.setText("");
        newEntryType = 0;
    }
    
    private boolean isValidTime(String time) {
        if (time == null || !time.contains(":")) return false;
        String[] parts = time.split(":");
        // HH:MM veya HH:MM:SS formatını kabul et
        if (parts.length != 2 && parts.length != 3) return false;
        try {
            int hour = Integer.parseInt(parts[0]);
            int min = Integer.parseInt(parts[1]);
            int sec = parts.length == 3 ? Integer.parseInt(parts[2]) : 0;
            return hour >= 0 && hour <= 23 && min >= 0 && min <= 59 && sec >= 0 && sec <= 59;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    // HH:MM veya HH:MM:SS formatından saat, dakika, saniye çıkar
    private int[] parseTime(String time) {
        String[] parts = time.split(":");
        int hour = Integer.parseInt(parts[0]);
        int min = Integer.parseInt(parts[1]);
        int sec = parts.length == 3 ? Integer.parseInt(parts[2]) : 0;
        return new int[]{hour, min, sec};
    }
    
    private void handleSettingsClick(int mouseX, int mouseY, int button) {
        ModConfig config = MuzMod.instance.getConfig();
        
        switch (settingsSubTab) {
            case 0: // General
                defaultMiningWarpField.mouseClicked(mouseX, mouseY, button);
                defaultAfkWarpField.mouseClicked(mouseX, mouseY, button);
                discordWebhookField.mouseClicked(mouseX, mouseY, button);
                repairThresholdSlider.mouseClicked(mouseX, mouseY, button);
                timeOffsetSlider.mouseClicked(mouseX, mouseY, button);
                detectRadiusSlider.mouseClicked(mouseX, mouseY, button);
                repairClickDelaySlider.mouseClicked(mouseX, mouseY, button);
                break;
                
            case 1: // Mining
                initialWalkMinSlider.mouseClicked(mouseX, mouseY, button);
                initialWalkMaxSlider.mouseClicked(mouseX, mouseY, button);
                walkYawVariationSlider.mouseClicked(mouseX, mouseY, button);
                maxDistFromCenterSlider.mouseClicked(mouseX, mouseY, button);
                if (secondWalkToggle.mouseClicked(mouseX, mouseY, button)) {
                    config.setSecondWalkEnabled(secondWalkToggle.isEnabled());
                }
                secondWalkMinSlider.mouseClicked(mouseX, mouseY, button);
                secondWalkMaxSlider.mouseClicked(mouseX, mouseY, button);
                secondWalkAngleSlider.mouseClicked(mouseX, mouseY, button);
                if (strafeToggle.mouseClicked(mouseX, mouseY, button)) {
                    config.setStrafeEnabled(strafeToggle.isEnabled());
                }
                strafeIntervalSlider.mouseClicked(mouseX, mouseY, button);
                strafeDurationSlider.mouseClicked(mouseX, mouseY, button);
                if (miningJitterToggle.mouseClicked(mouseX, mouseY, button)) {
                    config.setMiningJitterEnabled(miningJitterToggle.isEnabled());
                }
                miningJitterYawSlider.mouseClicked(mouseX, mouseY, button);
                miningJitterPitchSlider.mouseClicked(mouseX, mouseY, button);
                miningJitterIntervalSlider.mouseClicked(mouseX, mouseY, button);
                if (instantFleeToggle.mouseClicked(mouseX, mouseY, button)) {
                    config.setInstantFlee(instantFleeToggle.isEnabled());
                }
                break;
                
            case 2: // OX
                oxMinPlayersSlider.mouseClicked(mouseX, mouseY, button);
                break;
                
            case 3: // Obsidian
                if (obsidianJitterToggle.mouseClicked(mouseX, mouseY, button)) {
                    config.setObsidianJitterEnabled(obsidianJitterToggle.isEnabled());
                }
                obsidianJitterYawSlider.mouseClicked(mouseX, mouseY, button);
                obsidianJitterPitchSlider.mouseClicked(mouseX, mouseY, button);
                obsidianJitterIntervalSlider.mouseClicked(mouseX, mouseY, button);
                obsidianAimSpeedSlider.mouseClicked(mouseX, mouseY, button);
                obsidianTurnSpeedSlider.mouseClicked(mouseX, mouseY, button);
                if (obsidianSellToggle.mouseClicked(mouseX, mouseY, button)) {
                    config.setObsidianSellEnabled(obsidianSellToggle.isEnabled());
                }
                if (obsidianPlayerDetectionToggle.mouseClicked(mouseX, mouseY, button)) {
                    config.setObsidianPlayerDetectionEnabled(obsidianPlayerDetectionToggle.isEnabled());
                }
                obsidianSellCommandField.mouseClicked(mouseX, mouseY, button);
                obsidianSellDelaySlider.mouseClicked(mouseX, mouseY, button);
                obsidianTargetMinSlider.mouseClicked(mouseX, mouseY, button);
                obsidianTargetMaxSlider.mouseClicked(mouseX, mouseY, button);
                break;
                
            case 4: // Duel
                if (duelHudToggle.mouseClicked(mouseX, mouseY, button)) {
                    config.setDuelHudEnabled(duelHudToggle.isEnabled());
                }
                duelPlayer1Field.mouseClicked(mouseX, mouseY, button);
                duelPlayer2Field.mouseClicked(mouseX, mouseY, button);
                
                if (startAnalysisButton.mouseClicked(mouseX, mouseY, button)) {
                    startDuelAnalysis();
                }
                if (stopAnalysisButton.mouseClicked(mouseX, mouseY, button)) {
                    MuzMod.instance.getDuelAnalyzerState().stopAnalysis();
                }
                break;
                
            case 5: // Config
                if (exportConfigButton.mouseClicked(mouseX, mouseY, button)) {
                    if (MuzMod.instance.exportConfigToDefault()) {
                        configStatusMessage = "§a✓ Config varsayılan olarak kaydedildi!";
                    } else {
                        configStatusMessage = "§c✗ Config kaydedilemedi!";
                    }
                    configStatusTime = System.currentTimeMillis();
                }
                if (importConfigButton.mouseClicked(mouseX, mouseY, button)) {
                    if (MuzMod.instance.importConfigFromDefault()) {
                        configStatusMessage = "§a✓ Varsayılan config yüklendi!";
                        // GUI'yi güncelle
                        loadCurrentValues();
                    } else {
                        configStatusMessage = "§c✗ Config yüklenemedi!";
                    }
                    configStatusTime = System.currentTimeMillis();
                }
                if (exportScheduleButton.mouseClicked(mouseX, mouseY, button)) {
                    if (MuzMod.instance.exportScheduleToDefault()) {
                        configStatusMessage = "§a✓ Schedule varsayılan olarak kaydedildi!";
                    } else {
                        configStatusMessage = "§c✗ Schedule kaydedilemedi!";
                    }
                    configStatusTime = System.currentTimeMillis();
                }
                if (importScheduleButton.mouseClicked(mouseX, mouseY, button)) {
                    if (MuzMod.instance.importScheduleFromDefault()) {
                        configStatusMessage = "§a✓ Varsayılan schedule yüklendi!";
                    } else {
                        configStatusMessage = "§c✗ Schedule yüklenemedi!";
                    }
                    configStatusTime = System.currentTimeMillis();
                }
                break;
        }
    }
    
    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
        
        // Release all sliders
        for (ModernSlider slider : sliders) {
            slider.mouseReleased(mouseX, mouseY, state);
        }
        
        // Release all buttons
        for (ModernButton button : buttons) {
            button.mouseReleased(mouseX, mouseY, state);
        }
    }
    
    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        // ESC to close
        if (keyCode == Keyboard.KEY_ESCAPE) {
            closing = true;
            return;
        }
        
        // Handle text field input
        for (ModernTextField field : textFields) {
            if (field.keyTyped(typedChar, keyCode)) {
                return;
            }
        }
        
        super.keyTyped(typedChar, keyCode);
    }
    
    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        
        // Handle scroll
        int scroll = Mouse.getEventDWheel();
        if (scroll != 0) {
            int mouseX = Mouse.getEventX() * width / mc.displayWidth;
            int mouseY = height - Mouse.getEventY() * height / mc.displayHeight - 1;
            
            // Schedule tab scroll
            if (currentTab == 1) {
                int listX = guiX + 20;
                int listY = guiY + 130;
                int listW = 225;
                int listH = 200;
                
                if (mouseX >= listX && mouseX < listX + listW && mouseY >= listY && mouseY < listY + listH) {
                    ScheduleManager schedule = MuzMod.instance.getScheduleManager();
                    List<ScheduleEntry> entries = schedule.getEntriesForDay(selectedDay);
                    int maxScroll = Math.max(0, entries.size() - 5);
                    
                    if (scroll > 0) {
                        scheduleScrollOffset = Math.max(0, scheduleScrollOffset - 1);
                    } else {
                        scheduleScrollOffset = Math.min(maxScroll, scheduleScrollOffset + 1);
                    }
                }
            }
            
            // Settings tab scroll
            if (currentTab == 2) {
                int contentX = guiX + 30;
                int contentY = guiY + 135;
                int contentW = guiWidth - 60;
                int contentH = guiHeight - 200;
                
                if (mouseX >= contentX && mouseX < contentX + contentW && mouseY >= contentY && mouseY < contentY + contentH) {
                    int scrollAmount = scroll > 0 ? -30 : 30;
                    
                    switch (settingsSubTab) {
                        case 1: // Mining
                            miningScrollOffset = Math.max(0, Math.min(200, miningScrollOffset + scrollAmount));
                            break;
                        case 3: // Obsidian
                            obsidianScrollOffset = Math.max(0, Math.min(100, obsidianScrollOffset + scrollAmount));
                            break;
                        case 5: // Config
                            configScrollOffset = Math.max(0, Math.min(100, configScrollOffset + scrollAmount));
                            break;
                    }
                }
            }
        }
    }
    
    private void saveSettings() {
        ModConfig config = MuzMod.instance.getConfig();
        ScheduleManager schedule = MuzMod.instance.getScheduleManager();
        
        // General settings
        schedule.setDefaultMiningWarp(defaultMiningWarpField.getText());
        schedule.setDefaultAfkWarp(defaultAfkWarpField.getText());
        config.setDiscordWebhookUrl(discordWebhookField.getText());
        config.setRepairDurabilityThreshold(repairThresholdSlider.getIntValue());
        config.setTimeOffsetHours(timeOffsetSlider.getIntValue());
        config.setPlayerDetectionRadius(detectRadiusSlider.getValue());
        config.setRepairClickDelay((float)repairClickDelaySlider.getValue());
        
        // Mining settings
        config.setInitialWalkDistanceMin(initialWalkMinSlider.getIntValue());
        config.setInitialWalkDistanceMax(initialWalkMaxSlider.getIntValue());
        config.setWalkYawVariation(walkYawVariationSlider.getIntValue());
        config.setMaxDistanceFromCenter(maxDistFromCenterSlider.getIntValue());
        config.setSecondWalkEnabled(secondWalkToggle.isEnabled());
        config.setSecondWalkDistanceMin(secondWalkMinSlider.getIntValue());
        config.setSecondWalkDistanceMax(secondWalkMaxSlider.getIntValue());
        config.setSecondWalkAngleVariation(secondWalkAngleSlider.getIntValue());
        config.setStrafeEnabled(strafeToggle.isEnabled());
        config.setStrafeInterval(strafeIntervalSlider.getIntValue() * 1000); // sn -> ms
        config.setStrafeDuration(strafeDurationSlider.getIntValue());
        config.setMiningJitterEnabled(miningJitterToggle.isEnabled());
        config.setMiningJitterYaw((float)miningJitterYawSlider.getValue());
        config.setMiningJitterPitch((float)miningJitterPitchSlider.getValue());
        config.setMiningJitterInterval((int)miningJitterIntervalSlider.getValue());
        config.setInstantFlee(instantFleeToggle.isEnabled());
        
        // OX settings
        config.setOxMinPlayers(oxMinPlayersSlider.getIntValue());
        
        // Obsidian settings
        config.setObsidianJitterEnabled(obsidianJitterToggle.isEnabled());
        config.setObsidianJitterYaw((float)obsidianJitterYawSlider.getValue());
        config.setObsidianJitterPitch((float)obsidianJitterPitchSlider.getValue());
        config.setObsidianJitterInterval((int)obsidianJitterIntervalSlider.getValue());
        config.setObsidianAimSpeed((float)obsidianAimSpeedSlider.getValue());
        config.setObsidianTurnSpeed((float)obsidianTurnSpeedSlider.getValue());
        config.setObsidianSellEnabled(obsidianSellToggle.isEnabled());
        config.setObsidianPlayerDetectionEnabled(obsidianPlayerDetectionToggle.isEnabled());
        config.setObsidianSellCommand(obsidianSellCommandField.getText());
        config.setObsidianSellDelay((int)(obsidianSellDelaySlider.getValue() * 1000));
        config.setObsidianTargetMinOffset(obsidianTargetMinSlider.getIntValue());
        config.setObsidianTargetMaxOffset(obsidianTargetMaxSlider.getIntValue());
        
        // Duel settings
        config.setDuelHudEnabled(duelHudToggle.isEnabled());
        
        // Save to file
        config.saveConfig();
        schedule.saveSchedule();
        
        // Show feedback
        MuzMod.instance.sendChat("§a§lAyarlar kaydedildi!");
    }
    
    private void startDuelAnalysis() {
        String player1 = duelPlayer1Field.getText().trim();
        String player2 = duelPlayer2Field.getText().trim();
        
        if (player1.isEmpty() || player2.isEmpty()) {
            MuzMod.instance.sendChat("§c§lHata: §7Her iki oyuncu adını da girmelisiniz!");
            return;
        }
        
        MuzMod.instance.getDuelAnalyzerState().startAnalysis(player1, player2);
        MuzMod.instance.sendChat("§a§lDuel analizi başlatıldı! §7" + player1 + " vs " + player2);
    }
    
    /**
     * Config import sonrası GUI değerlerini güncelle
     */
    private void loadCurrentValues() {
        ModConfig config = MuzMod.instance.getConfig();
        ScheduleManager schedule = MuzMod.instance.getScheduleManager();
        
        // General settings
        defaultMiningWarpField.setText(schedule.getDefaultMiningWarp());
        defaultAfkWarpField.setText(schedule.getDefaultAfkWarp());
        discordWebhookField.setText(config.getDiscordWebhookUrl());
        repairThresholdSlider.setValue(config.getRepairDurabilityThreshold());
        timeOffsetSlider.setValue(config.getTimeOffsetHours());
        detectRadiusSlider.setValue(config.getPlayerDetectionRadius());
        repairClickDelaySlider.setValue(config.getRepairClickDelay());
        
        // Mining settings
        initialWalkMinSlider.setValue(config.getInitialWalkDistanceMin());
        initialWalkMaxSlider.setValue(config.getInitialWalkDistanceMax());
        walkYawVariationSlider.setValue(config.getWalkYawVariation());
        maxDistFromCenterSlider.setValue(config.getMaxDistanceFromCenter());
        secondWalkToggle.setEnabled(config.isSecondWalkEnabled());
        secondWalkMinSlider.setValue(config.getSecondWalkDistanceMin());
        secondWalkMaxSlider.setValue(config.getSecondWalkDistanceMax());
        secondWalkAngleSlider.setValue(config.getSecondWalkAngleVariation());
        strafeToggle.setEnabled(config.isStrafeEnabled());
        strafeIntervalSlider.setValue(config.getStrafeInterval() / 1000.0);
        strafeDurationSlider.setValue(config.getStrafeDuration());
        miningJitterToggle.setEnabled(config.isMiningJitterEnabled());
        miningJitterYawSlider.setValue(config.getMiningJitterYaw());
        miningJitterPitchSlider.setValue(config.getMiningJitterPitch());
        miningJitterIntervalSlider.setValue(config.getMiningJitterInterval());
        instantFleeToggle.setEnabled(config.isInstantFlee());
        
        // OX settings
        oxMinPlayersSlider.setValue(config.getOxMinPlayers());
        
        // Obsidian settings
        obsidianJitterToggle.setEnabled(config.isObsidianJitterEnabled());
        obsidianJitterYawSlider.setValue(config.getObsidianJitterYaw());
        obsidianJitterPitchSlider.setValue(config.getObsidianJitterPitch());
        obsidianJitterIntervalSlider.setValue(config.getObsidianJitterInterval());
        obsidianAimSpeedSlider.setValue(config.getObsidianAimSpeed());
        obsidianTurnSpeedSlider.setValue(config.getObsidianTurnSpeed());
        obsidianSellToggle.setEnabled(config.isObsidianSellEnabled());
        obsidianPlayerDetectionToggle.setEnabled(config.isObsidianPlayerDetectionEnabled());
        obsidianSellCommandField.setText(config.getObsidianSellCommand());
        obsidianSellDelaySlider.setValue(config.getObsidianSellDelay() / 1000.0);
        obsidianTargetMinSlider.setValue(config.getObsidianTargetMinOffset());
        obsidianTargetMaxSlider.setValue(config.getObsidianTargetMaxOffset());
        
        // Duel settings
        duelHudToggle.setEnabled(config.isDuelHudEnabled());
        
        // Toggles
        botToggle.setEnabled(MuzMod.instance.isBotEnabled());
        scheduleToggle.setEnabled(schedule.isScheduleEnabled());
        hudToggle.setEnabled(config.isShowOverlay());
        autoAfkToggle.setEnabled(schedule.isAutoAfkWhenIdle());
    }
    
    private String truncate(String text, int maxLen) {
        if (text.length() <= maxLen) return text;
        return text.substring(0, maxLen - 3) + "...";
    }
    
    private String formatDuration(long ms) {
        long seconds = ms / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }
    
    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
    
    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }
}
