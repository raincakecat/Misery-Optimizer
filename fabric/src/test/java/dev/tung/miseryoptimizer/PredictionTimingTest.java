package dev.tung.miseryoptimizer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class PredictionTimingTest {
    @Test
    void observedSlowConfirmationsOnlyExtendTheOptionalTimeout() {
        AtomicLong clock = new AtomicLong(1_000);
        PredictionTiming timing = new PredictionTiming(clock::get);
        long normal = PredictionRegistry.timeoutMillis(80, 0);
        timing.startCrystal(7);
        clock.addAndGet(1_200);
        timing.confirmCrystal(7);
        assertEquals(normal, timing.timeoutMillis(80, 0, false));
        assertTrue(timing.timeoutMillis(80, 0, true) > normal);
        assertEquals(1, timing.samples());
        assertEquals(1, timing.confirmed());
    }

    @Test
    void rejectedPredictionCountsOnceAndDisconnectResetsMeasurements() {
        AtomicLong clock = new AtomicLong();
        PredictionTiming timing = new PredictionTiming(clock::get);
        timing.startAnchor(42);
        timing.startAnchor(42);
        clock.addAndGet(200);
        timing.rejectAnchor(42);
        timing.rejectAnchor(42);
        assertEquals(1, timing.corrected());
        timing.clear();
        assertEquals(0, timing.corrected());
        assertEquals(0, timing.samples());
    }
}
