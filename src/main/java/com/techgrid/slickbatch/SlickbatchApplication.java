package com.techgrid.slickbatch;

import com.techgrid.slickbatch.application.StageWaitTimes;
import com.techgrid.slickbatch.logging.SBLogger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Objects;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({StageWaitTimes.class})
public class SlickbatchApplication {

	public static void main(String[] args) {
		var label =
				"███████ ██      ██  ██████ ██   ██ ██████   █████  ████████  ██████ ██   ██\n" +
				"██      ██      ██ ██      ██  ██  ██   ██ ██   ██    ██    ██      ██   ██\n" +
				"███████ ██      ██ ██      █████   ██████  ███████    ██    ██      ███████\n" +
				"     ██ ██      ██ ██      ██  ██  ██   ██ ██   ██    ██    ██      ██   ██\n" +
				"███████ ███████ ██  ██████ ██   ██ ██████  ██   ██    ██     ██████ ██   ██\n" +
				"Version: 0.0.3\n";
		SBLogger.info(label);
		SpringApplication.run(SlickbatchApplication.class, args);
	}

	@EventListener(ApplicationReadyEvent.class)
	public void printReadyMessage() {
		SBLogger.info("\nReady to accept requests...\n");
	}
}
