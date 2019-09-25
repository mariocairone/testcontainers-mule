package com.mariocairone.mule.testcontainers.wait.strategy;

import java.time.Duration;

import org.testcontainers.containers.wait.strategy.AbstractWaitStrategy;

public class FixedDurationWaitStrategy extends AbstractWaitStrategy {

	@java.lang.SuppressWarnings("all")
	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FixedDurationWaitStrategy.class);

	private Duration duration;

	public FixedDurationWaitStrategy() {
		super();
	}

	@Override
	protected void waitUntilReady() {

		try {
			Thread.sleep(duration.toMillis());
		} catch (InterruptedException e) {
			log.error(e.getMessage(), e);
		}
	}

	public FixedDurationWaitStrategy withDuration(Duration duration) {
		this.duration = duration;
		return this;
	}

}
