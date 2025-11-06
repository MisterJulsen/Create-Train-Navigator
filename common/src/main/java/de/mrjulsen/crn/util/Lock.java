package de.mrjulsen.crn.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableSet;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.client.ClientWrapper;
import de.mrjulsen.crn.client.gui.ModGuiIcons;
import de.mrjulsen.crn.config.ModCommonConfig;
import de.mrjulsen.crn.exceptions.RuntimeSideException;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.client.util.DLSprite;
import de.mrjulsen.mcdragonlib.data.IIterableEnum;
import de.mrjulsen.mcdragonlib.data.ITranslatableEnum;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import dev.architectury.utils.GameInstance;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;

public class Lock {

    public static enum LockState implements ITranslatableEnum, IIterableEnum<LockState> {
        UNLOCKED("unlocked", (byte)0, ModGuiIcons.UNLOCKED, (key) -> key.withStyle(ChatFormatting.GREEN)),
        //TRUSTED("trusted", (byte)1, ModGuiIcons.TRUSTED, (key) -> TextUtils.translate(key).withStyle(ChatFormatting.GOLD)),
        LOCKED("locked", Byte.MAX_VALUE, ModGuiIcons.LOCKED, (key) -> key.withStyle(ChatFormatting.RED));

        private final String name;
        private final byte index;
        private final ModGuiIcons icon;
        private final UnaryOperator<MutableComponent> text;

        private LockState(String name, byte index, ModGuiIcons icon, UnaryOperator<MutableComponent> text) {
            this.name = name;
            this.index = index;
            this.icon = icon;
            this.text = text;
        }

        public byte getIndex() {
            return index;
        }

        public String getName() {
            return name;
        }

        public Component getFormattedText() {
            return text.apply(getValueTranslation());
        }

        public DLSprite getIcon() {
            return icon.getAsSprite(16, 16);
        }

        public static LockState getByIndex(int i) {
            return Arrays.stream(values()).filter(x -> x.getIndex() == i).findFirst().orElse(UNLOCKED);
        }

        @Override
        public LockState[] getValues() {
            return values();
        }

        @Override
        public Data getTranslationData() {
            return new Data(CreateRailwaysNavigator.MOD_ID, "lock_state", name);
        }
    }

    private static final String NBT_STATE = "State";
    private static final String NBT_TRUSTED = "Trusted";

    public static final String TRANSLATION_KEY_TRUSTED_PLAYERS = "gui." + CreateRailwaysNavigator.MOD_ID + ".lock.trusted_players";
    public static final String TRANSLATION_KEY_TRANSFER_OWNERSHIP = "gui." + CreateRailwaysNavigator.MOD_ID + ".lock.transfer_ownership";
    
    private final MutableComponent charAllowed = TextUtils.text("\u2714").withStyle(ChatFormatting.GREEN);
    private final MutableComponent charTrusted = TextUtils.text("\u2714").withStyle(ChatFormatting.GOLD);
    private final MutableComponent charLocked = TextUtils.text("\u274C").withStyle(ChatFormatting.RED);

    private final MutableComponent txtPermissions = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".lock.permissions");
    private final MutableComponent txtRightClickOptions = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".lock.right_click_options").withStyle(ChatFormatting.DARK_GRAY).withStyle(ChatFormatting.ITALIC);
    private final MutableComponent txtNoOwner = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".lock.no_owner");
    private final String keyStatus = "gui." + CreateRailwaysNavigator.MOD_ID + ".lock.state";
    private final String keyOwner = "gui." + CreateRailwaysNavigator.MOD_ID + ".lock.owner";
    
    private Owner owner;
    private LockState state = LockState.UNLOCKED;
    private final Set<Owner> trusted = new HashSet<>();


    private Lock(LockState status, Owner owner, Set<Owner> trusted) {
        this.state = status;
        this.owner = owner;
        this.trusted.addAll(trusted);
    }

    public Lock(Owner owner) {
        this(LockState.UNLOCKED, owner, new HashSet<>());
    }

    public Lock() {
        this(LockState.UNLOCKED, null, new HashSet<>());
    }

    public void set(LockState state) {
        this.state = state;
    }

    public LockState get() {
        return state;
    }

    /**
     * @throws RuntimeSideException Server-side only!
     */
    public boolean isAllowed(Owner target) throws RuntimeSideException {
        if (!DragonLib.hasServer()) {
            throw new RuntimeSideException(false);
        }
        return this.owner == null || isAdmin(target) || (this.owner.equals(target) || switch (state) {
            case LOCKED -> isTrusted(target);
            default -> true;
        });
    }
    
    /**
     * @throws RuntimeSideException Server-side only!
     */
    public boolean isAdmin(Owner target) throws RuntimeSideException {
        if (!DragonLib.hasServer()) {
            throw new RuntimeSideException(false);
        }
        return this.owner != null && (this.owner.equals(target) || (ModCommonConfig.GLOBAL_SETTINGS_ADMIN_PERMISSION_LEVEL.get() >= 0 && GameInstance.getServer().getPlayerList().getPlayer(target.uuid()).hasPermissions(ModCommonConfig.GLOBAL_SETTINGS_ADMIN_PERMISSION_LEVEL.get())));
    }

    /**
     * @throws RuntimeSideException Client-side only!
     */
    public boolean isAllowed() throws RuntimeSideException {
        if (Platform.getEnvironment() != Env.CLIENT) {
            throw new RuntimeSideException(true);
        }
        Owner self = ClientWrapper.getMe();
        return this.owner == null || isAdmin() || (this.owner.equals(self) || switch (state) {
            case LOCKED -> isTrusted(self);
            default -> true;
        });
    }
    
    /**
     * @throws RuntimeSideException Client-side only!
     */
    public boolean isAdmin() throws RuntimeSideException {
        if (Platform.getEnvironment() != Env.CLIENT) {
            throw new RuntimeSideException(true);
        }
        Owner self = ClientWrapper.getMe();
        return this.owner != null && (this.owner.equals(self) || (ModCommonConfig.GLOBAL_SETTINGS_ADMIN_PERMISSION_LEVEL.get() >= 0 && ClientWrapper.getClientPlayer().hasPermissions(ModCommonConfig.GLOBAL_SETTINGS_ADMIN_PERMISSION_LEVEL.get())));
    }

    public Set<Owner> getTrusted() {
        return ImmutableSet.copyOf(this.trusted);
    }

    public void addTrusted(Owner target) {
        this.trusted.add(target);
    }

    public void updateTrusted(Set<Owner> targets) {
        this.trusted.clear();
        this.trusted.addAll(targets);
    }

    public boolean isTrusted(Owner target) {
        return this.trusted.contains(target);
    }

    public void removeTrusted(Owner target) {
        this.trusted.remove(target);
    }

    public Optional<Owner> getOwner() {
        return Optional.ofNullable(owner);
    }

    public void setOwner(Owner newOwner) {
        this.owner = newOwner;
    }
    
    public List<FormattedText> asText(Owner target) {
        List<FormattedText> texts = new ArrayList<>(4);
        texts.add(TextUtils.empty().append(txtPermissions).append(" ").append(isTrusted(target) ? charTrusted : (isAllowed() ? charAllowed : charLocked)));
        texts.add(TextUtils.translate(keyStatus, get().getFormattedText()).withStyle(ChatFormatting.GRAY));
        texts.add(TextUtils.translate(keyOwner, getOwner().map(x -> x.name().isBlank() ? txtNoOwner : TextUtils.text(x.name()).withStyle(ChatFormatting.GREEN)).orElse(txtNoOwner)).withStyle(ChatFormatting.GRAY));        
        if (isAdmin()) {
            texts.add(txtRightClickOptions);
        }
        return texts;
    }

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.putByte(NBT_STATE, state.getIndex());
        if (owner != null) owner.toNbt(nbt);
        ListTag list = new ListTag();
        for (Owner t : trusted) {
            list.add(t.toNbt());
        }
        nbt.put(NBT_TRUSTED, list);
        return nbt;
    }

    public static Lock fromNbt(CompoundTag nbt) {
        return new Lock(
            LockState.getByIndex(nbt.getByte(NBT_STATE)),
            Owner.fromNbt(nbt),
            nbt.getList(NBT_TRUSTED, Tag.TAG_COMPOUND).stream().map(x -> Owner.fromNbt((CompoundTag)x)).collect(Collectors.toSet())
        );
    }
}
