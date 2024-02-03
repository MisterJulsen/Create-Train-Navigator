package de.mrjulsen.crn.data;

import java.util.List;

import com.simibubi.create.content.trains.entity.Train;

import de.mrjulsen.crn.config.ModClientConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

public class UserSettings {
    private static final String NBT_TRANSFER_TIME = "TransferTime";
    private static final String NBT_TRAIN_GROUPS = "TrainGroupBlacklist";

    private final int transferTime;
    private final List<? extends String> trainGroupBlacklist;

    public UserSettings() {
        this(ModClientConfig.TRANSFER_TIME.get(), ModClientConfig.TRAIN_GROUP_FILTER_BLACKLIST.get());
    }

    private UserSettings(int transferTime, List<? extends String> trainGroupBlacklist) {
        this.transferTime = transferTime;
        this.trainGroupBlacklist = trainGroupBlacklist;
    }

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.putInt(NBT_TRANSFER_TIME, getTransferTime());
        ListTag list = new ListTag();
        list.addAll(getTrainGroupBlacklist().stream().map(x -> StringTag.valueOf(x)).toList());
        nbt.put(NBT_TRAIN_GROUPS, list);
        return nbt;
    }

    public static UserSettings fromNbt(CompoundTag nbt) {
        return new UserSettings(
            nbt.getInt(NBT_TRANSFER_TIME),
            nbt.getList(NBT_TRAIN_GROUPS, Tag.TAG_STRING).stream().map(x ->  ((StringTag)x).getAsString()).toList()
        );
    }

    public int getTransferTime() {
        return transferTime;
    }

    public List<? extends String> getTrainGroupBlacklist() {
        return trainGroupBlacklist;
    }

    public boolean isTrainExcluded(Train train, GlobalSettings settingsInstance) {
        boolean b = settingsInstance.getTrainGroupsList().stream().filter(x -> getTrainGroupBlacklist().contains(x.getGroupName())).anyMatch(x -> x.contains(train));
        return b;
    }
}
