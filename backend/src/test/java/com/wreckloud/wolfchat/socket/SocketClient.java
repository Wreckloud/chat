package com.wreckloud.wolfchat.socket;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * @Description
 * @Author Wreckloud
 * @Date 2025-12-03
 */
@Slf4j
public class SocketClient {
    public static void main(String[] args) {

        // 主线程: 发送消息
        try {
            Socket socket  = new Socket("127.0.0.1", 6666);
            log.info("客户端启动成功...");

            Scanner sc = new Scanner(System.in);

            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            PrintWriter pw = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

            new Thread(()->{
                while (true){
                    // 接收服务器回信
                    String line = null;
                    try {
                        line = bufferedReader.readLine();
                        if (line == null) break;
                        System.out.println(line);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                }
            }).start();

            while (true) {
                // 发送消息
                String msg = sc.nextLine();

                pw.println(msg);


            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
