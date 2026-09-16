package org.java5thsem.predictions.provider;

import java.util.List;

public record SeasonSchedule(List<String> rounds, List<ProviderFixture> fixtures) {
}
