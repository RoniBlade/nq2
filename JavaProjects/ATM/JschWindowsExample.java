import com.jcraft.jsch.*;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class JschWindowsExample {

    private final String host = "192.168.21.124";
    private final String username = "Asana-1C\\администратор";
    private final String password = "SuZWLRVmSlGwZ4udeyuq6gM13JnvTiAIO";
    private final int port = 22;

    private final String CMD_GET_USERS_JSON = "powershell -NoLogo -NoProfile -ExecutionPolicy Bypass -Command \"" +
            "$ErrorActionPreference='Stop'; " +
            "$OutputEncoding = [Console]::OutputEncoding = [Text.UTF8Encoding]::new(); " +
            "$users = Get-LocalUser | Where-Object { [int](($_.SID.Value).Split('-')[-1]) -ge 1000 }; " +
            "$result = foreach ($user in $users) { " +
            "try { $groups = ([ADSI](\\\"WinNT://./$($user.Name),user\\\")).Groups() | ForEach-Object { $_.GetType().InvokeMember(\\\"Name\\\", 'GetProperty', $null, $_, $null) } } catch { $groups = @() }; "
            +
            "[PSCustomObject]@{ " +
            "Name = $user.Name; " +
            "Description = $user.Description; " +
            "Enabled = $user.Enabled; " +
            "AccountExpires = $user.AccountExpires; " +
            "SID = $user.SID.Value; " +
            "Groups = $groups " +
            "} }; " +
            "$result | ConvertTo-Json -Compress\"";

    public static void main(String[] args) {

        JschWindowsExample jschWindowsExample = new JschWindowsExample();
        jschWindowsExample.connectAndRun();

    }

    public void connectAndRun() {
        try {
            JSch jSch = new JSch();
            Session session = jSch.getSession(username, host, port);
            session.setPassword(password);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect(10_000);

            System.out.println("Подключено к " + host);

            Channel channel = session.openChannel("exec");
            ChannelExec channelExec = (ChannelExec) channel;
            channelExec.setCommand(CMD_GET_USERS_JSON);

            InputStream inputStream = channelExec.getInputStream();

            channelExec.connect();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int read = 0;

            while ((read = inputStream.read(buffer)) != -1) {
                baos.write(buffer, 0, read);
            }
            System.out.println(baos.toString(StandardCharsets.UTF_8));

            channelExec.disconnect();
            session.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
