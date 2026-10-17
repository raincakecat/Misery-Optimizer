package dev.tung.miseryoptimizer;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

final class MiseryOptimizerConfigScreen extends Screen {
    private final Screen parent;
    private final MiseryOptimizerConfig config;

    MiseryOptimizerConfigScreen(Screen parent, MiseryOptimizerConfig config) {
        super(Component.literal("Misery Optimizer"));
        this.parent = parent;
        this.config = config;
    }

    @Override
    protected void init() {
        int x = width / 2 - 110;
        int y = Math.max(16, height / 2 - 110);
        addRenderableWidget(Button.builder(label("Crystal hit", config.crystals), button -> {
            config.crystals = !config.crystals;
            button.setMessage(label("Crystal hit", config.crystals));
            config.save();
        }).bounds(x, y, 108, 20).build());
        addRenderableWidget(Button.builder(label("Crystal FX", config.crystalEffects), button -> {
            config.crystalEffects = !config.crystalEffects;
            button.setMessage(label("Crystal FX", config.crystalEffects));
            config.save();
        }).bounds(x + 112, y, 108, 20).build());
        addRenderableWidget(Button.builder(label("Crystal placement", config.crystalPlacement), button -> {
            config.crystalPlacement = !config.crystalPlacement;
            if (!config.crystalPlacement) GhostCrystal.clear();
            button.setMessage(label("Crystal placement", config.crystalPlacement));
            config.save();
        }).bounds(x, y + 22, 220, 20).build());
        addRenderableWidget(Button.builder(label("Anchor feedback", config.anchors), button -> {
            config.anchors = !config.anchors;
            button.setMessage(label("Anchor feedback", config.anchors));
            config.save();
        }).bounds(x, y + 44, 220, 20).build());
        addRenderableWidget(Button.builder(label("Pearl prediction", config.pearls), button -> {
            config.pearls = !config.pearls;
            if (!config.pearls) GhostPearl.clear();
            button.setMessage(label("Pearl prediction", config.pearls));
            config.save();
        }).bounds(x, y + 66, 220, 20).build());
        addRenderableWidget(Button.builder(label("Terrain preview", config.terrainPreview), button -> {
            config.terrainPreview = !config.terrainPreview;
            if (!config.terrainPreview) ExplosionPreview.restoreAll();
            button.setMessage(label("Terrain preview", config.terrainPreview));
            config.save();
        }).bounds(x, y + 88, 108, 20).build());
        addRenderableWidget(Button.builder(label("Safe outlines", config.safeTerrainPreview), button -> {
            config.safeTerrainPreview = !config.safeTerrainPreview;
            ExplosionPreview.restoreAll();
            button.setMessage(label("Safe outlines", config.safeTerrainPreview));
            config.save();
        }).bounds(x + 112, y + 88, 108, 20).build());
        addRenderableWidget(Button.builder(label("Early slot", config.slotSync), button -> {
            config.slotSync = !config.slotSync;
            button.setMessage(label("Early slot", config.slotSync));
            config.save();
        }).bounds(x, y + 110, 108, 20).build());
        addRenderableWidget(Button.builder(label("Block fix", config.blockCorrections), button -> {
            config.blockCorrections = !config.blockCorrections;
            if (!config.blockCorrections) BlockActionPreview.clear();
            button.setMessage(label("Block fix", config.blockCorrections));
            config.save();
        }).bounds(x + 112, y + 110, 108, 20).build());
        addRenderableWidget(Button.builder(blockLimitLabel(), button -> {
            config.maxPreviewBlocks = switch (config.maxPreviewBlocks) {
                case 128 -> 256;
                case 256 -> 512;
                case 512 -> 768;
                default -> 128;
            };
            button.setMessage(blockLimitLabel());
            config.save();
        }).bounds(x, y + 132, 108, 20).build());
        addRenderableWidget(Button.builder(label("Eat timing", config.foodTiming), button -> {
            config.foodTiming = !config.foodTiming;
            if (!config.foodTiming) FoodUsePreview.clear();
            button.setMessage(label("Eat timing", config.foodTiming));
            config.save();
        }).bounds(x + 112, y + 132, 108, 20).build());
        addRenderableWidget(Button.builder(graceLabel(), button -> {
            config.extraGraceMillis = switch (config.extraGraceMillis) {
                case 0 -> 500;
                case 500 -> 1000;
                case 1000 -> 2000;
                default -> 0;
            };
            button.setMessage(graceLabel());
            config.save();
        }).bounds(x, y + 154, 220, 20).build());
        addRenderableWidget(Button.builder(pearlHandoffLabel(), button -> {
            config.pearlHandoffTicks = switch (config.pearlHandoffTicks) {
                case 0 -> 3;
                case 3 -> 6;
                case 6 -> 9;
                default -> 0;
            };
            button.setMessage(pearlHandoffLabel());
            config.save();
        }).bounds(x, y + 176, 220, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Performance..."), button ->
            minecraft.gui.setScreen(new MiseryOptimizerPerformanceScreen(this, config)))
            .bounds(x, y + 202, 108, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Done"), button -> onClose())
            .bounds(x + 112, y + 202, 108, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        graphics.centeredText(font, title, width / 2, Math.max(6, height / 2 - 132), 0xFFFFFF);
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }

    private static Component label(String name, boolean enabled) {
        return Component.literal(name + ": " + (enabled ? "On" : "Off"));
    }

    private Component blockLimitLabel() {
        return Component.literal("Block cap: " + config.maxPreviewBlocks);
    }

    private Component graceLabel() {
        return Component.literal("Extra prediction time: " + config.extraGraceMillis + " ms");
    }

    private Component pearlHandoffLabel() {
        return Component.literal("Pearl handoff: " + config.pearlHandoffTicks + " ticks");
    }
}
