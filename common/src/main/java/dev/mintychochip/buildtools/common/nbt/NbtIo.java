package dev.mintychochip.buildtools.common.nbt;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Gzip named-compound NBT used by Sponge schematics.
 */
public final class NbtIo {
    private NbtIo() {}

    /**
     * @param name root tag name (typically {@code Schematic})
     * @param root compound
     * @return gzip bytes
     */
    public static byte[] writeGzipNamed(String name, NbtTag.NbtCompound root) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (GZIPOutputStream gzip = new GZIPOutputStream(bytes);
                    DataOutputStream output = new DataOutputStream(gzip)) {
                output.writeByte(NbtTag.COMPOUND);
                output.writeUTF(name);
                root.writePayload(output);
            }
            return bytes.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to write NBT", exception);
        }
    }

    /**
     * @param data gzip named compound
     * @return root compound
     * @throws IllegalArgumentException if the bytes are not a gzip compound
     */
    public static NbtTag.NbtCompound readGzipNamed(byte[] data) {
        try (DataInputStream input = new DataInputStream(new GZIPInputStream(new ByteArrayInputStream(data)))) {
            int type = input.readByte();
            if (type != NbtTag.COMPOUND) {
                throw new IOException("Root NBT tag must be a compound");
            }
            input.readUTF();
            return (NbtTag.NbtCompound) NbtTag.read(NbtTag.COMPOUND, input);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Failed to read NBT", exception);
        }
    }
}
