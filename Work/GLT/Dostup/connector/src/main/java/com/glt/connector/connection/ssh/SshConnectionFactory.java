package com.glt.connector.connection.ssh;

import com.glt.connector.connection.Connection;import com.glt.connector.connection.ConnectionFactory;
import com.glt.connector.exception.ScannerException;
import com.glt.connector.model.enums.OperatingSystemType;
import com.glt.connector.task.ScanTask;
import com.glt.connector.util.EncryptionUtils;
import lombok.RequiredArgsConstructor;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class SshConnectionFactory implements ConnectionFactory {

    private final EncryptionUtils encryptionUtils;

    @Override
    public boolean supports(ScanTask task) {
        return task.getOsType() == OperatingSystemType.UBUNTU
                || task.getOsType() == OperatingSystemType.CENTOS;
    }

    @Override
    public Connection create(ScanTask task) throws Exception {
        SSHClient ssh = new SSHClient();
        ssh.addHostKeyVerifier(new PromiscuousVerifier());

        try {
            try { ssh.setConnectTimeout(5000); } catch (Throwable ignored) {}
            try { ssh.setTimeout(15000); }       catch (Throwable ignored) {}

            ssh.connect(task.getServerIp(), task.getPort());

            String password = encryptionUtils.decrypt(task.getEncryptedPassword());
            ssh.authPassword(task.getLogin(), password);

            if (!ssh.isAuthenticated()) {
                throw new ScannerException("SSH auth failed for " + task.getServerIp());
            }

            // Быстрая проверка "здоровья" канала
            try (var s = ssh.startSession()) {
                var c = s.exec("echo ok");
                c.join();
                if (c.getExitStatus() == null) {
                    throw new ScannerException("SSH test command timeout on " + task.getServerIp());
                }
            }

            // Возвращаем объект, который ИМПЛЕМЕНТИРУЕТ интерфейс Connection
            return new SshConnection(ssh, Duration.ofSeconds(20));

        } catch (Exception e) {
            try { ssh.disconnect(); } catch (Exception ignored) {}
            try { ssh.close(); }       catch (Exception ignored) {}
            if (!(e instanceof ScannerException)) {
                throw new ScannerException("SSH connect/auth failed for " + task.getServerIp()
                        + ": " + e.getMessage(), e);
            }
            throw e;
        }
    }
}
