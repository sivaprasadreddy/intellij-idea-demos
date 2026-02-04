package dev.sivalabs.quicknotes.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

import dev.sivalabs.quicknotes.TestcontainersConfig;
import dev.sivalabs.quicknotes.domain.entity.User;
import dev.sivalabs.quicknotes.domain.model.CreateUserCmd;
import dev.sivalabs.quicknotes.domain.model.Role;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;

@SpringBootTest(webEnvironment = NONE)
@Import(TestcontainersConfig.class)
@Sql("/test-data.sql")
class UserServiceTests {

    @Autowired
    UserService userService;

    @Test
    void shouldFindUserByEmail() {
        Optional<User> user = userService.findByEmail("admin@gmail.com");

        assertThat(user).isPresent();
        assertThat(user.get().getName()).isEqualTo("Administrator");
        assertThat(user.get().getEmail()).isEqualTo("admin@gmail.com");
        assertThat(user.get().getRole()).isEqualTo(Role.ROLE_ADMIN);
    }

    @Test
    void shouldFindUserByEmailIgnoringCase() {
        Optional<User> user = userService.findByEmail("ADMIN@GMAIL.COM");

        assertThat(user).isPresent();
        assertThat(user.get().getName()).isEqualTo("Administrator");
        assertThat(user.get().getEmail()).isEqualTo("admin@gmail.com");
    }

    @Test
    void shouldReturnEmptyWhenUserNotFound() {
        Optional<User> user = userService.findByEmail("nonexistent@gmail.com");

        assertThat(user).isEmpty();
    }

    @Test
    void shouldCreateNewUser() {
        CreateUserCmd cmd = new CreateUserCmd("John Doe", "john@example.com", "password123", Role.ROLE_USER);

        userService.createUser(cmd);

        Optional<User> createdUser = userService.findByEmail("john@example.com");
        assertThat(createdUser).isPresent();
        assertThat(createdUser.get().getName()).isEqualTo("John Doe");
        assertThat(createdUser.get().getEmail()).isEqualTo("john@example.com");
        assertThat(createdUser.get().getRole()).isEqualTo(Role.ROLE_USER);
    }

    @Test
    void shouldCreateUserWithAdminRole() {
        CreateUserCmd cmd = new CreateUserCmd("Admin User", "newadmin@example.com", "adminpass", Role.ROLE_ADMIN);

        userService.createUser(cmd);

        Optional<User> createdUser = userService.findByEmail("newadmin@example.com");
        assertThat(createdUser).isPresent();
        assertThat(createdUser.get().getRole()).isEqualTo(Role.ROLE_ADMIN);
    }
}
