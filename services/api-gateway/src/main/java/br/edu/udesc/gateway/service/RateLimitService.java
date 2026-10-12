package br.edu.udesc.gateway.service;

import br.edu.udesc.gateway.domain.RouteDefinition;
import br.edu.udesc.gateway.exception.GatewayException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class RateLimitService {

    private final Clock clock;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public RateLimitService() {
        this(Clock.systemUTC());
    }

    public RateLimitService(Clock clock) {
        this.clock = clock;
    }

    public void check(String clientKey, RouteDefinition route) {
        if (route == null || route.getRateLimitPerMinute() <= 0) {
            return;
        }

        String mapKey = route.getName() + ":" + clientKey;
        long nowSeconds = clock.instant().getEpochSecond();
        long currentMinute = nowSeconds / 60;

        Window window = windows.compute(mapKey, (k, existing) -> {
            if (existing == null || existing.minuteEpoch < currentMinute) {
                return new Window(currentMinute, 1);
            }
            existing.counter.incrementAndGet();
            return existing;
        });

        if (window.counter.get() > route.getRateLimitPerMinute()) {
            long secondsRemaining = 60 - (nowSeconds % 60);
            if (secondsRemaining <= 0) {
                secondsRemaining = 1;
            }
            throw new GatewayException(429, "Limite de requisições excedido",
                    Map.of("Retry-After", String.valueOf(secondsRemaining)));
        }
    }

    @Scheduled(fixedRate = 300_000)
    public void cleanupExpiredWindows() {
        long currentMinute = clock.instant().getEpochSecond() / 60;
        windows.entrySet().removeIf(entry -> entry.getValue().minuteEpoch < currentMinute);
    }

    public int getActiveWindowsCount() {
        return windows.size();
    }

    static class Window {
        final long minuteEpoch;
        final AtomicInteger counter;

        Window(long minuteEpoch, int initialCount) {
            this.minuteEpoch = minuteEpoch;
            this.counter = new AtomicInteger(initialCount);
        }
    }
}
