package de.mrjulsen.crn.client.gui.widgets.create;

import de.mrjulsen.crn.util.IngameTimeNumberFormatter;

public class CreateTimeSelectionComponent extends CreateScrollNumberInput {

    public CreateTimeSelectionComponent(int x, int y, int w) {
        super(x, y, w);
        this.step.set(500D);
        this.min.set(0D);
        this.max.set(24000D); // TODO: Time System?
        this.format.set(IngameTimeNumberFormatter.INSTANCE);
    }    
}
