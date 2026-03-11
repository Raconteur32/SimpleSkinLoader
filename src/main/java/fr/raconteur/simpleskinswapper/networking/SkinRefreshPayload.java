package fr.raconteur.simpleskinswapper.networking;

import com.mojang.authlib.properties.Property;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record SkinRefreshPayload(Property textureProperty) implements CustomPayload {
    public static final CustomPayload.Id<SkinRefreshPayload> PACKET_ID =
            new CustomPayload.Id<>(Identifier.of("skinshuffle", "skin_refresh"));

    public static final PacketCodec<RegistryByteBuf, SkinRefreshPayload> PACKET_CODEC =
            PacketCodec.of(SkinRefreshPayload::write, SkinRefreshPayload::read);

    private static void write(SkinRefreshPayload payload, RegistryByteBuf buf) {
        Property p = payload.textureProperty();
        buf.writeBoolean(p.hasSignature());
        buf.writeString(p.name());
        buf.writeString(p.value());
        if (p.hasSignature()) buf.writeString(p.signature());
    }

    private static SkinRefreshPayload read(RegistryByteBuf buf) {
        boolean hasSig = buf.readBoolean();
        String name = buf.readString();
        String value = buf.readString();
        String sig = hasSig ? buf.readString() : null;
        return new SkinRefreshPayload(new Property(name, value, sig));
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }
}
