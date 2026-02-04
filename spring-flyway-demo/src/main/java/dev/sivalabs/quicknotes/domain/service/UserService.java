package dev.sivalabs.quicknotes.domain.service;

import dev.sivalabs.quicknotes.domain.entity.User;
import dev.sivalabs.quicknotes.domain.exception.BadRequestException;
import dev.sivalabs.quicknotes.domain.model.CreateUserCmd;
import dev.sivalabs.quicknotes.domain.repo.UserRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository userRepository;

    UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email);
    }

    @Transactional
    public void createUser(CreateUserCmd cmd) {
        if (userRepository.existsByEmailIgnoreCase(cmd.email())) {
            throw new BadRequestException("User with email already exists");
        }
        var user = new User();
        user.setName(cmd.name());
        user.setEmail(cmd.email());
        user.setPassword(cmd.password());
        user.setRole(cmd.role());
        userRepository.save(user);
    }
}
