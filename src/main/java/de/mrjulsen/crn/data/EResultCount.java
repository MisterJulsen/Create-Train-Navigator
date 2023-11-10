package de.mrjulsen.crn.data;

import net.minecraft.util.StringRepresentable;

public enum EResultCount implements StringRepresentable, ITranslatableEnum {
	ALL(0, "all"),
	BEST(1, "best"),
    FIXED_AMOUNT(2, "fixed_amount");
	
	private String name;
	private int count;
	
	private EResultCount(int count, String name) {
		this.name = name;
		this.count = count;
	}
	
	public String getCriteriaName() {
		return this.name;
	}

	public int getId() {
		return this.count;
	}	

	public static EResultCount getCriteriaById(int count) {
		for (EResultCount shape : EResultCount.values()) {
			if (shape.getId() == count) {
				return shape;
			}
		}
		return EResultCount.ALL;
	}

    @Override
    public String getSerializedName() {
        return name;
    }

	@Override
	public String getNameOfEnum() {
		return "result_count";
	}

	@Override
	public String getValue() {
		return this.name;
	}
}
