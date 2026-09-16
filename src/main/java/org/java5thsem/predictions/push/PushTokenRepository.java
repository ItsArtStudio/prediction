package org.java5thsem.predictions.push;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PushTokenRepository extends JpaRepository<PushToken, Long> {

    Optional<PushToken> findByToken(String token);

    Optional<PushToken> findByIdAndUser_Id(Long id, Long userId);

    Optional<PushToken> findByUser_IdAndDeviceId(Long userId, String deviceId);

    List<PushToken> findByUser_IdAndActiveTrueOrderByUpdatedAtDesc(Long userId);

    List<PushToken> findByUser_IdAndActiveTrue(Long userId);
}
