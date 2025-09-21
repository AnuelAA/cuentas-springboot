package com.cuentas.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.cuentas")
public class CuentasProjectApplication {

	public static void main(String[] args) {
		SpringApplication.run(CuentasProjectApplication.class, args);
	}

}
