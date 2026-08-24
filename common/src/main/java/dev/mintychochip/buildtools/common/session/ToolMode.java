package dev.mintychochip.buildtools.common.session;

public enum ToolMode {
    FILL, REPLACE, COPY, PASTE;

    public ToolMode next() {
        ToolMode[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }
}
