package com.sba.ssos.security;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class LastSeenAtFilterTest {

    @Test
    @Disabled("Stub — implemented during 02-02 TDD cycle")
    void shouldUpdateLastSeenAtWhenStale() {
        // TODO: assert save called when lastSeenAt is null or older than 5 min
    }

    @Test
    @Disabled("Stub — implemented during 02-02 TDD cycle")
    void shouldNotUpdateLastSeenAtWithinDebounceWindow() {
        // TODO: assert save NOT called when lastSeenAt is recent
    }

    @Test
    @Disabled("Stub — implemented during 02-02 TDD cycle")
    void shouldSkipActuatorPaths() {
        // TODO: assert shouldNotFilter returns true for /actuator paths
    }

    @Test
    @Disabled("Stub — implemented during 02-02 TDD cycle")
    void shouldSwallowExceptions() {
        // TODO: assert filterChain.doFilter called even when repo throws
    }
}
