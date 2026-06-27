package org.acme.tennis.solver;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.score.stream.Joiners;
import org.acme.tennis.domain.DomainClasses.*;

public class LeagueConstraintProvider implements ConstraintProvider {

    @Override
    public Constraint[] defineConstraints(ConstraintFactory factory) {
        return new Constraint[] {
                courtConflict(factory),
                playerAvailability(factory),
                rewardMaxMatches(factory),
                rewardMorningTimeslots(factory)
        };
    }

    private Constraint courtConflict(ConstraintFactory factory) {
        return factory.forEachUniquePair(Match.class, Joiners.equal(Match::getCourtTimeslot))
                .filter((m1, m2) -> m1.getCourtTimeslot() != null)
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Court occupied");
    }

    private Constraint playerAvailability(ConstraintFactory factory) {
        return factory.forEach(Match.class)
                .filter(match -> match.getCourtTimeslot() != null)
                .filter(match -> {
                    String dayHour = match.getCourtTimeslot().getDay() + "-" + match.getCourtTimeslot().getHour();
                    return !match.getPlayer1().getAvailableDayHours().contains(dayHour) ||
                            !match.getPlayer2().getAvailableDayHours().contains(dayHour);
                })
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Player unavailable");
    }

    private Constraint rewardMaxMatches(ConstraintFactory factory) {
        return factory.forEach(Match.class)
                .filter(match -> match.getCourtTimeslot() != null)
                .join(LeaguePreferences.class)
                .reward(HardSoftScore.ONE_SOFT, (match, prefs) -> prefs.getWeight("20"))
                .asConstraint("Maximize matches");
    }

    private Constraint rewardMorningTimeslots(ConstraintFactory factory) {
        return factory.forEach(Match.class)
                .filter(match -> match.getCourtTimeslot() != null && match.getCourtTimeslot().getHour() < 12)
                .join(LeaguePreferences.class)
                .reward(HardSoftScore.ONE_SOFT, (match, prefs) -> prefs.getWeight("30"))
                .asConstraint("Morning matches");
    }
}