package net.aetherealtech.ujpeinvoice.transport;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ThrottleTest {

    private long nanos;
    private final List<Duration> sleeps = new ArrayList<>();
    private final Throttle throttle = new Throttle(Duration.ofSeconds(1), () -> nanos, wait -> {
        sleeps.add(wait);
        nanos += wait.toNanos();
    });

    @Test
    void theFirstRequestIsNeverDelayed() throws Exception {
        throttle.acquire();

        assertThat(sleeps).isEmpty();
    }

    @Test
    void aRequestRightAfterAnotherWaitsTheWholeInterval() throws Exception {
        throttle.acquire();
        throttle.acquire();

        assertThat(sleeps).containsExactly(Duration.ofSeconds(1));
    }

    @Test
    void aRequestWaitsOnlyTheRemainderOfTheInterval() throws Exception {
        throttle.acquire();
        nanos += Duration.ofMillis(300).toNanos();
        throttle.acquire();

        assertThat(sleeps).containsExactly(Duration.ofMillis(700));
    }

    @Test
    void aRequestAfterTheIntervalIsNotDelayedAndRestartsIt() throws Exception {
        throttle.acquire();
        nanos += Duration.ofSeconds(2).toNanos();
        throttle.acquire();
        throttle.acquire();

        assertThat(sleeps).containsExactly(Duration.ofSeconds(1));
    }

    @Test
    void theShippedThrottleDelaysNothingBeforeItsFirstRequest() throws Exception {
        long before = System.nanoTime();

        Throttle.oncePerSecond().acquire();

        assertThat(System.nanoTime() - before).isLessThan(Duration.ofMillis(500).toNanos());
    }
}
