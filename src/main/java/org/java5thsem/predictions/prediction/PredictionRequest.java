package org.java5thsem.predictions.prediction;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PredictionRequest(
        @NotNull(message = "homeScore is required")
        @Min(value = 0, message = "homeScore must be 0 or greater")
        @Max(value = 99, message = "homeScore must be 99 or less")
        Integer homeScore,

        @NotNull(message = "awayScore is required")
        @Min(value = 0, message = "awayScore must be 0 or greater")
        @Max(value = 99, message = "awayScore must be 99 or less")
        Integer awayScore
) {
}
