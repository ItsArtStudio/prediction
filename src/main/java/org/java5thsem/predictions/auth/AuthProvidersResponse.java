package org.java5thsem.predictions.auth;

import java.util.List;

public record AuthProvidersResponse(List<AuthProviderStatus> providers) {
}
