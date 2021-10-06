package com.techgrid.slickbatch.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@ConfigurationProperties("slickbatch.stage")
public class StageWaitTimes {
    private Map<String, Integer> waitMs;

    public Map<String, Integer> getWaitMs() {
        return waitMs;
    }

    public void setWaitMs(Map<String, Integer> waitMs) {
        this.waitMs = waitMs;
    }
}
