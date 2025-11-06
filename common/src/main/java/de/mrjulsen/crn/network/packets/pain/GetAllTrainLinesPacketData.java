package de.mrjulsen.crn.network.packets.pain;

import java.util.List;

import de.mrjulsen.crn.data.TrainLine;
import de.mrjulsen.crn.data.storage.GlobalSettings;
import de.mrjulsen.mcdragonlib.data.DLStatus;
import de.mrjulsen.mcdragonlib.network.NetworkPacketContext;
import de.mrjulsen.mcdragonlib.network.NetworkPacketData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

public class GetAllTrainLinesPacketData extends NetworkPacketData {
    
    private static final String NBT_DATA = "Data";

    private List<TrainLine> lines;

    public GetAllTrainLinesPacketData(DLStatus status) {
        super(status);
    }
    
    public GetAllTrainLinesPacketData(List<TrainLine> lines) {
        super(DLStatus.OK);
        this.lines = lines;
    }

    @Override
    protected void write(CompoundTag nbt) {
        ListTag list = new ListTag();
        for (TrainLine line : lines) {
            list.add(line.toNbt());
        }
        nbt.put(NBT_DATA, list);
    }

    @Override
    protected void read(CompoundTag nbt) {
        this.lines = nbt.getList(NBT_DATA, Tag.TAG_COMPOUND).stream().map(x -> TrainLine.fromNbt((CompoundTag)x)).toList();
    }

    public List<TrainLine> getLines() {
        return lines;
    }

    public static GetAllTrainLinesPacketData handle(NetworkPacketContext context) {
        return new GetAllTrainLinesPacketData(GlobalSettings.getInstance().getAllTrainLines());
    }
    
}
