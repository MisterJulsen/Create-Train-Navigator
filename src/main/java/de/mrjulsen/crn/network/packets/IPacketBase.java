package de.mrjulsen.crn.network.packets;

import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public interface IPacketBase<T> {
    void encode(T message, FriendlyByteBuf buffer);
    T decode(FriendlyByteBuf buffer);
    void handle(T message, Supplier<NetworkEvent.Context> supplier);
}
