package me.wishva.globalTradeLogistics.core.security;

/**
 * Thread-scoped holder for the {@link CurrentPrincipal} resolved by
 * {@code JwtAuthFilter} in api-gateway. Relies on {@code core} being bundled
 * in the EAR's shared {@code lib/} directory (see app/pom.xml's
 * maven-ear-plugin config) so the WAR and every EJB module load this exact
 * class from one shared classloader — otherwise each module would see its
 * own, disconnected copy of the ThreadLocal.
 * <p>
 * A local EJB call from a servlet runs on the caller's own thread, so the
 * value set by the filter is visible to the EJB tier for the rest of that
 * request. The filter MUST clear this in a {@code finally} block, since
 * GlassFish reuses request-handling threads across requests.
 */
public final class CurrentPrincipalHolder {

    private static final ThreadLocal<CurrentPrincipal> HOLDER = new ThreadLocal<>();

    private CurrentPrincipalHolder() {
    }

    public static void set(CurrentPrincipal principal) {
        HOLDER.set(principal);
    }

    public static CurrentPrincipal get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
