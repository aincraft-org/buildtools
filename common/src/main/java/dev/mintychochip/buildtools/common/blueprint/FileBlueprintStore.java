package dev.mintychochip.buildtools.common.blueprint;

import dev.mintychochip.buildtools.api.ActorId;
import dev.mintychochip.buildtools.api.blueprint.BlueprintMeta;
import dev.mintychochip.buildtools.api.clipboard.Clipboard;
import dev.mintychochip.buildtools.api.service.BlueprintStore;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

public final class FileBlueprintStore implements BlueprintStore {
    private final Path root;

    public FileBlueprintStore(Path root) {
        this.root = Objects.requireNonNull(root, "root");
    }

    @Override
    public void save(ActorId owner, String name, Clipboard clipboard) {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(clipboard, "clipboard");
        if (!isSafeName(name)) {
            throw new IllegalArgumentException("Invalid blueprint name: " + name);
        }
        try {
            Path directory = ownerDirectory(owner);
            Files.createDirectories(directory);
            long timestamp = Instant.now().toEpochMilli();
            Files.write(directory.resolve(name + ".schem"), SchematicCodec.encode(
                    clipboard, name, owner.value().toString(), timestamp));
            SchematicCodec.Dimensions dimensions = SchematicCodec.dimensionsOf(clipboard);
            String json = """
                    {"name":"%s","owner":"%s","createdAt":%d,"width":%d,"height":%d,"length":%d}
                    """.formatted(
                    escape(name),
                    owner.value(),
                    timestamp,
                    dimensions.width(),
                    dimensions.height(),
                    dimensions.length());
            Files.writeString(directory.resolve(name + ".json"), json, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to save blueprint " + name, exception);
        }
    }

    @Override
    public Optional<Clipboard> load(ActorId owner, String name) {
        if (!isSafeName(name)) {
            return Optional.empty();
        }
        Path file = ownerDirectory(owner).resolve(name + ".schem");
        if (!Files.isRegularFile(file)) {
            return Optional.empty();
        }
        try {
            return Optional.of(SchematicCodec.decode(Files.readAllBytes(file)));
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load blueprint " + name, exception);
        }
    }

    @Override
    public List<BlueprintMeta> list(ActorId owner) {
        Path directory = ownerDirectory(owner);
        if (!Files.isDirectory(directory)) {
            return List.of();
        }
        try (Stream<Path> stream = Files.list(directory)) {
            List<BlueprintMeta> metas = new ArrayList<>();
            stream.filter(path -> path.getFileName().toString().endsWith(".json"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .forEach(path -> readMeta(owner, path).ifPresent(metas::add));
            return List.copyOf(metas);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to list blueprints", exception);
        }
    }

    @Override
    public boolean delete(ActorId owner, String name) {
        if (!isSafeName(name)) {
            return false;
        }
        Path directory = ownerDirectory(owner);
        Path schematic = directory.resolve(name + ".schem");
        Path meta = directory.resolve(name + ".json");
        boolean existed = Files.exists(schematic) || Files.exists(meta);
        try {
            Files.deleteIfExists(schematic);
            Files.deleteIfExists(meta);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to delete blueprint " + name, exception);
        }
        return existed;
    }

    private Optional<BlueprintMeta> readMeta(ActorId owner, Path path) {
        try {
            String json = Files.readString(path, StandardCharsets.UTF_8);
            String name = extract(json, "name");
            long createdAt = Long.parseLong(extract(json, "createdAt"));
            int width = Integer.parseInt(extract(json, "width"));
            int height = Integer.parseInt(extract(json, "height"));
            int length = Integer.parseInt(extract(json, "length"));
            return Optional.of(new BlueprintMeta(name, owner, Instant.ofEpochMilli(createdAt), width, height, length));
        } catch (RuntimeException | IOException ignored) {
            return Optional.empty();
        }
    }

    private Path ownerDirectory(ActorId owner) {
        return root.resolve(owner.value().toString());
    }

    private static boolean isSafeName(String name) {
        return name != null && name.matches("[A-Za-z0-9._-]{1,64}");
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String extract(String json, String key) {
        String quoted = "\"" + key + "\"";
        int index = json.indexOf(quoted);
        if (index < 0) {
            throw new IllegalArgumentException("Missing " + key);
        }
        int colon = json.indexOf(':', index);
        String raw = json.substring(colon + 1).trim();
        if (raw.startsWith("\"")) {
            int end = raw.indexOf('"', 1);
            return raw.substring(1, end);
        }
        int end = 0;
        while (end < raw.length() && (Character.isDigit(raw.charAt(end)) || raw.charAt(end) == '-')) {
            end++;
        }
        return raw.substring(0, end);
    }

    public static ActorId ownerFrom(UUID uuid) {
        return new ActorId(uuid);
    }
}
