package com.techgrid.slickbatch;

import com.techgrid.slickbatch.application.StageWaitTimes;
import com.techgrid.slickbatch.logging.SBLogger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({StageWaitTimes.class})
public class SlickbatchApplication {

	public static void main(String[] args) {
		SBLogger.info(
				"███████ ██      ██  ██████ ██   ██ ██████   █████  ████████  ██████ ██   ██\n" +
				"██      ██      ██ ██      ██  ██  ██   ██ ██   ██    ██    ██      ██   ██\n" +
				"███████ ██      ██ ██      █████   ██████  ███████    ██    ██      ███████\n" +
				"     ██ ██      ██ ██      ██  ██  ██   ██ ██   ██    ██    ██      ██   ██\n" +
				"███████ ███████ ██  ██████ ██   ██ ██████  ██   ██    ██     ██████ ██   ██\n");
		SpringApplication.run(SlickbatchApplication.class, args);
	}

}
