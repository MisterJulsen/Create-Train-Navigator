package de.mrjulsen.crn.data;

import java.util.Arrays;

import de.mrjulsen.crn.client.gui.ModGuiIcons;
import de.mrjulsen.mcdragonlib.common.ITranslatableEnum;
import net.minecraft.util.StringRepresentable;

public enum EDisplayInfo implements StringRepresentable, ITranslatableEnum{
	SIMPLE(0, "simple", ModGuiIcons.LESS_DETAILS),
    DETAILED(1, "detailed", ModGuiIcons.DETAILED),
	INFORMATIVE(2, "informative", ModGuiIcons.VERY_DETAILED);
	
	private String name;
	private int id;
	private ModGuiIcons icon;
	
	private EDisplayInfo(int id, String name, ModGuiIcons icon) {
		this.name = name;
		this.id = id;
		this.icon = icon;
	}
	
	public String getInfoTypeName() {
		return this.name;
	}

	public int getId() {
		return this.id;
	}

	public ModGuiIcons getIcon() {
		return icon;
	}

	public static EDisplayInfo getTypeById(int id) {
		return Arrays.stream(values()).filter(x -> x.getId() == id).findFirst().orElse(EDisplayInfo.SIMPLE);
	}

    @Override
    public String getSerializedName() {
        return name;
    }

	@Override
	public String getEnumName() {
		return "display_info_type";
	}

	@Override
	public String getEnumValueName() {
		return this.name;
	}
}
