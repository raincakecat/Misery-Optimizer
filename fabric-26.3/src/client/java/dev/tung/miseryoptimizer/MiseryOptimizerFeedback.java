package dev.tung.miseryoptimizer;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.InteractionHand;

/** Optional feedback from the companion Paper plugin. */
final class MiseryOptimizerFeedback {
    private static final int MAX_BYTES = 6_172;

    record Payload(byte[] bytes) implements CustomPacketPayload {
        static final Type<Payload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("tung", "feedback"));
        static final StreamCodec<RegistryFriendlyByteBuf, Payload> CODEC = new StreamCodec<>() {
            @Override public Payload decode(RegistryFriendlyByteBuf buffer) {
                int length = buffer.readableBytes();
                if (length > MAX_BYTES) throw new IllegalArgumentException("Combat feedback too large");
                byte[] bytes = new byte[length];
                buffer.readBytes(bytes);
                return new Payload(bytes);
            }
            @Override public void encode(RegistryFriendlyByteBuf buffer, Payload value) {
                buffer.writeBytes(value.bytes);
            }
        };
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    static void register() {
        PayloadTypeRegistry.clientboundPlay().register(Payload.TYPE, Payload.CODEC);
        ClientPlayNetworking.registerGlobalReceiver(Payload.TYPE, (payload, context) -> accept(payload.bytes));
    }

    private static void accept(byte[] message) {
        if (message.length < 2 || message[0] != 2) return;
        try {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(message, 2, message.length - 2));
            switch (message[1]) {
                case 1 -> {
                    if (message.length < 28) return;
                    Vec3 center = new Vec3(in.readDouble(), in.readDouble(), in.readDouble());
                    int count = in.readUnsignedShort();
                    if (count > 2048 || in.available() != count * 3) return;
                    Set<Long> blocks = new HashSet<>(count * 2);
                    int ox = (int) Math.floor(center.x);
                    int oy = (int) Math.floor(center.y);
                    int oz = (int) Math.floor(center.z);
                    for (int i = 0; i < count; i++) {
                        blocks.add(new BlockPos(ox + in.readByte(), oy + in.readByte(), oz + in.readByte()).asLong());
                    }
                    ExplosionPreview.acceptExact(center, blocks);
                }
                case 2 -> {
                    if (in.available() == Integer.BYTES) MiseryOptimizerClient.rejectCrystal(in.readInt());
                }
                case 3 -> {
                    if (in.available() == Long.BYTES) MiseryOptimizerClient.rejectAnchor(BlockPos.of(in.readLong()));
                }
                case 4 -> {
                    if (in.available() == 0) GhostPearl.rejectOldest();
                }
                case 5 -> {
                    if (in.available() == Long.BYTES) GhostCrystal.reject(BlockPos.of(in.readLong()));
                }
                case 6 -> {
                    if (in.available() == Long.BYTES) BlockActionPreview.rejectPlacement(BlockPos.of(in.readLong()));
                }
                case 7 -> {
                    if (in.available() == Long.BYTES) BlockActionPreview.rejectBreak(BlockPos.of(in.readLong()));
                }
                case 8 -> {
                    if (in.available() == 1) FoodUsePreview.reject(
                        in.readUnsignedByte() == 1 ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
                }
                default -> { }
            }
        } catch (IOException ignored) {
            // Ignore malformed or stale feedback; ordinary Minecraft packets still reconcile predictions.
        }
    }
}
