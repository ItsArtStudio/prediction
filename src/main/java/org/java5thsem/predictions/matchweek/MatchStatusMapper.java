package org.java5thsem.predictions.matchweek;

import org.java5thsem.predictions.provider.ProviderFixture;

import java.util.Locale;
import java.util.Set;

public final class MatchStatusMapper {

    private static final Set<String> LIVE = Set.of("1H", "HT", "2H", "ET", "BT", "P", "INT", "LIVE");
    private static final Set<String> FINISHED = Set.of("FT", "AET", "PEN");
    private static final Set<String> SCHEDULED = Set.of("NS", "TBD");

    private MatchStatusMapper() {
    }

    public static boolean isLive(String statusShort) {
        String status = matchStatus(statusShort);
        return "LIVE".equals(status) || "SUSPENDED".equals(status);
    }

    public static String matchStatus(String statusShort) {
        String code = normalize(statusShort);
        if (code == null) {
            return "UNKNOWN";
        }
        if (LIVE.contains(code)) {
            return "LIVE";
        }
        if (FINISHED.contains(code)) {
            return "FINISHED";
        }
        if (SCHEDULED.contains(code)) {
            return "SCHEDULED";
        }
        return switch (code) {
            case "PST" -> "POSTPONED";
            case "CANC" -> "CANCELLED";
            case "SUSP" -> "SUSPENDED";
            case "ABD" -> "ABANDONED";
            case "AWD", "WO" -> "AWARDED";
            default -> "UNKNOWN";
        };
    }

    static String matchweekStatus(Iterable<ProviderFixture> fixtures) {
        boolean any = false;
        boolean anyLive = false;
        boolean anyOpen = false;
        boolean anyFinished = false;
        for (ProviderFixture fixture : fixtures) {
            any = true;
            String status = matchStatus(fixture.statusShort());
            switch (status) {
                case "LIVE", "SUSPENDED" -> anyLive = true;
                case "FINISHED", "CANCELLED", "ABANDONED", "AWARDED" -> anyFinished = true;
                default -> anyOpen = true;
            }
        }
        if (!any) {
            return "SCHEDULED";
        }
        if (anyLive) {
            return "LIVE";
        }
        if (anyFinished && anyOpen) {
            return "IN_PROGRESS";
        }
        if (anyFinished) {
            return "FINISHED";
        }
        return "SCHEDULED";
    }

    private static String normalize(String statusShort) {
        if (statusShort == null || statusShort.isBlank()) {
            return null;
        }
        return statusShort.trim().toUpperCase(Locale.ROOT);
    }
}
