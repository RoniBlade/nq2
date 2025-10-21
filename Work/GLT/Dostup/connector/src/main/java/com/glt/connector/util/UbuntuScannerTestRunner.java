//package com.glt.connector.util;
//
//import com.glt.connector.dto.Account.AccountDto;
//import com.glt.connector.connector.linux.ubuntu.UbuntuConnector;
//import com.glt.connector.connection.ssh.SshConnectionFactory;
//import com.glt.connector.model.enums.OperatingSystemType;
//import com.glt.connector.task.ScanTask;
//import org.springframework.boot.SpringApplication;
//import org.springframework.boot.autoconfigure.SpringBootApplication;
//
//import java.util.ArrayList;
//import java.util.List;
//
//@SpringBootApplication(scanBasePackages = "com.glt.connector")
//public class UbuntuScannerTestRunner {
//
//    public static void main(String[] args) {
//        // Spring Boot context init (чтобы сработал @Component)
//        var ctx = SpringApplication.run(UbuntuScannerTestRunner.class, args);
//
//        // Получаем EncryptionUtils из контекста
//        EncryptionUtils encryptionUtils = ctx.getBean(EncryptionUtils.class);
//        String encryptedPassword = encryptionUtils.encrypt("D9rd7458w1wD");
//
//        // Создаём задачу для подключения
//        ScanTask task = new ScanTask();
//        task.setOsType(OperatingSystemType.UBUNTU);
//        task.setServerIp("95.164.93.173");
//        task.setPort(22);
//        task.setLogin("root");
//        task.setEncryptedPassword(encryptedPassword);
//
//        // Создаём SshConnectionFactory вручную
//        SshConnectionFactory factory = new SshConnectionFactory(encryptionUtils);
//
//        // Инициализируем сканер
//        UbuntuConnector scanner = new UbuntuConnector(factory);
//
//        // Тест сканирования пользователей
//        List<AccountDto> users = new ArrayList<>(scanner.scanUsers(task));
//        System.out.println("Пользователи:");
//        users.forEach(u -> System.out.println(" - " + u.toString()));
//
//    }
//}
