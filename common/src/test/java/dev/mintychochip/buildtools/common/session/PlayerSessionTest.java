package dev.mintychochip.buildtools.common.session;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PlayerSessionTest {
    @Test
    void clearResetsModeToFill() {
        PlayerSession session = new PlayerSession();
        session.setMode(ToolMode.PASTE);
        session.clear();
        assertEquals(ToolMode.FILL, session.mode());
    }
}
