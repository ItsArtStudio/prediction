package org.java5thsem.predictions.auth;

import org.java5thsem.predictions.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SocialAccountRepository extends JpaRepository<SocialAccount, Long> {

    Optional<SocialAccount> findByProviderAndProviderSubject(SocialProvider provider, String providerSubject);

    List<SocialAccount> findByUser(User user);

    boolean existsByUserAndProvider(User user, SocialProvider provider);
}
