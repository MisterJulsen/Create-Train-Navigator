package de.mrjulsen.crn.util;

import de.mrjulsen.mcdragonlib.util.time.format.ITimeFormatter;
import de.mrjulsen.mcdragonlib.util.time.format.TimeFormat12Hours;
import de.mrjulsen.mcdragonlib.util.time.format.TimeFormat24Hours;
import de.mrjulsen.mcdragonlib.util.time.format.TimeFormatTicks;

public enum ETimeFormat {
    HOURS_24("hours_24", new TimeFormat24Hours()),
    HOURS_12("hours_12", new TimeFormat12Hours()),
    TICKS("ticks", new TimeFormatTicks());

    private final String name;
    private final ITimeFormatter format;

    private ETimeFormat(String name, ITimeFormatter format) {
        this.name = name;
        this.format = format;
    }

    public String getName() {
        return name;
    }

    public ITimeFormatter getFormat() {
        return format;
    }
}
