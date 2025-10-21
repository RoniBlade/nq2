package com.glt.connector.connection.ssh;

import com.glt.connector.connection.Connection;
import com.glt.connector.dto.ExecResult;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.*;

public class SshConnection implements Connection {

    private final SSHClient ssh;
    /** таймаут ожидания завершения команды */
    private final Duration cmdTimeout;

    public SshConnection(SSHClient ssh) {
        this(ssh, Duration.ofSeconds(15));
    }

    public SshConnection(SSHClient ssh, Duration cmdTimeout) {
        this.ssh = ssh;
        this.cmdTimeout = cmdTimeout;
    }

    @Override
    public ExecResult execWithResult(String command) throws Exception {
        try (Session session = ssh.startSession()) {
            final Session.Command cmd = session.exec(command);

            // читаем stdout/stderr параллельно, чтобы не зависнуть
            final StringBuilder outBuf = new StringBuilder();
            final StringBuilder errBuf = new StringBuilder();

            ExecutorService ioPool = Executors.newFixedThreadPool(2, r -> {
                Thread t = new Thread(r, "ssh-io");
                t.setDaemon(true);
                return t;
            });

            Future<?> outF = ioPool.submit(() -> {
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(cmd.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) outBuf.append(line).append('\n');
                } catch (Exception ignored) {}
            });

            Future<?> errF = ioPool.submit(() -> {
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(cmd.getErrorStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) errBuf.append(line).append('\n');
                } catch (Exception ignored) {}
            });

            // ждём завершения
            cmd.join(cmdTimeout.toMillis(), TimeUnit.MILLISECONDS);
            boolean finished = cmd.getExitStatus() != null;
            if (!finished) {
                try { cmd.close(); } catch (Exception ignored) {}
                try { session.close(); } catch (Exception ignored) {}
                ioPool.shutdownNow();
                throw new TimeoutException("SSH command timed out after " + cmdTimeout);
            }

            // добираем потоки (коротко)
            try {
                outF.get(2, TimeUnit.SECONDS);
                errF.get(2, TimeUnit.SECONDS);
            } catch (Exception ignored) {
                outF.cancel(true);
                errF.cancel(true);
            } finally {
                ioPool.shutdownNow();
            }

            Integer exit = cmd.getExitStatus();
            int exitCode = (exit == null ? -1 : exit);
            return new ExecResult(outBuf.toString(), errBuf.toString(), exitCode);
        }
    }

    @Override
    public String execStdout(String command) throws IOException {
        try {
            return execWithResult(command).stdout;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            // оборачиваем checked Exception в IOException, чтобы сигнатура интерфейса соблюдалась
            throw new IOException(e);
        }
    }

    @Override
    public String execStderr(String command) throws IOException {
        try {
            return execWithResult(command).stderr;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    @Override
    public void close() throws IOException {
        try { ssh.disconnect(); } catch (Exception ignored) {}
        try { ssh.close(); }       catch (Exception ignored) {}
    }
}
