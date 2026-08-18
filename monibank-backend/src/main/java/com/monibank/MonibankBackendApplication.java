package com.monibank;

import com.monibank.mainframe.config.MainframeProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(MainframeProperties.class)
public class MonibankBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(MonibankBackendApplication.class, args);
	}
}