package com.chaiyanan09.toothseg;

import com.chaiyanan09.toothseg.history.PredictionHistory;
import com.chaiyanan09.toothseg.repository.PredictionHistoryRepository;
import com.chaiyanan09.toothseg.repository.UserRepository;
import com.chaiyanan09.toothseg.user.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Needs a running PostgreSQL (see README). Each test runs in a transaction that is rolled back,
 * so nothing is left in the database.
 */
@SpringBootTest
@Transactional
class PersistenceMappingTest {

    @Autowired
    UserRepository users;

    @Autowired
    PredictionHistoryRepository history;

    @Autowired
    EntityManager em;

    @Test
    void savesAndLoadsHistoryWithArrayAndJsonColumns() {
        User u = new User();
        u.fullName = "Mapping Test";
        u.email = "mapping-test@example.com";
        u.passwordHash = "x";
        users.save(u);

        PredictionHistory h = new PredictionHistory();
        h.setUserId(u.id);
        h.setPath("case01/pano.png");
        h.setInferenceMs(842);
        h.setPresentFdi(List.of("11", "12"));
        h.setMissingFdi(List.of("18", "28"));
        h.setPresentTopScoreMap(Map.of("11", 0.98, "12", 0.95));
        history.save(h);

        // write to the database, then forget cached objects so the next reads really hit PostgreSQL
        em.flush();
        em.clear();

        List<PredictionHistory> list = history.findByUserIdOrderByCreatedAtDesc(u.id);
        assertThat(list).hasSize(1);

        PredictionHistory loaded = list.get(0);
        assertThat(loaded.getId()).isNotNull();
        assertThat(loaded.getPresentFdi()).containsExactly("11", "12");
        assertThat(loaded.getMissingFdi()).containsExactly("18", "28");
        assertThat(loaded.getPresentTopScoreMap())
                .containsEntry("11", 0.98)
                .containsEntry("12", 0.95);

        assertThat(history.findByIdAndUserId(loaded.getId(), u.id)).isPresent();
        assertThat(history.findByIdAndUserId(loaded.getId(), UUID.randomUUID())).isEmpty();
    }

    @Test
    void findsUserByEmailAndResetToken() {
        User u = new User();
        u.fullName = "Reset Test";
        u.email = "reset-test@example.com";
        u.passwordHash = "x";
        u.resetTokenHash = "token-hash-123";
        users.save(u);
        em.flush();
        em.clear();

        assertThat(users.existsByEmail("reset-test@example.com")).isTrue();
        assertThat(users.findByEmail("reset-test@example.com")).isPresent();
        assertThat(users.findByResetTokenHash("token-hash-123")).isPresent();
        assertThat(users.findByResetTokenHash("token-hash-123").get().id).isEqualTo(u.id);
    }
}
