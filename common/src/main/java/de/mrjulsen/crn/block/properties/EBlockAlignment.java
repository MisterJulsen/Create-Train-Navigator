package de.mrjulsen.crn.block.properties;

import java.util.Arrays;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.mcdragonlib.data.ITranslatableEnum;

public enum EBlockAlignment implements ITranslatableEnum {
	NEGATIVE(-1, "negative"),
	CENTER(0, "center"),
    POSITIVE(1, "positive");
	
	private String name;
	private int index;
	
	private EBlockAlignment(int index, String name) {
		this.name = name;
		this.index = index;
	}
	
	public String getName() {
		return this.name;
	}

	public int getId() {
		return this.index;
	}	

	public static EBlockAlignment getSideById(int index) {
		return Arrays.stream(values()).filter(x -> x.getId() == index).findFirst().orElse(CENTER);
	}

    @Override
    public String getSerializedName() {
        return name;
    }

	@Override
	public Data getTranslationData() {
		return new Data(CreateRailwaysNavigator.MOD_ID, "block_alignment", name);
	}
	
}
