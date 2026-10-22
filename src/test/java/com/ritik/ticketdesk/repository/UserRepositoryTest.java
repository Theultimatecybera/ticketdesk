package com.ritik.ticketdesk.repository;

import com.ritik.ticketdesk.entity.User;
import com.ritik.ticketdesk.entity.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Should save user and find by email")
    void shouldSaveAndFindByEmail() {
        User user = new User("Ritik Pal", "ritik@example.com", UserRole.USER);
        User savedUser = userRepository.save(user);

        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getCreatedAt()).isNotNull();

        Optional<User> found = userRepository.findByEmail("ritik@example.com");
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Ritik Pal");
        assertThat(found.get().getRole()).isEqualTo(UserRole.USER);
    }

    @Test
    @DisplayName("Should check if user exists by email")
    void shouldCheckExistsByEmail() {
        User user = new User("Admin User", "admin@example.com", UserRole.ADMIN);
        userRepository.save(user);

        assertThat(userRepository.existsByEmail("admin@example.com")).isTrue();
        assertThat(userRepository.existsByEmail("nonexistent@example.com")).isFalse();
    }
}
