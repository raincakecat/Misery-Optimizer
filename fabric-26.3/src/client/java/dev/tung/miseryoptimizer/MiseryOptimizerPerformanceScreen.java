package dev.tung.miseryoptimizer;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

final class MiseryOptimizerPerformanceScreen extends Screen {
    private final Screen parent;
    private final MiseryOptimizerConfig config;
    private Button diagnosticsButton;

    MiseryOptimizerPerformanceScreen(Screen parent, MiseryOptimizerConfig config) {
        super(Component.literal("Misery Optimizer Performance"));
        this.parent = parent;
        this.config = config;
    }

    @Override
    protected void init() {
        int x = width / 2 - 110;
        int y = Math.max(20, height / 2 - 80);
        addRenderableWidget(Button.builder(label("Adaptive effects", config.adaptiveEffects), button -> {
            config.adaptiveEffects = !config.adaptiveEffects;
            button.setMessage(label("Adaptive effects", config.adaptiveEffects));
            config.save();
        }).bounds(x, y, 220, 20).build());
        addRenderableWidget(Button.builder(label("Preview frame budget", config.previewFrameBudget), button -> {
            config.previewFrameBudget = !config.previewFrameBudget;
            ExplosionPreview.restoreAll();
            button.setMessage(label("Preview frame budget", config.previewFrameBudget));
            config.save();
        }).bounds(x, y + 24, 220, 20).build());
        addRenderableWidget(Button.builder(label("Adaptive prediction timing", config.adaptivePredictionTiming), button -> {
            config.adaptivePredictionTiming = !config.adaptivePredictionTiming;
            button.setMessage(label("Adaptive prediction timing", config.adaptivePredictionTiming));
            config.save();
        }).bounds(x, y + 48, 220, 20).build());
        addRenderableWidget(Button.builder(label("Diagnostics", config.diagnosticsEnabled), button -> {
            config.diagnosticsEnabled = !config.diagnosticsEnabled;
            button.setMessage(label("Diagnostics", config.diagnosticsEnabled));
            config.save();
            diagnosticsButton.active = config.diagnosticsEnabled;
        }).bounds(x, y + 72, 220, 20).build());
        diagnosticsButton = Button.builder(Component.literal("View diagnostics"), button ->
            minecraft.gui.setScreen(new MiseryOptimizerDiagnosticsScreen(this)))
            .bounds(x, y + 100, 220, 20).build();
        diagnosticsButton.active = config.diagnosticsEnabled;
        addRenderableWidget(diagnosticsButton);
        addRenderableWidget(Button.builder(Component.literal("Back"), button -> onClose())
            .bounds(x, y + 128, 220, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        graphics.centeredText(font, title, width / 2, Math.max(6, height / 2 - 108), 0xFFFFFF);
    }

    @Override
    public void onClose() { minecraft.gui.setScreen(parent); }

    private static Component label(String name, boolean enabled) {
        return Component.literal(name + ": " + (enabled ? "On" : "Off"));
    }
}
