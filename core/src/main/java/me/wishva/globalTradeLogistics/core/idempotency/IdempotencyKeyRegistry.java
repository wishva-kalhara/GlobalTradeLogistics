package me.wishva.globalTradeLogistics.core.idempotency;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Process-wide, in-memory store of idempotency keys already consumed by
 * {@code @IdempotencyChecked} methods. Replaces the legacy {@code logs}
 * table — keys are recorded synchronously after a successful business-method
 * invocation and checked before the next one with the same key.
 * <p>
 * Not durable across restarts or cluster members; sufficient for this
 * single-instance deployment. Intentionally a plain singleton (not a CDI
 * bean) so it resolves from the shared {@code core} library JAR without
 * requiring a {@code beans.xml} there.
 */
public final class IdempotencyKeyRegistry {

    private static final IdempotencyKeyRegistry INSTANCE = new IdempotencyKeyRegistry();

    private final Set<String> seenKeys = ConcurrentHashMap.newKeySet();

    private IdempotencyKeyRegistry() {
    }

    public static IdempotencyKeyRegistry getInstance() {
        return INSTANCE;
    }

    public boolean hasSeen(String idempotencyKey) {
        return seenKeys.contains(idempotencyKey);
    }

    public void record(String idempotencyKey) {
        seenKeys.add(idempotencyKey);
    }
}
