package dev.tung.miseryoptimizer;

import net.minecraft.world.phys.Vec3;

/** Lightweight measurements used by the optional diagnostics screen and adaptive effects. */
final class ClientDiagnostics {
    static final PredictionTiming TIMING = new PredictionTiming(() -> System.nanoTime() / 1_000_000L);
    private static long lastFrameNanos;
    private static double frameMillis;
    private static long lastEffectNanos;
    private static Vec3 lastEffectCenter;
    private static long effectsShown;
    private static long effectsSkipped;
    private static long terrainCorrections;
    private static long scans;
    private static double scanMillis;

    private ClientDiagnostics() {}

    static void frame() {
        long now = System.nanoTime();
        if (lastFrameNanos != 0) {
            double interval = (now - lastFrameNanos) / 1_000_000.0;
            if (interval > 0 && interval < 1_000) {
                frameMillis = frameMillis == 0 ? interval : frameMillis * 0.9 + interval * 0.1;
            }
        }
        lastFrameNanos = now;
    }

    static boolean showEffect(Vec3 center, boolean adaptive) {
        long now = System.nanoTime();
        long interval = frameMillis > 33 ? 150_000_000L : 75_000_000L;
        if (adaptive && lastEffectCenter != null && now - lastEffectNanos < interval
            && lastEffectCenter.distanceToSqr(center) < 36.0) {
            effectsSkipped++;
            return false;
        }
        lastEffectNanos = now;
        lastEffectCenter = center;
        effectsShown++;
        return true;
    }

    static void scan(long elapsedNanos) {
        double elapsed = elapsedNanos / 1_000_000.0;
        scanMillis = scans == 0 ? elapsed : scanMillis * 0.9 + elapsed * 0.1;
        scans++;
    }

    static void terrainCorrection() { terrainCorrections++; }
    static int fps() { return frameMillis > 0 ? (int) Math.round(1_000.0 / frameMillis) : 0; }
    static double frameMillis() { return frameMillis; }
    static double scanMillis() { return scanMillis; }
    static long scans() { return scans; }
    static long effectsShown() { return effectsShown; }
    static long effectsSkipped() { return effectsSkipped; }
    static long corrections() { return terrainCorrections + TIMING.corrected(); }

    static void clear() {
        TIMING.clear();
        lastFrameNanos = 0;
        frameMillis = 0;
        lastEffectNanos = 0;
        lastEffectCenter = null;
        effectsShown = 0;
        effectsSkipped = 0;
        terrainCorrections = 0;
        scans = 0;
        scanMillis = 0;
    }
}
