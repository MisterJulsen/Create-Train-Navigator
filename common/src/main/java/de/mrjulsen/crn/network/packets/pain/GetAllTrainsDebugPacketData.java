package de.mrjulsen.crn.network.packets.pain;

import java.util.List;

import de.mrjulsen.crn.data.train.TrainListener;
import de.mrjulsen.crn.debug.TrainDebugData;
import de.mrjulsen.mcdragonlib.data.DLStatus;
import de.mrjulsen.mcdragonlib.network.NetworkPacketContext;
import de.mrjulsen.mcdragonlib.network.NetworkPacketData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

public class GetAllTrainsDebugPacketData extends NetworkPacketData {

    private static final String NBT_DATA = "Data";
    private List<TrainDebugData> data;

    public GetAllTrainsDebugPacketData(DLStatus status) {
        super(status);
    }

    public GetAllTrainsDebugPacketData(List<TrainDebugData> data) {
        super(DLStatus.OK);
        this.data = data;
    }

    @Override
    protected void write(CompoundTag nbt) {
        ListTag list = new ListTag();
        for (TrainDebugData d : data) {
            list.add(d.toNbt());
        }
        nbt.put(NBT_DATA, list);
    }

    @Override
    protected void read(CompoundTag nbt) {
        this.data = nbt.getList(NBT_DATA, Tag.TAG_STRING).stream().map(x -> TrainDebugData.fromNbt((CompoundTag)x)).toList();
    }

    public List<TrainDebugData> getData() {
        return data;
    }

    public static GetAllTrainsDebugPacketData handle(NetworkPacketContext context) {
        return new GetAllTrainsDebugPacketData(TrainListener.getAllTrainData().stream().map(x -> TrainDebugData.fromTrain(x)).toList());
    }
}
