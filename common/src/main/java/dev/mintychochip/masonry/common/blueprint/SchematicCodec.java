package dev.mintychochip.masonry.common.blueprint;

import dev.mintychochip.masonry.api.clipboard.BlockOffset;
import dev.mintychochip.masonry.api.clipboard.Clipboard;
import dev.mintychochip.masonry.api.world.BlockState;
import dev.mintychochip.masonry.common.nbt.NbtIo;
import dev.mintychochip.masonry.common.nbt.NbtTag;
import dev.mintychochip.masonry.common.tool.BlockStates;
import java.io.ByteArrayOutputStream;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Sponge Schematic v2 encode/decode over gzip NBT. Decode keeps air so paste can clear holes.
 */
public final class SchematicCodec {
    /** Schematic format version. */
    public static final int SCHEMATIC_VERSION = 2;
    /** Approximate data version for Paper 26.2. */
    public static final int DATA_VERSION = 4440;

    private SchematicCodec() {}

    /**
     * @param clipboard body including air
     * @param name blueprint name
     * @param owner owner UUID string
     * @param timestamp epoch millis
     * @return gzip-compressed named compound
     */
    public static byte[] encode(Clipboard clipboard, String name, String owner, long timestamp) {
        Dimensions dimensions = dimensions(clipboard);
        Map<String, Integer> palette = new LinkedHashMap<>();
        palette.put("minecraft:air", 0);
        for (BlockState state : clipboard.blocks().values()) {
            palette.putIfAbsent(toString(state), palette.size());
        }
        byte[] blockData = encodeBlockData(clipboard, dimensions, palette);

        NbtTag.NbtCompound paletteTag = new NbtTag.NbtCompound();
        palette.forEach((key, index) -> paletteTag.putInt(key, index));

        NbtTag.NbtCompound metadata = new NbtTag.NbtCompound()
                .putString("Name", name)
                .putString("Owner", owner)
                .putLong("Date", timestamp)
                .putString("World", clipboard.originWorldId());

        NbtTag.NbtCompound schematic = new NbtTag.NbtCompound()
                .putInt("Version", SCHEMATIC_VERSION)
                .putInt("DataVersion", DATA_VERSION)
                .putShort("Width", (short) dimensions.width)
                .putShort("Height", (short) dimensions.height)
                .putShort("Length", (short) dimensions.length)
                .putInt("PaletteMax", palette.size())
                .put("Palette", paletteTag)
                .putByteArray("BlockData", blockData)
                .put("Metadata", metadata);

        return NbtIo.writeGzipNamed("Schematic", schematic);
    }

    /**
     * @param data gzip schematic bytes
     * @return clipboard whose map includes air cells
     */
    public static Clipboard decode(byte[] data) {
        NbtTag.NbtCompound schematic = NbtIo.readGzipNamed(data);
        int width = schematic.getShort("Width");
        int height = schematic.getShort("Height");
        int length = schematic.getShort("Length");
        NbtTag.NbtCompound paletteTag = schematic.getCompound("Palette");
        Map<Integer, BlockState> palette = new LinkedHashMap<>();
        paletteTag.values().forEach((key, tag) ->
                palette.put(((NbtTag.NbtInt) tag).value(), BlockStates.parse(key)));
        byte[] blockData = schematic.getByteArray("BlockData");
        String world = "world";
        if (schematic.get("Metadata") instanceof NbtTag.NbtCompound metadata && metadata.get("World") != null) {
            world = metadata.getString("World");
        }
        Map<BlockOffset, BlockState> blocks = new LinkedHashMap<>();
        VarIntReader reader = new VarIntReader(blockData);
        for (int y = 0; y < height; y++) {
            for (int z = 0; z < length; z++) {
                for (int x = 0; x < width; x++) {
                    int index = reader.next();
                    BlockState state = palette.getOrDefault(index, BlockState.AIR);
                    blocks.put(new BlockOffset(x, y, z), state);
                }
            }
        }
        return new Clipboard(world, blocks);
    }

    /**
     * Inclusive dimensions covering every stored offset.
     *
     * @param clipboard clipboard
     * @return width/height/length
     */
    public static Dimensions dimensionsOf(Clipboard clipboard) {
        return dimensions(clipboard);
    }

    private static Dimensions dimensions(Clipboard clipboard) {
        if (clipboard.isEmpty()) {
            return new Dimensions(1, 1, 1);
        }
        int maxX = 0;
        int maxY = 0;
        int maxZ = 0;
        for (BlockOffset offset : clipboard.blocks().keySet()) {
            maxX = Math.max(maxX, offset.x());
            maxY = Math.max(maxY, offset.y());
            maxZ = Math.max(maxZ, offset.z());
        }
        return new Dimensions(maxX + 1, maxY + 1, maxZ + 1);
    }

    private static byte[] encodeBlockData(
            Clipboard clipboard, Dimensions dimensions, Map<String, Integer> palette) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        for (int y = 0; y < dimensions.height; y++) {
            for (int z = 0; z < dimensions.length; z++) {
                for (int x = 0; x < dimensions.width; x++) {
                    BlockState state = clipboard.blocks().getOrDefault(new BlockOffset(x, y, z), BlockState.AIR);
                    writeVarInt(output, palette.getOrDefault(toString(state), 0));
                }
            }
        }
        return output.toByteArray();
    }

    private static String toString(BlockState state) {
        if (state.properties().isEmpty()) {
            return state.namespacedKey();
        }
        StringBuilder builder = new StringBuilder(state.namespacedKey()).append('[');
        boolean first = true;
        for (Map.Entry<String, String> entry : state.properties().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .toList()) {
            if (!first) {
                builder.append(',');
            }
            builder.append(entry.getKey()).append('=').append(entry.getValue());
            first = false;
        }
        return builder.append(']').toString();
    }

    private static void writeVarInt(ByteArrayOutputStream output, int value) {
        int remaining = value;
        while ((remaining & ~0x7F) != 0) {
            output.write((remaining & 0x7F) | 0x80);
            remaining >>>= 7;
        }
        output.write(remaining);
    }

    /**
     * Inclusive schematic size.
     *
     * @param width X
     * @param height Y
     * @param length Z
     */
    public record Dimensions(int width, int height, int length) {}

    private static final class VarIntReader {
        private final byte[] data;
        private int index;

        private VarIntReader(byte[] data) {
            this.data = data;
        }

        private int next() {
            int value = 0;
            int shift = 0;
            while (index < data.length) {
                int next = data[index++] & 0xFF;
                value |= (next & 0x7F) << shift;
                if ((next & 0x80) == 0) {
                    return value;
                }
                shift += 7;
            }
            return value;
        }
    }
}
