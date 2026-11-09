package com.tokoku;

import org.springframework.boot.SpringApplication;

public class TestTokokuApplication {

	public static void main(String[] args) {
		SpringApplication.from(TokokuApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
