package de.mrjulsen.crn.util;

public interface IFrontierEntry {
    PenaltyResult getPenaltyReasons();
    void setPenaltyReasons(PenaltyResult penalties);
    int getPenalty();
}
