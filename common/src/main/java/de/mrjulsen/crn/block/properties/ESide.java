package de.mrjulsen.crn.block.properties;

import java.util.Arrays;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.mcdragonlib.data.ITranslatableEnum;

public enum ESide implements ITranslatableEnum {
	FRONT(0, "front"),
    BOTH(2, "both");
	
	private String name;
	private int index;
	
	private ESide(int index, String name) {
		this.name = name;
		this.index = index;
	}
	
	public String getName() {
		return this.name;
	}

	public int getId() {
		return this.index;
	}	

	public static ESide getSideById(int index) {
		return Arrays.stream(values()).filter(x -> x.getId() == index).findFirst().orElse(FRONT);
	}

    @Override
    public String getSerializedName() {
        return name;
    }

	@Override
	public Data getTranslationData() {
		return new Data(CreateRailwaysNavigator.MOD_ID, "side", name);
	}
}
