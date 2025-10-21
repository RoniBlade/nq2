//package com.glt.connector.util;
//
//import com.glt.connector.dto.Account.AccountBatch;
//import com.glt.connector.connector.windows.WindowsConnector;
//import com.glt.connector.connection.ssh.SshConnectionFactory;
//import com.glt.connector.model.enums.OperatingSystemType;
//import com.glt.connector.task.ScanTask;
//import org.springframework.boot.SpringApplication;
//import org.springframework.boot.autoconfigure.SpringBootApplication;
//
//@SpringBootApplication(scanBasePackages = "com.glt.connector")
//public class WindowsScannerTestRunner {
//
//    public static void main(String[] args) {
//        // Инициализация контекста Spring Boot
//        var ctx = SpringApplication.run(WindowsScannerTestRunner.class, args);
//
//        // Получаем EncryptionUtils из контекста
//        EncryptionUtils encryptionUtils = ctx.getBean(EncryptionUtils.class);
//        String encryptedPassword = encryptionUtils.encrypt("potteri1972");
//
//        // Задача на подключение к Windows по SSH
//        ScanTask task = new ScanTask();
//        task.setOsType(OperatingSystemType.WINDOWS);
//        task.setServerIp("127.0.0.1"); // или внешний IP, если доступен
//        task.setPort(22);
//        task.setLogin("Adam"); // ваш логин в Windows
//        task.setEncryptedPassword(encryptedPassword);
//
//        // Создаём SshConnectionFactory вручную
//        SshConnectionFactory factory = new SshConnectionFactory(encryptionUtils);
//
//        // Создаём WindowsConnector
//        WindowsConnector scanner = new WindowsConnector(factory);
//
//        try {
//            // Получаем пользователей
//
//            long start = System.nanoTime();
//
//            AccountBatch users = new AccountBatch(scanner.scanUsers(task));
//
//            System.out.println("Пользователи:");
//            users.getUsers().forEach(System.out::println);
//
//        } catch (Exception e) {
//            System.err.println("Ошибка при сканировании: " + e.getMessage());
//        }
//    }
//}
