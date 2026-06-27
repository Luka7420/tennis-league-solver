package org.acme.tennis.domain;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.solution.*;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.*;

public class DomainClasses {

    // --- Input JSON DTOs ---
    public static class InputRequest {
        @JsonProperty("VREME")
        public String vreme;
        @JsonProperty("TIMEOUT")
        public String timeout;
        @JsonProperty("DOZVOLI_IGRACU_VISE_MECEVA_U_ISTOM_DANU")
        public String dozvoliViseMeceva;
        @JsonProperty("PRIORITETI")
        public List<InputPriority> prioriteti;
        @JsonProperty("TERMINI_KLUBA")
        public List<InputTermin> terminiKluba;
        @JsonProperty("IGRACI")
        public List<InputPlayer> igraci;
    }

    public static class InputPriority {
        public String id;
        public String name;
        public String priority;
    }

    public static class InputTermin {
        public String dan;
        public String sat;
        public String teren;
    }

    public static class InputPlayer {
        @JsonProperty("PLAYER_ID")
        public String playerId;
        @JsonProperty("PLAYER_NAME")
        public String playerName;
        @JsonProperty("ZELI_IGRATI_MECEVA")
        public String zeliIgrati;
        @JsonProperty("PREOSTALO_MECEVA")
        public String preostaloMeceva;
        @JsonProperty("TERMINI_IGRACA")
        public List<InputTermin> terminiIgraca;
        @JsonProperty("POTENCIJALNI_PROTIVNICI")
        public List<String> potencijalniProtivnici;
    }

    // --- Timefold Domain Models ---
    public static class CourtTimeslot {
        private String id;
        private int day;
        private int hour;
        private String courtId;

        public CourtTimeslot(String id, int day, int hour, String courtId) {
            this.id = id;
            this.day = day;
            this.hour = hour;
            this.courtId = courtId;
        }

        public String getId() {
            return id;
        }

        public int getDay() {
            return day;
        }

        public int getHour() {
            return hour;
        }

        public String getCourtId() {
            return courtId;
        }
    }

    public static class Player {
        private String id;
        private int wantedMatches;
        private int remainingMatches;
        private Set<String> availableDayHours;

        public Player(String id, int wantedMatches, int remainingMatches, Set<String> availableDayHours) {
            this.id = id;
            this.wantedMatches = wantedMatches;
            this.remainingMatches = remainingMatches;
            this.availableDayHours = availableDayHours;
        }

        public String getId() {
            return id;
        }

        public int getWantedMatches() {
            return wantedMatches;
        }

        public int getRemainingMatches() {
            return remainingMatches;
        }

        public Set<String> getAvailableDayHours() {
            return availableDayHours;
        }
    }

    public static class LeaguePreferences {
        private Map<String, Integer> weights;

        public LeaguePreferences(Map<String, Integer> weights) {
            this.weights = weights;
        }

        public int getWeight(String id) {
            return weights.getOrDefault(id, 0);
        }
    }

    @PlanningEntity
    public static class Match {
        @PlanningId
        private String id;
        private Player player1;
        private Player player2;

        @PlanningVariable(allowsUnassigned = true)
        private CourtTimeslot courtTimeslot;

        public Match() {
        }

        public Match(String id, Player player1, Player player2) {
            this.id = id;
            this.player1 = player1;
            this.player2 = player2;
        }

        public String getId() {
            return id;
        }

        public Player getPlayer1() {
            return player1;
        }

        public Player getPlayer2() {
            return player2;
        }

        public CourtTimeslot getCourtTimeslot() {
            return courtTimeslot;
        }

        public void setCourtTimeslot(CourtTimeslot courtTimeslot) {
            this.courtTimeslot = courtTimeslot;
        }
    }

    @PlanningSolution
    public static class LeagueSchedule {
        @ProblemFactProperty
        private LeaguePreferences preferences;
        @ProblemFactCollectionProperty
        private List<Player> players;
        @ValueRangeProvider
        @ProblemFactCollectionProperty
        private List<CourtTimeslot> courtTimeslots;
        @PlanningEntityCollectionProperty
        private List<Match> matches;
        @PlanningScore
        private HardSoftScore score;

        public LeagueSchedule() {
        }

        public LeagueSchedule(LeaguePreferences prefs, List<Player> players, List<CourtTimeslot> slots,
                List<Match> matches) {
            this.preferences = prefs;
            this.players = players;
            this.courtTimeslots = slots;
            this.matches = matches;
        }

        public List<Player> getPlayers() {
            return players;
        }

        public List<CourtTimeslot> getCourtTimeslots() {
            return courtTimeslots;
        }

        public List<Match> getMatches() {
            return matches;
        }

        public HardSoftScore getScore() {
            return score;
        }

        public void setScore(HardSoftScore score) {
            this.score = score;
        }
    }
}