package de.mrjulsen.crn.data;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import de.mrjulsen.crn.util.Lock;
import de.mrjulsen.crn.util.Owner;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.util.DLColor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;

public class TrainCategory {

    public static final int MAX_NAME_LENGTH = 32;

    private static final String NBT_ID = "Id";
    private static final String NBT_NAME = "Name";
    private static final String NBT_COLOR = "Color";
    private static final String NBT_LAST_EDITOR = "LastEditor";
    private static final String NBT_LAST_EDITED_TIME = "LastEditedTimestamp";
    private static final String NBT_OWNER = "Owner";

    private final UUID id;
    private String name;
    private DLColor color = DLColor.TRANSPARENT;
    
    protected final Lock owner;
    protected Owner lastEditor;
    protected long lastEditedTime = 0;

    
    public TrainCategory(UUID id, String name, Player player) {
        this(id, name, new Owner(player));
    }

    public TrainCategory(UUID id, String name, Owner owner) {
        this(id, name, new Lock(owner));
    }

    protected TrainCategory(UUID id, String name, Lock owner) {
        this.id = id;
        this.name = name;
        this.owner = owner;
        owner.getOwner().ifPresent(this::updateLastEdited);
    }

    public static UUID genMD5Uuid(String name) {
        byte[] nameBytes = name.getBytes(StandardCharsets.UTF_8);
        return UUID.nameUUIDFromBytes(nameBytes);
    }

    public UUID getId() {
        return id;
    }

    public String getCategoryName() {
        return name;
    }

    public DLColor getColor() {
        return color;
    }

    public void setColor(DLColor color) {
        this.color = color;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Lock getOwner() {
        return owner;
    }

    public Optional<Owner> getLastEditor() {
        return Optional.ofNullable(lastEditor);
    }

    private void updateLastEdited(Owner editor) {
        this.lastEditor = editor;
        this.lastEditedTime = new Date().getTime();
    }

    public void updateLastEdited(Player player) {
        this.updateLastEdited(new Owner(player));
    }

    public Date getLastEditedTime() {
        return new Date(lastEditedTime);
    }

    public String getLastEditedTimeFormatted() {
        return DragonLib.DATE_FORMAT.format(getLastEditedTime());
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TrainCategory o) {
            return name.equals(o.name);
        }
        return false;
    }

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        
        nbt.putUUID(NBT_ID, id);
        getLastEditor().ifPresent(x -> nbt.put(NBT_LAST_EDITOR, x.toNbt()));
        nbt.putLong(NBT_LAST_EDITED_TIME, lastEditedTime);
        nbt.put(NBT_OWNER, owner.toNbt());
        nbt.putString(NBT_NAME, getCategoryName());
        nbt.putInt(NBT_COLOR, getColor().getAsARGB());

        return nbt;
    }

    public static TrainCategory fromNbt(CompoundTag nbt) {
        String name = nbt.getString(NBT_NAME);
        UUID id = nbt.contains(NBT_ID) ? nbt.getUUID(NBT_ID) : genMD5Uuid(name);
        
        Owner lastEditor = nbt.contains(NBT_LAST_EDITOR) && nbt.getTagType(NBT_LAST_EDITOR) == Tag.TAG_COMPOUND ? Owner.fromNbt(nbt.getCompound(NBT_LAST_EDITOR)) : null;
        long lastEditedTime = nbt.getLong(NBT_LAST_EDITED_TIME);
        Lock owner = nbt.contains(NBT_OWNER) && nbt.getTagType(NBT_OWNER) == Tag.TAG_COMPOUND ? Lock.fromNbt(nbt.getCompound(NBT_OWNER)) : new Lock(new Owner((UUID)null));

        TrainCategory category = new TrainCategory(id, name, owner);
        category.setColor(DLColor.fromInt(nbt.getInt(NBT_COLOR)));
        category.lastEditor = lastEditor;
        category.lastEditedTime = lastEditedTime;
        return category;
    }
}
