package de.mrjulsen.crn.network.packets.cts;

import java.util.function.Supplier;

import de.mrjulsen.crn.ModMain;
import de.mrjulsen.crn.data.AliasName;
import de.mrjulsen.crn.data.GlobalSettingsManager;
import de.mrjulsen.crn.data.TrainStationAlias;
import de.mrjulsen.crn.network.InstanceManager;
import de.mrjulsen.crn.network.NetworkManager;
import de.mrjulsen.crn.network.packets.IPacketBase;
import de.mrjulsen.crn.network.packets.stc.GlobalSettingsResponsePacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

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
                nbt.putString(NBT_STRING, (String)data);
                break;          
            case UNREGISTER_ALIAS:
            case REGISTER_ALIAS:
                nbt = ((TrainStationAlias)data).toNbt();
                break;
            case UPDATE_ALIAS:
                Object[] dataArr = (Object[])data;
                nbt.putString(NBT_STRING, (String)dataArr[0]);
                nbt.put(NBT_COMPOUND_TAG, ((TrainStationAlias)dataArr[1]).toNbt());
                break;
            default:
                return;
        }
        NetworkManager.sendToServer(new GlobalSettingsUpdatePacket(InstanceManager.registerClientResponseReceievedAction(then), nbt, action));
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
    public void handle(GlobalSettingsUpdatePacket packet, Supplier<NetworkEvent.Context> context) {        
        context.get().enqueueWork(() ->
        {
            if (GlobalSettingsManager.getInstance().getSettingsData() == null) {
                ModMain.LOGGER.error("Failed to handle GlobalSettingsUpdatePacket! The settings instance of the global settings manager is null.");
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
                case UPDATE_ALIAS:
                    GlobalSettingsManager.getInstance().getSettingsData().updateAliasServer(AliasName.of(packet.data.getString(NBT_STRING)), TrainStationAlias.fromNbt(packet.data.getCompound(NBT_COMPOUND_TAG)));
                    break;
                default:
                    return;
            }
            GlobalSettingsManager.getInstance().setDirty();
            NetworkManager.sendToClient(new GlobalSettingsResponsePacket(packet.id, GlobalSettingsManager.getInstance().getSettingsData()), context.get().getSender());
        });
        
        context.get().setPacketHandled(true);      
    } 
    
    public static enum EGlobalSettingsAction {
        REGISTER_ALIAS,
        UNREGISTER_ALIAS_STRING,
        UNREGISTER_ALIAS,
        UPDATE_ALIAS,
        ADD_TO_BLACKLIST,
        REMOVE_FROM_BLACKLIST,        
        ADD_TRAIN_TO_BLACKLIST,
        REMOVE_TRAIN_FROM_BLACKLIST;
    }
}
