package org.java5thsem.predictions.auth;

public interface SocialTokenVerifier {

    SocialProfile verify(SocialProvider provider, String idToken);
}
