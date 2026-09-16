package org.java5thsem.predictions.live;

public record LineupPlayerResponse(
        long id,
        String name,
        Integer number,
        String position,
        String grid
) {
}
