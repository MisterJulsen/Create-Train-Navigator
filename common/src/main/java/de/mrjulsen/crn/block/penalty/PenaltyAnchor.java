package de.mrjulsen.crn.block.penalty;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.simibubi.create.Create;
import com.simibubi.create.content.logistics.filter.FilterItemStack;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.graph.DimensionPalette;
import com.simibubi.create.content.trains.graph.TrackEdge;
import com.simibubi.create.content.trains.graph.TrackGraph;
import com.simibubi.create.content.trains.observer.TrackObserver;
import com.simibubi.create.content.trains.signal.SignalPropagator;

import de.mrjulsen.crn.CRNPlatformSpecific;
import de.mrjulsen.crn.api.core.INavigationPenaltyProvider;
import de.mrjulsen.crn.event.ModCommonEvents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class PenaltyAnchor extends TrackObserver implements INavigationPenaltyProvider {

    public static final int MIN_PENALTY = 0;
    public static final int MAX_PENALTY = 1000;
    public static final int PENALTY_STEP = 10;
    public static final int DEFAULT_PENALTY = 50;
    public static final int ACTIVE_DURATION = 20;

    private static final String NBT_PENALTY = "Penalty";
    private static final String NBT_ACTIVE_TICKS = "ActiveTicks";
    private static final int FILTER_CACHE_PRUNE_INTERVAL = 200;

    private int penalty = DEFAULT_PENALTY;
    private int activeTicks;

    private final Map<UUID, FilterMatch> filterCache = new HashMap<>();
    private int pruneTimer = FILTER_CACHE_PRUNE_INTERVAL;

    private record FilterMatch(int storageVersion, boolean matches) {}

    @Override
    public void keepAlive(Train train) {
        super.keepAlive(train);
        activeTicks = ACTIVE_DURATION;
    }

    @Override
    public boolean isActivated() {
        return activeTicks > 0;
    }

    @Override
    public void blockEntityAdded(BlockEntity blockEntity, boolean front) {
        super.blockEntityAdded(blockEntity, front);
        if (blockEntity instanceof PenaltyAnchorBlockEntity anchor)
            setPenaltyAndNotify(blockEntity.getLevel(), anchor.getEffectivePenalty());
    }

    @Override
    public void tick(TrackGraph graph, boolean preTrains) {
        super.tick(graph, preTrains);
        if (!preTrains)
            return;
        if (activeTicks > 0)
            activeTicks--;
        if (filterCache.isEmpty() || --pruneTimer > 0)
            return;
        pruneTimer = FILTER_CACHE_PRUNE_INTERVAL;
        filterCache.keySet().removeIf(id -> !Create.RAILWAYS.trains.containsKey(id));
    }

    public void setPenaltyAndNotify(Level level, int penalty) {
        int clamped = Mth.clamp(penalty, MIN_PENALTY, MAX_PENALTY);
        if (this.penalty == clamped)
            return;
        this.penalty = clamped;
        this.filterCache.clear();
        notifyTrains(level);
    }

    private void notifyTrains(Level level) {
        TrackGraph graph = Create.RAILWAYS.sided(level).getGraph(level, edgeLocation.getFirst());
        if (graph == null)
            return;
        TrackEdge edge = graph.getConnection(edgeLocation.map(graph::locateNode));
        if (edge == null)
            return;
        SignalPropagator.notifyTrains(graph, edge);
    }

    public int getPenalty() {
        return penalty;
    }

    @Override
    public int getNavigationPenalty(Train train) {
        return penalty > MIN_PENALTY && matchesFilter(train) ? penalty : 0;
    }

    private boolean matchesFilter(Train train) {
        FilterItemStack filter = getFilter();
        if (filter.isEmpty())
            return true;

        Level level = resolveLevel();
        if (level == null)
            return true;

        int storageVersion = 0;
        for (Carriage carriage : train.carriages)
            if (carriage.storage != null)
                storageVersion += carriage.storage.getVersion();

        FilterMatch cached = filterCache.get(train.id);
        if (cached != null && cached.storageVersion() == storageVersion)
            return cached.matches();

        boolean matches = CRNPlatformSpecific.trainCarriesFilteredCargo(level, filter, train);
        filterCache.put(train.id, new FilterMatch(storageVersion, matches));
        return matches;
    }

    private Level resolveLevel() {
        ResourceKey<Level> dimension = getBlockEntityDimension();
        if (dimension == null)
            return null;
        return ModCommonEvents.getCurrentServer().map(server -> (Level) server.getLevel(dimension)).orElse(null);
    }

    @Override
    public void read(CompoundTag nbt, boolean migration, DimensionPalette dimensions) {
        super.read(nbt, migration, dimensions);
        this.penalty = nbt.contains(NBT_PENALTY) ? Mth.clamp(nbt.getInt(NBT_PENALTY), MIN_PENALTY, MAX_PENALTY) : DEFAULT_PENALTY;
        this.activeTicks = Mth.clamp(nbt.getInt(NBT_ACTIVE_TICKS), 0, ACTIVE_DURATION);
    }

    @Override
    public void write(CompoundTag nbt, DimensionPalette dimensions) {
        super.write(nbt, dimensions);
        nbt.putInt(NBT_PENALTY, penalty);
        nbt.putInt(NBT_ACTIVE_TICKS, activeTicks);
    }

}
