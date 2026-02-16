package de.mrjulsen.crn.network.packets.pain;

import java.util.Collection;
import de.mrjulsen.crn.data.StationTag;
import de.mrjulsen.crn.data.storage.GlobalSettings;
import de.mrjulsen.crn.data.train.TrainUtils;
import de.mrjulsen.mcdragonlib.data.DLStatus;
import de.mrjulsen.mcdragonlib.network.NetworkPacketContext;
import de.mrjulsen.mcdragonlib.network.NetworkPacketData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

public class GetAllStationsAsTagsPacketData {

    private static final String NBT_DATA = "Data";
    
    private static final String NBT_EXCLUDE_BLACKLISTED = "ExcludeBlacklisted";

    public static class Request extends NetworkPacketData {
        
        private boolean excludeBlacklisted;

        public Request(DLStatus status) {
            super(status);
        }

        public Request(boolean excludeBlacklisted) {
            super(DLStatus.OK);
            this.excludeBlacklisted = excludeBlacklisted;
        }

        @Override
        protected void write(CompoundTag nbt) {
            nbt.putBoolean(NBT_EXCLUDE_BLACKLISTED, excludeBlacklisted);
        }

        @Override
        protected void read(CompoundTag nbt) {
            this.excludeBlacklisted = nbt.getBoolean(NBT_EXCLUDE_BLACKLISTED);
        }
    }

    public static class Response extends NetworkPacketData {
        private Collection<StationTag> tags;

        public Response(DLStatus status) {
            super(status);
        }

        public Response(Collection<StationTag> tags) {
            super(DLStatus.OK);
            this.tags = tags;
        }

        @Override
        protected void write(CompoundTag nbt) {
            ListTag list = new ListTag();
            for (StationTag tag : tags) {
                list.add(tag.toNbt());
            }
            nbt.put(NBT_DATA, list);
        }

        @Override
        protected void read(CompoundTag nbt) {
            this.tags = nbt.getList(NBT_DATA, Tag.TAG_COMPOUND).stream().map(x -> StationTag.fromNbt((CompoundTag)x, null)).toList();
        }

        public Collection<StationTag> getTags() {
            return tags;
        }
    }

    public static Response handle(Request packet, NetworkPacketContext context) {
        return new Response(TrainUtils.getAllStations().stream()
                .filter(x -> !packet.excludeBlacklisted || !GlobalSettings.getInstance().isStationBlacklisted(x))
                .map(x -> GlobalSettings.getInstance().getOrCreateStationTagFor(x)).distinct()
                .sorted((a, b) -> a.getTagName().get().compareToIgnoreCase(b.getTagName().get())).toList());
    }

}
