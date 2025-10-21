package org.larb.services;

import org.larb.model.User;
import org.larb.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {


    /*
    TODO
        Создание пользователя
        Редактирование пользователя
            Изменение пароля
            Изменение логина
        Удаление пользователя
        Блокировка пользователя
    */

    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Создание пользователя
     *
     * @param user Новый пользователь
     * @return Созданный пользователь
     */
    public User createUser(User user) {
        return userRepository.save(user);
    }

    /**
     * Редактирование пользователя
     *
     * @param userId ID пользователя
     * @param updatedUser Обновленная информация о пользователе
     * @return Обновленный пользователь или Optional.empty(), если пользователь не найден
     */
    public Optional<User> editUser(Long userId, User updatedUser) {
        return userRepository.findById(userId).map(existingUser -> {
            existingUser.setUsername(updatedUser.getUsername());
            existingUser.setPassword(updatedUser.getPassword());
            return userRepository.save(existingUser);
        });
    }

    /**
     * Изменение пароля
     *
     * @param userId ID пользователя
     * @param newPassword Новый пароль
     * @return Обновленный пользователь или Optional.empty(), если пользователь не найден
     */
    public Optional<User> changePassword(Long userId, String newPassword) {
        return userRepository.findById(userId).map(existingUser -> {
            existingUser.setPassword(newPassword);
            return userRepository.save(existingUser);
        });
    }

    /**
     * Изменение логина
     *
     * @param userId ID пользователя
     * @param newUsername Новый логин
     * @return Обновленный пользователь или Optional.empty(), если пользователь не найден
     */
    public Optional<User> changeUsername(Long userId, String newUsername) {
        return userRepository.findById(userId).map(existingUser -> {
            existingUser.setUsername(newUsername);
            return userRepository.save(existingUser);
        });
    }

    /**
     * Удаление пользователя
     *
     * @param userId ID пользователя
     */
    public void deleteUser(Long userId) {
        userRepository.deleteById(userId);
    }

    /**
     * Блокировка пользователя
     *
     * @param userId ID пользователя
     * @return Обновленный пользователь или Optional.empty(), если пользователь не найден
     */
    public Optional<User> blockUser(Long userId) {
        return userRepository.findById(userId).map(existingUser -> {
            existingUser.setBlocked(true);
            return userRepository.save(existingUser);
        });
    }


    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }
}
