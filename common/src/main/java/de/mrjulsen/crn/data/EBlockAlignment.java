package de.mrjulsen.crn.data;

import java.util.Arrays;

import de.mrjulsen.mcdragonlib.core.ITranslatableEnum;
import net.minecraft.util.StringRepresentable;

public enum EBlockAlignment implements StringRepresentable, ITranslatableEnum {
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
	public String getEnumName() {
		return "block_alignment";
	}

	@Override
	public String getEnumValueName() {
		return name;
	}
}
