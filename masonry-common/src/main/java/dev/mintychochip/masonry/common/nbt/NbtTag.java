package dev.mintychochip.masonry.common.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Minimal NBT tags needed for Sponge Schematic v2 (no float/double).
 */
public abstract class NbtTag {
    /** End of compound. */
    public static final int END = 0;
    public static final int BYTE = 1;
    public static final int SHORT = 2;
    public static final int INT = 3;
    public static final int LONG = 4;
    public static final int BYTE_ARRAY = 7;
    public static final int STRING = 8;
    public static final int LIST = 9;
    public static final int COMPOUND = 10;
    public static final int INT_ARRAY = 11;

    /** @return NBT type id */
    public abstract int type();

    /**
     * Writes this tag's payload (not the type byte or name).
     *
     * @param output output
     * @throws IOException if writing fails
     */
    public abstract void writePayload(DataOutput output) throws IOException;

    /**
     * @param type type id
     * @param input payload
     * @return tag
     * @throws IOException if the type is unsupported or the stream is short
     */
    public static NbtTag read(int type, DataInput input) throws IOException {
        return switch (type) {
            case BYTE -> new NbtByte(input.readByte());
            case SHORT -> new NbtShort(input.readShort());
            case INT -> new NbtInt(input.readInt());
            case LONG -> new NbtLong(input.readLong());
            case BYTE_ARRAY -> {
                int length = input.readInt();
                byte[] data = new byte[length];
                input.readFully(data);
                yield new NbtByteArray(data);
            }
            case STRING -> new NbtString(input.readUTF());
            case LIST -> {
                int listType = input.readByte();
                int size = input.readInt();
                List<NbtTag> values = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    values.add(read(listType, input));
                }
                yield new NbtList(listType, values);
            }
            case COMPOUND -> NbtCompound.readPayload(input);
            case INT_ARRAY -> {
                int length = input.readInt();
                int[] data = new int[length];
                for (int i = 0; i < length; i++) {
                    data[i] = input.readInt();
                }
                yield new NbtIntArray(data);
            }
            default -> throw new IOException("Unsupported NBT type: " + type);
        };
    }

    /** TAG_Byte. */
    public static final class NbtByte extends NbtTag {
        private final byte value;

        public NbtByte(byte value) {
            this.value = value;
        }

        public byte value() {
            return value;
        }

        @Override
        public int type() {
            return BYTE;
        }

        @Override
        public void writePayload(DataOutput output) throws IOException {
            output.writeByte(value);
        }
    }

    /** TAG_Short. */
    public static final class NbtShort extends NbtTag {
        private final short value;

        public NbtShort(short value) {
            this.value = value;
        }

        public short value() {
            return value;
        }

        @Override
        public int type() {
            return SHORT;
        }

        @Override
        public void writePayload(DataOutput output) throws IOException {
            output.writeShort(value);
        }
    }

    /** TAG_Int. */
    public static final class NbtInt extends NbtTag {
        private final int value;

        public NbtInt(int value) {
            this.value = value;
        }

        public int value() {
            return value;
        }

        @Override
        public int type() {
            return INT;
        }

        @Override
        public void writePayload(DataOutput output) throws IOException {
            output.writeInt(value);
        }
    }

    /** TAG_Long. */
    public static final class NbtLong extends NbtTag {
        private final long value;

        public NbtLong(long value) {
            this.value = value;
        }

        public long value() {
            return value;
        }

        @Override
        public int type() {
            return LONG;
        }

        @Override
        public void writePayload(DataOutput output) throws IOException {
            output.writeLong(value);
        }
    }

    /** TAG_String. */
    public static final class NbtString extends NbtTag {
        private final String value;

        public NbtString(String value) {
            this.value = Objects.requireNonNull(value, "value");
        }

        public String value() {
            return value;
        }

        @Override
        public int type() {
            return STRING;
        }

        @Override
        public void writePayload(DataOutput output) throws IOException {
            output.writeUTF(value);
        }
    }

    /** TAG_Byte_Array. */
    public static final class NbtByteArray extends NbtTag {
        private final byte[] value;

        public NbtByteArray(byte[] value) {
            this.value = Objects.requireNonNull(value, "value");
        }

        public byte[] value() {
            return value;
        }

        @Override
        public int type() {
            return BYTE_ARRAY;
        }

        @Override
        public void writePayload(DataOutput output) throws IOException {
            output.writeInt(value.length);
            output.write(value);
        }
    }

    /** TAG_Int_Array. */
    public static final class NbtIntArray extends NbtTag {
        private final int[] value;

        public NbtIntArray(int[] value) {
            this.value = Objects.requireNonNull(value, "value");
        }

        public int[] value() {
            return value;
        }

        @Override
        public int type() {
            return INT_ARRAY;
        }

        @Override
        public void writePayload(DataOutput output) throws IOException {
            output.writeInt(value.length);
            for (int item : value) {
                output.writeInt(item);
            }
        }
    }

    /** TAG_List of a single element type. */
    public static final class NbtList extends NbtTag {
        private final int elementType;
        private final List<NbtTag> values;

        public NbtList(int elementType, List<NbtTag> values) {
            this.elementType = elementType;
            this.values = List.copyOf(values);
        }

        public List<NbtTag> values() {
            return values;
        }

        @Override
        public int type() {
            return LIST;
        }

        @Override
        public void writePayload(DataOutput output) throws IOException {
            output.writeByte(elementType);
            output.writeInt(values.size());
            for (NbtTag value : values) {
                value.writePayload(output);
            }
        }
    }

    /** TAG_Compound of named children. */
    public static final class NbtCompound extends NbtTag {
        private final Map<String, NbtTag> values = new LinkedHashMap<>();

        public NbtCompound put(String key, NbtTag tag) {
            values.put(key, tag);
            return this;
        }

        public NbtCompound putInt(String key, int value) {
            return put(key, new NbtInt(value));
        }

        public NbtCompound putShort(String key, short value) {
            return put(key, new NbtShort(value));
        }

        public NbtCompound putLong(String key, long value) {
            return put(key, new NbtLong(value));
        }

        public NbtCompound putString(String key, String value) {
            return put(key, new NbtString(value));
        }

        public NbtCompound putByteArray(String key, byte[] value) {
            return put(key, new NbtByteArray(value));
        }

        public NbtTag get(String key) {
            return values.get(key);
        }

        public int getInt(String key) {
            return ((NbtInt) require(key)).value();
        }

        public short getShort(String key) {
            return ((NbtShort) require(key)).value();
        }

        public long getLong(String key) {
            return ((NbtLong) require(key)).value();
        }

        public String getString(String key) {
            return ((NbtString) require(key)).value();
        }

        public byte[] getByteArray(String key) {
            return ((NbtByteArray) require(key)).value();
        }

        public NbtCompound getCompound(String key) {
            return (NbtCompound) require(key);
        }

        public Map<String, NbtTag> values() {
            return Map.copyOf(values);
        }

        private NbtTag require(String key) {
            NbtTag tag = values.get(key);
            if (tag == null) {
                throw new IllegalArgumentException("Missing NBT key: " + key);
            }
            return tag;
        }

        @Override
        public int type() {
            return COMPOUND;
        }

        @Override
        public void writePayload(DataOutput output) throws IOException {
            for (Map.Entry<String, NbtTag> entry : values.entrySet()) {
                output.writeByte(entry.getValue().type());
                output.writeUTF(entry.getKey());
                entry.getValue().writePayload(output);
            }
            output.writeByte(END);
        }

        static NbtCompound readPayload(DataInput input) throws IOException {
            NbtCompound compound = new NbtCompound();
            while (true) {
                int type = input.readByte();
                if (type == END) {
                    return compound;
                }
                String name = input.readUTF();
                compound.put(name, read(type, input));
            }
        }
    }
}
