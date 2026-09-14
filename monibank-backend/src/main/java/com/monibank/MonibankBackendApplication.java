package com.monibank;

import com.monibank.mainframe.config.DailyCloseProperties;
import com.monibank.mainframe.config.KicksTerminalProperties;
import com.monibank.mainframe.config.MainframeProperties;
import com.monibank.mainframe.security.RateLimitProperties;
import com.monibank.mainframe.security.AdminSecurityProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties({
		MainframeProperties.class,
		KicksTerminalProperties.class,
		DailyCloseProperties.class,
		RateLimitProperties.class,
		AdminSecurityProperties.class
})
@EnableScheduling
public class MonibankBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(MonibankBackendApplication.class, args);
	}
}
