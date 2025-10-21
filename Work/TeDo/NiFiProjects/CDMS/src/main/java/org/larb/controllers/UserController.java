package org.larb.controllers;

import org.larb.model.Product;
import org.larb.model.User;
import org.larb.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

//@RestController
//@RequestMapping("/users")
//public class UserController {
//
//    @Autowired
//    private UserService userService;
//
//    /**
//     * Создание пользователя
//     *
//     * @param user Новый пользователь
//     * @return Созданный пользователь
//     */
//    @PostMapping("users/register")
//    public ResponseEntity<User> createUser(@RequestBody User user) {
//        User createUser = userService.createUser(user);
//        return new ResponseEntity<>(createUser, HttpStatus.CREATED);
//    }
//
//    /**
//     * Редактирование пользователя
//     *
//     * @param userId ID пользователя
//     * @param updatedUser Обновленная информация о пользователе
//     * @return Обновленный пользователь или статус 404, если пользователь не найден
//     */
//    @PutMapping("/{user-id}")
//    public ResponseEntity<User> editUser(@PathVariable Long userId, @RequestBody User updatedUser) {
//        Optional<User> editedUser = userService.editUser(userId, updatedUser);
//        return editedUser.map(user -> new ResponseEntity<>(user, HttpStatus.OK)).orElseGet(()  -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
//    }
//
//    /**
//     * Изменение пароля
//     *
//     * @param userId ID пользователя
//     * @param newPassword Новый пароль
//     * @return Обновленный пользователь или статус 404, если пользователь не найден
//     */
//    @PutMapping("/{user-id}/change-password")
//    public ResponseEntity<User> changePassword(@PathVariable Long userId, @RequestBody String newPassword) {
//        Optional<User> updatedUser = userService.changePassword(userId, newPassword);
//        return updatedUser.map(user -> new ResponseEntity<>(user, HttpStatus.OK)).orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
//    }
//
//    /**
//     * Изменение имени
//     *
//     * @param userId ID пользователя
//     * @param newUsername Новое имя
//     * @return Обновленный пользователь или статус 404, если пользователь не найден
//     */
//    @PutMapping("/{user-id}/change-username")
//    public ResponseEntity<User> changeUsername(@PathVariable Long userId, @RequestBody String newUsername) {
//        Optional<User> updatedUser = userService.changeUsername(userId, newUsername);
//        return updatedUser.map(user -> new ResponseEntity<>(user, HttpStatus.OK)).orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
//    }
//
//    /**
//     * Блокировка пользователя
//     *
//     * @param userId
//     * @return Сообщение о том, что пользователь был удален либо статус 404, если пользователь не найден
//     *
//     */
//    @PutMapping("/{user-id}/block")
//    public ResponseEntity<User> blockUser(@PathVariable Long userId) {
//        Optional<User> blockedUser = userService.blockUser(userId);
//        return blockedUser.map(user -> new ResponseEntity<>(user, HttpStatus.OK)).orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
//    }
//
//    /**
//     * Получения товара по ID
//     *
//     * @param id
//     * @return Информация о продукте
//     */
//    @GetMapping
//    public ResponseEntity<User> getUserById(Long id) {
//        Optional<User> user = userService.getUserById(id);
//        return user.map((u) -> new ResponseEntity<>(u, HttpStatus.OK)).orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND)) ;
//    }
//
//}


@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public ResponseEntity<User> createUser(@RequestBody User user) {
        User createUser = userService.createUser(user);
        return new ResponseEntity<>(createUser, HttpStatus.CREATED);
    }

    @PutMapping("/{userId}")
    public ResponseEntity<User> editUser(@PathVariable Long userId, @RequestBody User updatedUser) {
        Optional<User> editedUser = userService.editUser(userId, updatedUser);
        return editedUser.map(user -> new ResponseEntity<>(user, HttpStatus.OK)).orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PutMapping("/{userId}/change-password")
    public ResponseEntity<User> changePassword(@PathVariable Long userId, @RequestBody String newPassword) {
        Optional<User> updatedUser = userService.changePassword(userId, newPassword);
        return updatedUser.map(user -> new ResponseEntity<>(user, HttpStatus.OK)).orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PutMapping("/{userId}/change-username")
    public ResponseEntity<User> changeUsername(@PathVariable Long userId, @RequestBody String newUsername) {
        Optional<User> updatedUser = userService.changeUsername(userId, newUsername);
        return updatedUser.map(user -> new ResponseEntity<>(user, HttpStatus.OK)).orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PutMapping("/{userId}/block")
    public ResponseEntity<User> blockUser(@PathVariable Long userId) {
        Optional<User> blockedUser = userService.blockUser(userId);
        return blockedUser.map(user -> new ResponseEntity<>(user, HttpStatus.OK)).orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        Optional<User> user = userService.getUserById(id);
        return user.map((u) -> new ResponseEntity<>(u, HttpStatus.OK)).orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }
}