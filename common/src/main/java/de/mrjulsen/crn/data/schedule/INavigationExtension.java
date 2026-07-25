package de.mrjulsen.crn.data.schedule;

import java.util.Optional;

import de.mrjulsen.crn.util.PenaltyResult;

public interface INavigationExtension {
    Optional<PenaltyResult> getPenaltiesByDirection();
}
