package com.liebeblack.divtrack.lite;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Orden de intento de las fuentes de tasa, con un circuito por fuente.
 *
 * <p>Una fuente que falla se salta durante un enfriamiento: nadie vuelve a esperar su
 * tiempo de espera (timeout) en cada pasada. Si todas estuvieran abiertas se reintenta el
 * orden completo, de modo que el circuito nunca puede dejar la aplicación sin consulta. El
 * estado vive en memoria a propósito: el fallo más común es de red y un reinicio del
 * proceso reabre los circuitos.
 */
final class ProviderPlan {
    /** Tiempo durante el que una fuente que falló queda fuera del orden de intentos. */
    static final long COOLDOWN_MS = 10L * 60L * 1000L;

    private final String[] providers;
    private final ConcurrentHashMap<String, Long> openUntil =
            new ConcurrentHashMap<String, Long>();

    ProviderPlan(String... providers) {
        this.providers = providers;
    }

    /** Fuentes a intentar ahora, en orden de preferencia. Nunca devuelve una lista vacía. */
    String[] orderFor(long nowMillis) {
        ArrayList<String> available = new ArrayList<String>(providers.length);
        for (String provider : providers) {
            Long until = openUntil.get(provider);
            if (until == null || until <= nowMillis) {
                available.add(provider);
            }
        }
        if (available.isEmpty()) {
            return providers.clone();
        }
        return available.toArray(new String[available.size()]);
    }

    void recordFailure(String provider, long nowMillis) {
        openUntil.put(provider, nowMillis + COOLDOWN_MS);
    }

    void recordSuccess(String provider) {
        openUntil.remove(provider);
    }
}
