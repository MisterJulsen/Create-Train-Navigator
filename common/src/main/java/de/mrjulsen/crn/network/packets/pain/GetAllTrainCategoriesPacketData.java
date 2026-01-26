package de.mrjulsen.crn.network.packets.pain;

import java.util.ArrayList;
import java.util.List;

import de.mrjulsen.crn.data.TrainCategory;
import de.mrjulsen.crn.data.storage.GlobalSettings;
import de.mrjulsen.mcdragonlib.data.DLStatus;
import de.mrjulsen.mcdragonlib.network.NetworkPacketContext;
import de.mrjulsen.mcdragonlib.network.NetworkPacketData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

public class GetAllTrainCategoriesPacketData extends NetworkPacketData {
    
    private static final String NBT_DATA = "Data";

    private List<TrainCategory> categories = new ArrayList<>();

    public GetAllTrainCategoriesPacketData(DLStatus status) {
        super(status);
    }
    
    public GetAllTrainCategoriesPacketData(List<TrainCategory> categories) {
        super(DLStatus.OK);
        this.categories = categories;
    }

    @Override
    protected void write(CompoundTag nbt) {
        ListTag list = new ListTag();
        for (TrainCategory category : categories) {
            list.add(category.toNbt());
        }
        nbt.put(NBT_DATA, list);
    }

    @Override
    protected void read(CompoundTag nbt) {
        this.categories = nbt.getList(NBT_DATA, Tag.TAG_COMPOUND).stream().map(x -> TrainCategory.fromNbt((CompoundTag)x)).toList();
    }
    

    public List<TrainCategory> getCategories() {
        return categories;
    }

    public static GetAllTrainCategoriesPacketData handle(NetworkPacketContext context) {
        return new GetAllTrainCategoriesPacketData(GlobalSettings.getInstance().getAllTrainCategories());
    }
    
}
