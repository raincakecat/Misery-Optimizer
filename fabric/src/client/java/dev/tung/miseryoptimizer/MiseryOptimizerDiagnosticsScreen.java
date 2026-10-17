package dev.tung.miseryoptimizer;

import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

final class MiseryOptimizerDiagnosticsScreen extends Screen {
    private final Screen parent;

    MiseryOptimizerDiagnosticsScreen(Screen parent) {
        super(Component.literal("Misery Optimizer Diagnostics"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(Component.literal("Back"), button -> onClose())
            .bounds(width / 2 - 110, Math.max(20, height / 2 + 72), 220, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);
        int y = Math.max(10, height / 2 - 88);
        line(graphics, title.getString(), y, 0xFFFFFF);
        line(graphics, "FPS: " + ClientDiagnostics.fps() + "  Frame: "
            + String.format(Locale.ROOT, "%.1f ms", ClientDiagnostics.frameMillis()), y + 20, 0xDDDDDD);
        line(graphics, "Preview slice: "
            + String.format(Locale.ROOT, "%.2f ms", ClientDiagnostics.scanMillis())
            + "  Slices: " + ClientDiagnostics.scans(), y + 36, 0xDDDDDD);
        line(graphics, "Confirmed predictions: " + ClientDiagnostics.TIMING.confirmed(), y + 52, 0xDDDDDD);
        line(graphics, "Corrections: " + ClientDiagnostics.corrections(), y + 68, 0xDDDDDD);
        line(graphics, "Effects shown / skipped: " + ClientDiagnostics.effectsShown()
            + " / " + ClientDiagnostics.effectsSkipped(), y + 84, 0xDDDDDD);
        line(graphics, "Observed response: " + ClientDiagnostics.TIMING.observedMillis()
            + " ms (" + ClientDiagnostics.TIMING.samples() + " samples)", y + 100, 0xDDDDDD);
    }

    private void line(GuiGraphics graphics, String value, int y, int color) {
        graphics.drawCenteredString(font, value, width / 2, y, color);
    }

    @Override
    public void onClose() { minecraft.setScreen(parent); }
}
