package org.dev.app.webflux.eureka.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@EnableEurekaServer
@SpringBootApplication
public class SpringBootWebfluxEurekaApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringBootWebfluxEurekaApplication.class, args);
	}

}
