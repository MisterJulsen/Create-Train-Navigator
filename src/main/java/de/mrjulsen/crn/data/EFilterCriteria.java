package de.mrjulsen.crn.data;

import net.minecraft.util.StringRepresentable;

public enum EFilterCriteria implements StringRepresentable, ITranslatableEnum {
	TRANSFER_COUNT(0, "transfer_count"),
    DURATION(1, "duration"),
	STOPOVERS(2, "stopovers");
	
	private String name;
	private int count;
	
	private EFilterCriteria(int count, String name) {
		this.name = name;
		this.count = count;
	}
	
	public String getCriteriaName() {
		return this.name;
	}

	public int getId() {
		return this.count;
	}	

	public static EFilterCriteria getCriteriaById(int count) {
		for (EFilterCriteria shape : EFilterCriteria.values()) {
			if (shape.getId() == count) {
				return shape;
			}
		}
		return EFilterCriteria.TRANSFER_COUNT;
	}

    @Override
    public String getSerializedName() {
        return name;
    }

	@Override
	public String getNameOfEnum() {
		return "filter_criteria";
	}

	@Override
	public String getValue() {
		return this.name;
	}

	public static int getDataFromRoute(EFilterCriteria criteria, Route route) {
		switch (criteria) {
			case DURATION:
				return route.getTotalDuration();
			case STOPOVERS:
				return route.getStationCount();
			case TRANSFER_COUNT:
			default:
				return route.getTransferCount();
		}
	}
}
