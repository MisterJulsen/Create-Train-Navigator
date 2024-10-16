package de.mrjulsen.crn.network.packets.cts;

import java.util.function.Supplier;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.data.AliasName;
import de.mrjulsen.crn.data.GlobalSettingsManager;
import de.mrjulsen.crn.data.TrainGroup;
import de.mrjulsen.crn.data.TrainStationAlias;
import de.mrjulsen.crn.network.InstanceManager;
import de.mrjulsen.crn.network.packets.stc.GlobalSettingsResponsePacket;
import de.mrjulsen.mcdragonlib.net.IPacketBase;
import dev.architectury.networking.NetworkManager.PacketContext;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public class GlobalSettingsUpdatePacket implements IPacketBase<GlobalSettingsUpdatePacket> {
    private static final String NBT_STRING = "Val";
    private static final String NBT_COMPOUND_TAG = "Tag";

    public long id;
    public CompoundTag data;
    public EGlobalSettingsAction action;
    
    public GlobalSettingsUpdatePacket() { }

    public GlobalSettingsUpdatePacket(long id, CompoundTag nbt, EGlobalSettingsAction action) {
        this.id = id;
        this.data = nbt;
        this.action = action;
    }

    public static void send(Object data, EGlobalSettingsAction action, Runnable then) {
        CompoundTag nbt = new CompoundTag();
        switch (action) {
            case ADD_TO_BLACKLIST:
            case REMOVE_FROM_BLACKLIST:            
            case ADD_TRAIN_TO_BLACKLIST:
            case REMOVE_TRAIN_FROM_BLACKLIST:
            case UNREGISTER_ALIAS_STRING:
            case UNREGISTER_TRAIN_GROUP_TRAIN:
                nbt.putString(NBT_STRING, (String)data);
                break;
            case UNREGISTER_ALIAS:
            case REGISTER_ALIAS:
                nbt = ((TrainStationAlias)data).toNbt();
                break;                
            case UNREGISTER_TRAIN_GROUP:
            case REGISTER_TRAIN_GROUP:
                nbt = ((TrainGroup)data).toNbt();
                break;
            case UPDATE_ALIAS:
                Object[] dataArr = (Object[])data;
                nbt.putString(NBT_STRING, (String)dataArr[0]);
                nbt.put(NBT_COMPOUND_TAG, ((TrainStationAlias)dataArr[1]).toNbt());
                break;
            case UPDATE_TRAIN_GROUP:
                Object[] dataArr1 = (Object[])data;
                nbt.putString(NBT_STRING, (String)dataArr1[0]);
                nbt.put(NBT_COMPOUND_TAG, ((TrainGroup)dataArr1[1]).toNbt());
                break;
            default:
                return;
        }
        CreateRailwaysNavigator.net().CHANNEL.sendToServer(new GlobalSettingsUpdatePacket(InstanceManager.registerClientResponseReceievedAction(then), nbt, action));
    }

    @Override
    public void encode(GlobalSettingsUpdatePacket packet, FriendlyByteBuf buffer) {
        buffer.writeLong(packet.id);
        buffer.writeNbt(packet.data);
        buffer.writeEnum(packet.action);
    }

    @Override
    public GlobalSettingsUpdatePacket decode(FriendlyByteBuf buffer) {
        long id = buffer.readLong();
        CompoundTag data = buffer.readNbt();
        EGlobalSettingsAction action = buffer.readEnum(EGlobalSettingsAction.class);
        GlobalSettingsUpdatePacket instance = new GlobalSettingsUpdatePacket(id, data, action);
        return instance;
    }
    
    @Override
    public void handle(GlobalSettingsUpdatePacket packet, Supplier<PacketContext> contextSupplier) {
        contextSupplier.get().queue(() -> {
            if (GlobalSettingsManager.getInstance().getSettingsData() == null) {
                CreateRailwaysNavigator.LOGGER.error("Failed to handle GlobalSettingsUpdatePacket! The settings instance of the global settings manager is null.");
                return;
            }
    
            switch (packet.action) {
                case ADD_TO_BLACKLIST:
                    GlobalSettingsManager.getInstance().getSettingsData().addToBlacklistServer(packet.data.getString(NBT_STRING));
                    break;
                case REMOVE_FROM_BLACKLIST:
                    GlobalSettingsManager.getInstance().getSettingsData().removeFromBlacklistServer(packet.data.getString(NBT_STRING));
                    break;
                case ADD_TRAIN_TO_BLACKLIST:
                    GlobalSettingsManager.getInstance().getSettingsData().addTrainToBlacklistServer(packet.data.getString(NBT_STRING));
                    break;
                case REMOVE_TRAIN_FROM_BLACKLIST:
                    GlobalSettingsManager.getInstance().getSettingsData().removeTrainFromBlacklistServer(packet.data.getString(NBT_STRING));
                    break;
                case UNREGISTER_ALIAS_STRING:
                    GlobalSettingsManager.getInstance().getSettingsData().unregisterAliasServer(packet.data.getString(NBT_STRING));
                    break;
                case UNREGISTER_ALIAS:
                    GlobalSettingsManager.getInstance().getSettingsData().unregisterAliasServer(TrainStationAlias.fromNbt(packet.data));
                    break;
                case REGISTER_ALIAS:
                    GlobalSettingsManager.getInstance().getSettingsData().registerAliasServer(TrainStationAlias.fromNbt(packet.data));
                    break;
                case UPDATE_ALIAS:
                    GlobalSettingsManager.getInstance().getSettingsData().updateAliasServer(AliasName.of(packet.data.getString(NBT_STRING)), TrainStationAlias.fromNbt(packet.data.getCompound(NBT_COMPOUND_TAG)));
                    break;
                case UNREGISTER_TRAIN_GROUP_TRAIN:
                    GlobalSettingsManager.getInstance().getSettingsData().unregisterTrainGroupServer(packet.data.getString(NBT_STRING));
                    break;
                case UNREGISTER_TRAIN_GROUP:
                    GlobalSettingsManager.getInstance().getSettingsData().unregisterTrainGroupServer(TrainGroup.fromNbt(packet.data).getGroupName());
                    break;
                case REGISTER_TRAIN_GROUP:
                    GlobalSettingsManager.getInstance().getSettingsData().registerTrainGroupServer(TrainGroup.fromNbt(packet.data));
                    break;
                case UPDATE_TRAIN_GROUP:
                    GlobalSettingsManager.getInstance().getSettingsData().updateTrainGroupServer(packet.data.getString(NBT_STRING), TrainGroup.fromNbt(packet.data.getCompound(NBT_COMPOUND_TAG)));
                    break;
                default:
                    return;
            }
            GlobalSettingsManager.getInstance().setDirty();
            CreateRailwaysNavigator.net().CHANNEL.sendToPlayer((ServerPlayer)contextSupplier.get().getPlayer(), new GlobalSettingsResponsePacket(packet.id, GlobalSettingsManager.getInstance().getSettingsData()));    
        });
    }
    
    public static enum EGlobalSettingsAction {
        REGISTER_ALIAS,
        UNREGISTER_ALIAS_STRING,
        UNREGISTER_ALIAS,
        UPDATE_ALIAS,        
        REGISTER_TRAIN_GROUP,
        UNREGISTER_TRAIN_GROUP_TRAIN,
        UNREGISTER_TRAIN_GROUP,
        UPDATE_TRAIN_GROUP,
        ADD_TO_BLACKLIST,
        REMOVE_FROM_BLACKLIST,        
        ADD_TRAIN_TO_BLACKLIST,
        REMOVE_TRAIN_FROM_BLACKLIST;
    }
}
