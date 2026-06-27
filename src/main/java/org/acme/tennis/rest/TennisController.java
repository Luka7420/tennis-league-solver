package org.acme.tennis.rest;

import ai.timefold.solver.core.api.solver.SolverJob;
import ai.timefold.solver.core.api.solver.SolverManager;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import org.acme.tennis.domain.DomainClasses.*;
import org.acme.tennis.solver.LeagueConstraintProvider;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/schedule")
public class TennisController {

    @PostMapping("/solve")
    public List<Object> solveSchedule(@RequestBody InputRequest input) {
        long startTime = System.currentTimeMillis();

        // 1. Map Preferences
        Map<String, Integer> weights = new HashMap<>();
        for (InputPriority p : input.prioriteti) {
            weights.put(p.id, Integer.parseInt(p.priority));
        }
        LeaguePreferences prefs = new LeaguePreferences(weights);

        // 2. Map Courts
        List<CourtTimeslot> courts = new ArrayList<>();
        for (InputTermin t : input.terminiKluba) {
            courts.add(new CourtTimeslot(
                    t.dan + "-" + t.sat + "-" + t.teren,
                    Integer.parseInt(t.dan),
                    Integer.parseInt(t.sat),
                    t.teren));
        }

        // 3. Map Players
        Map<String, Player> playerMap = new HashMap<>();
        List<Player> players = new ArrayList<>();
        for (InputPlayer p : input.igraci) {
            Set<String> available = p.terminiIgraca.stream()
                    .map(t -> t.dan + "-" + t.sat).collect(Collectors.toSet());
            Player player = new Player(p.playerId, Integer.parseInt(p.zeliIgrati), Integer.parseInt(p.preostaloMeceva),
                    available);
            playerMap.put(p.playerId, player);
            players.add(player);
        }

        // 4. Map and Deduplicate Matches
        Set<String> processedPairs = new HashSet<>();
        List<Match> matches = new ArrayList<>();
        for (InputPlayer p : input.igraci) {
            Player p1 = playerMap.get(p.playerId);
            for (String oppId : p.potencijalniProtivnici) {
                Player p2 = playerMap.get(oppId);
                if (p2 == null)
                    continue;
                String pairKey = p1.getId().compareTo(p2.getId()) < 0 ? p1.getId() + "_" + p2.getId()
                        : p2.getId() + "_" + p1.getId();
                if (!processedPairs.contains(pairKey)) {
                    matches.add(new Match(pairKey, p1, p2));
                    processedPairs.add(pairKey);
                }
            }
        }

        LeagueSchedule problem = new LeagueSchedule(prefs, players, courts, matches);

        // 5. Configure and Run Solver
        int timeoutSeconds = Integer.parseInt(input.timeout);
        SolverConfig config = new SolverConfig()
                .withSolutionClass(LeagueSchedule.class)
                .withEntityClasses(Match.class)
                .withConstraintProviderClass(LeagueConstraintProvider.class)
                .withTerminationConfig(new TerminationConfig().withSpentLimit(Duration.ofSeconds(timeoutSeconds)));

        LeagueSchedule solution;
        try (SolverManager<LeagueSchedule, String> solverManager = SolverManager.create(config)) {
            SolverJob<LeagueSchedule, String> job = solverManager.solve(UUID.randomUUID().toString(), problem);
            solution = job.getFinalBestSolution();
        } catch (Exception e) {
            throw new RuntimeException("Solver was interrupted or failed!", e);
        }

        long endTime = System.currentTimeMillis();

        // 6. Build Output
        return buildOutput(solution, startTime, endTime);
    }

    private List<Object> buildOutput(LeagueSchedule solution, long start, long end) {
        List<Object> output = new ArrayList<>();
        List<Match> played = solution.getMatches().stream().filter(m -> m.getCourtTimeslot() != null).toList();

        for (Match m : played) {
            output.add(Map.of(
                    "player1Id", m.getPlayer1().getId(), "player2Id", m.getPlayer2().getId(),
                    "courtID", m.getCourtTimeslot().getCourtId(),
                    "dayPlayed", String.valueOf(m.getCourtTimeslot().getDay()), "hourPlayed",
                    String.valueOf(m.getCourtTimeslot().getHour())));
        }

        output.add(Map.of("pocetakAlgoritmaTimeStamp", start, "krajAlgoritmaTimeStamp", end));

        List<Map<String, Object>> playerStats = new ArrayList<>();
        for (Player p : solution.getPlayers()) {
            int pot = (int) solution.getMatches().stream()
                    .filter(m -> m.getPlayer1().equals(p) || m.getPlayer2().equals(p)).count();
            int pld = (int) played.stream().filter(m -> m.getPlayer1().equals(p) || m.getPlayer2().equals(p)).count();
            playerStats.add(Map.of("playerID", p.getId(), "potentialMatches", pot, "playedMatches", pld));
        }

        long usedCourts = played.stream().map(m -> m.getCourtTimeslot().getId()).distinct().count();
        long usedSlots = played.stream().map(m -> m.getCourtTimeslot().getDay() + "-" + m.getCourtTimeslot().getHour())
                .distinct().count();
        long specSlots = solution.getCourtTimeslots().stream().map(c -> c.getDay() + "-" + c.getHour()).distinct()
                .count();

        output.add(Map.of(
                "players", playerStats,
                "club", Map.of("specifiedTimeslots", specSlots, "specifiedCourts", solution.getCourtTimeslots().size(),
                        "usedTimeslots", usedSlots, "usedCourts", usedCourts)));

        return output;
    }
}