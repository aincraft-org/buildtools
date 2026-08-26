package dev.mintychochip.masonry.common.session;

public enum ToolMode {
    SELECT, FILL, REPLACE, COPY, PASTE, CUT, MOVE;

    public ToolMode next() {
        ToolMode[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }
}
