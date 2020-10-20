package com.leeroy.forwordpanel.forwordpanel.common.util.remotessh;

import com.jcraft.jsch.*;
import com.leeroy.forwordpanel.forwordpanel.model.Server;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ClassPathUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ResourceUtils;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

/**
 * This class provide interface to execute command on remote Linux.
 */
@Slf4j
public class SSHCommandExecutor {
    private String ipAddress;

    private String username;

    private String password;

    private String privateKeyPath;

    public int  port = 34204;

    private Vector<String> stdout;

    public String getIpAddress() {
        return ipAddress;
    }

    public int getPort() {
        return port;
    }

    public SSHCommandExecutor(final String ipAddress, final String username, final String password) {
        this.ipAddress = ipAddress;
        this.username = username;
        this.password = password;
        stdout = new Vector<>();
    }

    public SSHCommandExecutor(Server server) {
        this.ipAddress = server.getHost();
        this.port = server.getPort();
        this.username = server.getUsername();
//        this.privateKeyPath = server.getPassword();
        this.password = server.getPassword();
        stdout = new Vector<>();
    }


    public SSHCommandExecutor(Server server, final String username, final String privateKeyPath) {
        this.ipAddress = server.getHost();
        this.port = server.getPort();
        this.username = username;
        this.privateKeyPath = privateKeyPath;
        stdout = new Vector<>();
    }

    public int execute(final String... commandList) {
        stdout.clear();
        int returnCode = 0;
        JSch jsch = new JSch();
        MyUserInfo userInfo = new MyUserInfo();
        try {
            Session session = jsch.getSession(username, ipAddress, port);
            session.setPassword(password);
            session.setUserInfo(userInfo);
            session.connect();
            for (String command : commandList) {
                // Create and connect channel.
                Channel channel = session.openChannel("exec");
                ((ChannelExec) channel).setCommand(command);
                channel.setInputStream(null);
                BufferedReader input = new BufferedReader(new InputStreamReader(channel
                        .getInputStream()));
                channel.connect();
                log.info("The remote command is: {}", command);

                // Get the output of remote command.
                String line;
                while ((line = input.readLine()) != null) {
                    stdout.add(line);
                }
                input.close();
                // Get the return code only after the channel is closed.
                if (channel.isClosed()) {
                    returnCode = channel.getExitStatus();
                }
                // Disconnect the channel and session.
                channel.disconnect();
            }

            session.disconnect();
        } catch (Exception e) {
            log.error("执行shell失败", e);
        }
        log.info("shell result: {}", StringUtils.join(stdout));
        return returnCode;
    }

    /**
     * 执行脚本
     * @param script
     * @return
     */
    public void executeScript(final String script) {
        try {
            List<String> commandList = new ArrayList<>();
            ClassPathResource resource = new ClassPathResource("scripts/" + script);
            BufferedReader br = new BufferedReader(new InputStreamReader(resource.getInputStream()));
            String str;
            while ((str = br.readLine()) != null) {
                commandList.add(str);
            }
            execute(commandList.toArray(new String[]{}));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Vector<String> getResultSet() {
        return stdout;
    }

    public String getResult() {
        return CollectionUtils.isEmpty(stdout) ? "" : stdout.get(0);
    }

    public static void main(final String[] args) {
        SSHCommandExecutor sshExecutor = new SSHCommandExecutor("120.241.154.4", "root", "28L8CegNk9");
        long stsart = System.currentTimeMillis();
        sshExecutor.execute("netstat -tunlp", "netstat -tunlp", "netstat -tunlp", "netstat -tunlp");
        Vector<String> stdout = sshExecutor.getResultSet();
        for (String str : stdout) {
            System.out.println(str);
        }
        System.out.println(System.currentTimeMillis() - stsart);
    }
}
