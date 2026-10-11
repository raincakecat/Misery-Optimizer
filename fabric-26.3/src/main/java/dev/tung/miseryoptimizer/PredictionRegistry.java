package dev.tung.miseryoptimizer;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.LongSupplier;

/** Short-lived visual predictions. The server remains authoritative. */
public final class PredictionRegistry {
    private final LongSupplier clock;
    private final Map<Integer, Long> crystals = new LinkedHashMap<>();
    private final Map<Long, Long> anchors = new LinkedHashMap<>();

    public PredictionRegistry(LongSupplier clock) {
        this.clock = clock;
    }

    public static long timeoutMillis(int pingMillis) {
        return Math.min(6_000L, Math.max(900L, Math.max(0L, pingMillis) * 3L + 500L));
    }

    public void predictCrystal(int entityId, int pingMillis) {
        predictCrystal(entityId, pingMillis, 0);
    }

    public void predictCrystal(int entityId, int pingMillis, int extraGraceMillis) {
        expire();
        putBounded(crystals, entityId, clock.getAsLong() + timeoutMillis(pingMillis, extraGraceMillis), 256);
    }

    public void predictAnchor(long blockPos, int pingMillis) {
        predictAnchor(blockPos, pingMillis, 0);
    }

    public void predictAnchor(long blockPos, int pingMillis, int extraGraceMillis) {
        expire();
        putBounded(anchors, blockPos, clock.getAsLong() + timeoutMillis(pingMillis, extraGraceMillis), 128);
    }

    public static long timeoutMillis(int pingMillis, int extraGraceMillis) {
        return Math.min(8_000L, timeoutMillis(pingMillis) + Math.max(0, extraGraceMillis));
    }

    public boolean isCrystalPending(int entityId) {
        return isPending(crystals, entityId);
    }

    public boolean isAnchorPending(long blockPos) {
        return isPending(anchors, blockPos);
    }

    public void confirmCrystal(int entityId) {
        crystals.remove(entityId);
    }

    public void confirmAnchor(long blockPos) {
        anchors.remove(blockPos);
    }

    public void expire() {
        long now = clock.getAsLong();
        crystals.values().removeIf(deadline -> deadline <= now);
        anchors.values().removeIf(deadline -> deadline <= now);
    }

    public void clear() {
        crystals.clear();
        anchors.clear();
    }

    private <K> boolean isPending(Map<K, Long> values, K key) {
        Long deadline = values.get(key);
        if (deadline == null) return false;
        if (deadline <= clock.getAsLong()) {
            values.remove(key);
            return false;
        }
        return true;
    }

    private static <K> void putBounded(Map<K, Long> values, K key, long deadline, int maximum) {
        values.remove(key);
        if (values.size() >= maximum) {
            Iterator<K> oldest = values.keySet().iterator();
            oldest.next();
            oldest.remove();
        }
        values.put(key, deadline);
    }
}
