package dev.caydenhatch.combatsync;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class PredictionRegistryTest {
    @Test
    void highPingPredictionWaitsLongerAndThenRestores() {
        AtomicLong clock = new AtomicLong(1_000);
        PredictionRegistry registry = new PredictionRegistry(clock::get);
        registry.predictCrystal(7, 400);
        assertTrue(registry.isCrystalPending(7));
        clock.addAndGet(1_699);
        assertTrue(registry.isCrystalPending(7));
        clock.incrementAndGet();
        assertFalse(registry.isCrystalPending(7));
    }

    @Test
    void authoritativeConfirmationAndDisconnectClearPendingState() {
        PredictionRegistry registry = new PredictionRegistry(() -> 0L);
        registry.predictCrystal(2, 80);
        registry.predictAnchor(42L, 80);
        registry.confirmCrystal(2);
        assertFalse(registry.isCrystalPending(2));
        assertTrue(registry.isAnchorPending(42L));
        registry.clear();
        assertFalse(registry.isAnchorPending(42L));
    }

    @Test
    void configurableGraceExtendsAndCapsPrediction() {
        assertEquals(2_000L, PredictionRegistry.timeoutMillis(100, 1_100));
        assertEquals(8_000L, PredictionRegistry.timeoutMillis(3_000, 2_000));
        AtomicLong clock = new AtomicLong();
        PredictionRegistry registry = new PredictionRegistry(clock::get);
        registry.predictAnchor(8L, 100, 1_100);
        clock.set(1_999);
        assertTrue(registry.isAnchorPending(8L));
        clock.set(2_000);
        assertFalse(registry.isAnchorPending(8L));
    }
}
