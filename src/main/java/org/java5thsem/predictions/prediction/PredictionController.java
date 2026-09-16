package org.java5thsem.predictions.prediction;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@Validated
@RestController
@RequestMapping("/api/users/{userId}/matches/{matchId}/prediction")
public class PredictionController {

    private final PredictionService predictionService;

    public PredictionController(PredictionService predictionService) {
        this.predictionService = predictionService;
    }

    @PostMapping
    public ResponseEntity<PredictionResponse> submit(
            @PathVariable @Positive Long userId,
            @PathVariable @Positive Long matchId,
            @Valid @RequestBody PredictionRequest request
    ) {
        PredictionResponse created = predictionService.submit(userId, matchId, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().build().toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping
    public PredictionResponse update(
            @PathVariable @Positive Long userId,
            @PathVariable @Positive Long matchId,
            @Valid @RequestBody PredictionRequest request
    ) {
        return predictionService.update(userId, matchId, request);
    }

    @GetMapping
    public PredictionResponse getCurrent(
            @PathVariable @Positive Long userId,
            @PathVariable @Positive Long matchId
    ) {
        return predictionService.getCurrent(userId, matchId);
    }
}
