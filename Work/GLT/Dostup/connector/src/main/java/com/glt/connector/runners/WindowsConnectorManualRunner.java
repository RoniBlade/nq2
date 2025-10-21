package com.glt.connector.runners;

import com.glt.connector.connector.windows.WindowsConnector;
import com.glt.connector.model.enums.OperatingSystemType;
import com.glt.connector.service.AccountService;
import com.glt.connector.task.ScanTask;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("manual-test")
@RequiredArgsConstructor
public class WindowsConnectorManualRunner implements CommandLineRunner {

    private final WindowsConnector connector;
    private final AccountService accountService;

    @Override
    public void run(String... args) throws Exception {
        ScanTask task = new ScanTask();
        task.setServerId(9803L);
        task.setTaskId("manual-001");
        task.setServerIp("192.168.21.124");
        task.setPort(22);
        task.setLogin("Asana-1C\\\\администратор");
        task.setEncryptedPassword("3w3l5iMIHui6AOpuR35y9IU4EJKesvbzJR9PVxcVS5VuSQBjBf+qfT36Gan2gPQX");
        task.setOsType(OperatingSystemType.WINDOWS);

        var users = connector.scanUsers(task);
        users.forEach(System.out::println);

        accountService.upsertBatch(users);

        System.out.println("=== Сохранил " + users.size() + " пользователей в БД ===");
    }
}
