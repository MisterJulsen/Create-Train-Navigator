package de.mrjulsen.crn.core.navigation;

import java.util.UUID;

import com.simibubi.create.content.trains.entity.Train;

public class TrainData {
    private UUID trainId;
    private String name;

    public TrainData(Train train) {
        this.trainId = train.id;
        this.name = train.name.getString();
    }

    public UUID getId() {
        return trainId;
    }

    public String getName() {
        return name;
    }

    
}
