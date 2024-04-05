package de.mrjulsen.crn.network;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Supplier;

import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.network.IPacketBase;
import net.minecraft.network.Connection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public abstract class NetworkManagerBase<N extends NetworkManagerBase<N>> {

    public final String PROTOCOL_VERSION;
    private static int currentId = 0;

    public final SimpleChannel MOD_CHANNEL;

    protected NetworkManagerBase(String modid, String channelName, String protocolVersion) {
        PROTOCOL_VERSION = protocolVersion;
        MOD_CHANNEL = NetworkRegistry.ChannelBuilder.named(new ResourceLocation(modid, channelName)).networkProtocolVersion(() -> PROTOCOL_VERSION).clientAcceptedVersions(PROTOCOL_VERSION::equals).serverAcceptedVersions(PROTOCOL_VERSION::equals).simpleChannel();
        
        registerPackets(packets());
    }

    public <T extends IPacketBase<T>> void registerPackets(Collection<Class<? extends IPacketBase<?>>> classes) {
        for (Class<? extends IPacketBase<?>> clazz : classes) {
            registerPacketHelper(clazz);
        }
    }
    
    @SuppressWarnings("unchecked")
    private <T extends IPacketBase<T>> void registerPacketHelper(Class<?> clazz) {
        registerPacket((Class<T>)clazz);
    }
    

    public static <T extends NetworkManagerBase<T>> T create(Class<T> networkManager, String modid, String channelName, String protocolVersion) {        
        try {
            return networkManager.getConstructor(String.class, String.class, String.class).newInstance(modid, channelName, protocolVersion);
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
            e.printStackTrace();
            DragonLib.LOGGER.error("Unable to create NetworkManager.", e);
            return null;
        }
    }

    public abstract Collection<Class<? extends IPacketBase<?>>> packets();

    public SimpleChannel getPlayChannel() {
        return MOD_CHANNEL;
    }

    private <T extends IPacketBase<T>> void registerPacket(Class<T> clazz) {
        try {
            T packet = clazz.getConstructor().newInstance();
            currentId++;
            final int id = currentId;
            MOD_CHANNEL.registerMessage(id, clazz, packet::encode, packet::decode, packet::handle, Optional.of(packet.getDirection()));
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException  | NoSuchMethodException | SecurityException e) {
            e.printStackTrace();
            DragonLib.LOGGER.error("Unable to register packet.", e);
        }
    }

    public <T> void sendToClient(IPacketBase<?> o, ServerPlayer player) {
        MOD_CHANNEL.sendTo(o, player.connection.getConnection(), NetworkDirection.PLAY_TO_CLIENT);
    }

    public <T> void sendToServer(Connection connection, IPacketBase<?> o) {
        MOD_CHANNEL.sendTo(o, connection, NetworkDirection.PLAY_TO_SERVER);
    }

    public static void executeOnClient(Runnable task) {        
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> task.run());
    }

    public static <T extends IPacketBase<?>> void handlePacket(T packet, Supplier<NetworkEvent.Context> context, Runnable run) {
        context.get().enqueueWork(() -> {
            if (packet.getDirection() == NetworkDirection.PLAY_TO_SERVER || packet.getDirection() == NetworkDirection.LOGIN_TO_SERVER) {
                run.run();
            } else if (packet.getDirection() == NetworkDirection.PLAY_TO_CLIENT || packet.getDirection() == NetworkDirection.LOGIN_TO_CLIENT) {
                executeOnClient(run);
            }
        });        
        context.get().setPacketHandled(true);
    }
}

