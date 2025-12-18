package com.wreckloud.wolfchat;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan({
		"com.wreckloud.wolfchat.account.infra.mapper",
		"com.wreckloud.wolfchat.group.infra.mapper"
})
public class WolfChatApplication {

	public static void main(String[] args) {
		SpringApplication.run(WolfChatApplication.class, args);
	}

}
