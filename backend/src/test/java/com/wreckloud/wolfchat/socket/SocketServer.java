package com.wreckloud.wolfchat.socket;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;

/**
 * @Description
 * @Author Wreckloud
 * @Date 2025-12-03
 */
@Slf4j
public class SocketServer {
    public static void main(String[] args) {
        try {
            ServerSocket server = new ServerSocket(6666);
            // 存储所有客户端的 PrintWriter
            Map<Integer, PrintWriter> ClientMap = Collections.synchronizedMap(new java.util.HashMap<>());
            log.info("服务器启动成功...");

            int userId = 0;

            while (true) {
                userId++;
                int currentUserId = userId; // ★ 为当前客户端固定一份 ID
                Socket accept = server.accept();
                log.info("用户{}已连接:{}:{}", currentUserId, accept.getInetAddress().getHostAddress(), server.getLocalPort());

                // 每连接一个就开一个线程处理它
                PrintWriter pw = new PrintWriter(accept.getOutputStream(), true);
                ClientMap.put(currentUserId, pw);

                new Thread(() -> {
                    // 拿到入口对象, 使用 转换流 确保编码.
                    try {
                        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(accept.getInputStream(), StandardCharsets.UTF_8));

                        // 主线程
                        while (true) {
                            // 接收消息
                            String line = bufferedReader.readLine();
                            if (line == null) {
                                break;
                            }
                            log.info("[用户{}]:{}", currentUserId, line);

                            synchronized (ClientMap) {
                                // 回信,给所有客户端
                                for (Map.Entry<Integer, PrintWriter> entry : ClientMap.entrySet()) {
                                    PrintWriter p = entry.getValue();
                                    // 但排除自己
                                    if (entry.getKey() == currentUserId) continue;
                                    p.println(LocalDateTime.now()+"[用户" + currentUserId + "]:" + line);
                                }
                            }

                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    } finally {
                        ClientMap.remove(currentUserId);
                        try {
                            accept.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                }).start();
            }


        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }
}
