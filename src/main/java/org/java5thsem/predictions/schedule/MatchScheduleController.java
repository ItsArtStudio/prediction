package org.java5thsem.predictions.schedule;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Validated
@RestController
@RequestMapping("/api/leagues/premier-league")
public class MatchScheduleController {

    private final MatchScheduleService matchScheduleService;

    public MatchScheduleController(MatchScheduleService matchScheduleService) {
        this.matchScheduleService = matchScheduleService;
    }

    @GetMapping("/schedule")
    public MatchScheduleResponse getSchedule(
            @RequestParam(required = false)
            @Min(2000)
            @Max(2100)
            Integer season,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to
    ) {
        return matchScheduleService.getSchedule(season, date, from, to);
    }
}
