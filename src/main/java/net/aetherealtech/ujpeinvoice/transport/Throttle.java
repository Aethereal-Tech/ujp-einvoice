package net.aetherealtech.ujpeinvoice.transport;

import java.time.Duration;
import java.util.function.LongSupplier;

/**
 * Spaces requests at least {@code interval} apart. D1 „API Documentation": „На ниво на корисник има поставено
 * ограничување од 1 request во секунда" (one request per second per user); {@code E4007} is the refusal.
 * Every request counts, including the server-time read that precedes a send or a status poll.
 *
 * <p>The clock and the sleeper are injected so a test can run on a fake clock and never sleep for real. The
 * instance is thread-safe: concurrent callers are serialized and each is spaced from the one before.
 */
final class Throttle {

    @FunctionalInterface
    interface Sleeper {
        void sleep(Duration duration) throws InterruptedException;
    }

    private final long intervalNanos;
    private final LongSupplier nanoClock;
    private final Sleeper sleeper;
    private long lastNanos;
    private boolean started;

    Throttle(Duration interval, LongSupplier nanoClock, Sleeper sleeper) {
        this.intervalNanos = interval.toNanos();
        this.nanoClock = nanoClock;
        this.sleeper = sleeper;
    }

    static Throttle oncePerSecond() {
        return new Throttle(Duration.ofSeconds(1), System::nanoTime,
                duration -> Thread.sleep(duration.toMillis(), duration.toNanosPart() % 1_000_000));
    }

    synchronized void acquire() throws InterruptedException {
        long now = nanoClock.getAsLong();
        if (started) {
            long wait = lastNanos + intervalNanos - now;
            if (wait > 0) {
                sleeper.sleep(Duration.ofNanos(wait));
                now = nanoClock.getAsLong();
            }
        }
        lastNanos = now;
        started = true;
    }
}
