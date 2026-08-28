package dev.mintychochip.masonry.paper.adapter;

import com.mojang.math.Transformation;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.block.data.CraftBlockData;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Sends unregistered BlockDisplay entities directly to one client. The NMS entity is only a
 * packet-data builder: it is never added to a world, tracked by Paper, or ticked by the server.
 * Mapped names are pinned by the Paperweight userdev bundle to Paper 26.2 build 119.
 */
final class PacketBlockDisplayRenderer {
    private static final EntityType<?> BLOCK_DISPLAY_TYPE = Objects.requireNonNull(
            BuiltInRegistries.ENTITY_TYPE.getValue(
                    net.minecraft.resources.Identifier.parse("minecraft:block_display")),
            "minecraft:block_display entity type");
    private static final Transformation TRANSFORMATION = new Transformation(
            new Vector3f(0.05f, 0.05f, 0.05f),
            new Quaternionf(),
            new Vector3f(0.9f, 0.9f, 0.9f),
            new Quaternionf());
    DisplayState spawn(Player player, World world, int x, int y, int z, BlockData blockData) {
        ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
        ServerLevel serverLevel = ((CraftWorld) world).getHandle();
        Display.BlockDisplay display = create(serverLevel, x, y, z, blockData);
        DisplayState state = new DisplayState(display.getId(), display.getUUID(), blockData.clone());
        send(serverPlayer, display, state.id(), state.uuid(), x, y, z);
        return state;
    }

    void resend(Player player, World world, int x, int y, int z, DisplayState state) {
        ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
        ServerLevel serverLevel = ((CraftWorld) world).getHandle();
        Display.BlockDisplay display = create(serverLevel, x, y, z, state.blockData());
        send(serverPlayer, display, state.id(), state.uuid(), x, y, z);
    }

    void remove(Player player, List<DisplayState> states) {
        if (states.isEmpty()) {
            return;
        }
        int[] ids = states.stream().mapToInt(DisplayState::id).toArray();
        ((CraftPlayer) player).getHandle().connection.send(new ClientboundRemoveEntitiesPacket(ids));
    }

    private static Display.BlockDisplay create(ServerLevel world, int x, int y, int z, BlockData blockData) {
        if (!(blockData instanceof CraftBlockData craftBlockData)) {
            throw new IllegalArgumentException("BlockData must be Paper CraftBlockData");
        }
        Display.BlockDisplay display = new Display.BlockDisplay(BLOCK_DISPLAY_TYPE, world);
        display.setPos(x, y, z);
        display.setBlockState(craftBlockData.getState());
        display.setTransformation(TRANSFORMATION);
        return display;
    }

    private static void send(
            ServerPlayer player,
            Display.BlockDisplay display,
            int id,
            UUID uuid,
            int x,
            int y,
            int z) {
        player.connection.send(new ClientboundAddEntityPacket(
                id,
                uuid,
                x,
                y,
                z,
                0.0f,
                0.0f,
                BLOCK_DISPLAY_TYPE,
                0,
                Vec3.ZERO,
                0.0));
        player.connection.send(new ClientboundSetEntityDataPacket(id, display.getEntityData().packAll()));
    }

    record DisplayState(int id, UUID uuid, BlockData blockData) {}
}
