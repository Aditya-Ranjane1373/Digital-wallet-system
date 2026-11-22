package com.wallet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class WalletSystemTaskApplication {

	public static void main(String[] args) {
		SpringApplication.run(WalletSystemTaskApplication.class, args);
		
		System.out.print("---------Wallate System Run Successfully---------");
	}

}
