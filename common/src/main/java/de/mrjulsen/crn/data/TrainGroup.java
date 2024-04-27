package de.mrjulsen.crn.data;

import java.util.Date;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.simibubi.create.content.trains.entity.Train;

import de.mrjulsen.mcdragonlib.DragonLib;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

public class TrainGroup {

    private static final String NBT_NAME = "Name";
    private static final String NBT_TRAIN_NAMES = "Trains";
    private static final String NBT_LAST_EDITOR = "LastEditor";
    private static final String NBT_LAST_EDITED_TIME = "LastEditedTimestamp";

    private String name;
    private Set<String> trainNames = new HashSet<>();
    
    protected String lastEditorName = null;
    protected long lastEditedTime = 0;

    public TrainGroup(String name) {
        this.name = name;
    }

    public TrainGroup(String name, Set<String> initialValues) {
        this.name = name;
        this.trainNames = initialValues;
    }

    public void addTrain(Train train) {
        addTrain(train.name.getString());
    }

    public void addTrain(String trainName) {
        trainNames.add(trainName);
    }

    public Set<String> getTrainNames() {
        return trainNames;
    }

    public boolean contains(Train train) {
        return trainNames.stream().anyMatch(x -> x.equals(train.name.getString()));
    }

    public boolean contains(String trainName) {
        return trainNames.stream().anyMatch(x -> x.equals(trainName));
    }

    public void setGroupName(String name) {
        this.name = name;
    }

    public String getGroupName() {
        return name;
    }

    public void update(TrainGroup newData) {
        this.name = newData.name;
        this.trainNames = newData.trainNames;
        this.lastEditedTime = newData.lastEditedTime;
        this.lastEditorName = newData.lastEditorName;
    }

    public void add(String trainName) {
        trainNames.add(trainName);
    }

    public void addAll(Set<String> trainNames) {
        this.trainNames.addAll(trainNames);
    }

    public void add(Train train) {
        add(train.name.getString());
    }

    public void addAllTrains(Set<Train> trains) {
        this.trainNames.addAll(trains.stream().map(x -> x.name.getString()).toList());
    }

    public void remove(Train train) {
        remove(train.name.getString());
    }

    public void remove(String trainName) {
        trainNames.removeIf(x -> x.equals(trainName));
    }

    public String getLastEditorName() {
        return lastEditorName;
    }

    public void updateLastEdited(String name) {
        this.lastEditorName = name;
        this.lastEditedTime = new Date().getTime();
    }

    public Date getLastEditedTime() {
        return new Date(lastEditedTime);
    }

    public String getLastEditedTimeFormatted() {
        return DragonLib.DATE_FORMAT.format(getLastEditedTime());
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, trainNames);
    }

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        
        if (lastEditorName != null) {
            nbt.putString(NBT_LAST_EDITOR, getLastEditorName());
        }
        nbt.putLong(NBT_LAST_EDITED_TIME, lastEditedTime);
        nbt.putString(NBT_NAME, getGroupName());
        ListTag tag = new ListTag();
        tag.addAll(getTrainNames().stream().map(x -> StringTag.valueOf(x)).toList());
        nbt.put(NBT_TRAIN_NAMES, tag);

        return nbt;
    }

    public static TrainGroup fromNbt(CompoundTag nbt) {
        String groupName = nbt.getString(NBT_NAME);
        Set<String> trainNames = new HashSet<>(nbt.getList(NBT_TRAIN_NAMES, Tag.TAG_STRING).stream().map(x -> ((StringTag)x).getAsString()).collect(Collectors.toSet()));        
        String lastEditorName = nbt.contains(NBT_LAST_EDITOR) ? nbt.getString(NBT_LAST_EDITOR) : null;
        long lastEditedTime = nbt.getLong(NBT_LAST_EDITED_TIME);
        TrainGroup group = new TrainGroup(groupName, trainNames);
        group.lastEditedTime = lastEditedTime;
        group.lastEditorName = lastEditorName;

        return group;
    }
}
