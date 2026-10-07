package de.mrjulsen.crn.components;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record NavigatorBackgroundComponent(int backgroundId) {

    public static final Codec<NavigatorBackgroundComponent> CODEC = RecordCodecBuilder.create(builder -> {
        return builder.group(
                Codec.INT.optionalFieldOf("background_id", 0).forGetter(NavigatorBackgroundComponent::backgroundId)
        ).apply(builder, NavigatorBackgroundComponent::new);
    });

    public static final StreamCodec<RegistryFriendlyByteBuf, NavigatorBackgroundComponent> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, NavigatorBackgroundComponent::backgroundId,
            NavigatorBackgroundComponent::new
    );

    public static NavigatorBackgroundComponent empty() {
        return new NavigatorBackgroundComponent(
            0
        );
    }
}
