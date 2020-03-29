package org.dev.app.webflux.app.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebConfiguration {

	@Value("${config.base.endpoint}")
	private String urlEndpoint;
	
	@Bean
	public WebClient webClient() {
		return WebClient.create(urlEndpoint);
	}
}
