package dev.mintychochip.masonry.api.clipboard;

/**
 * Block coordinate relative to a clipboard origin (usually the selection minimum).
 *
 * @param x relative X
 * @param y relative Y
 * @param z relative Z
 */
public record BlockOffset(int x, int y, int z) {}
