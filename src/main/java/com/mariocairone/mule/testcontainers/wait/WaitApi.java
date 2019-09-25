package com.mariocairone.mule.testcontainers.wait;

import java.time.Duration;

import com.mariocairone.mule.testcontainers.wait.strategy.ApiWaitStrategy;
import com.mariocairone.mule.testcontainers.wait.strategy.FixedDurationWaitStrategy;

/**
 * Convenience class with logic for building  {@link ApiWaitStrategy} instances.
 *
 */
public class WaitApi {


    /**
     * Convenience method to return a ApiWaitStrategy for an API endpoint.
     *
     * @param endpoint the endpoint path to check
     * @return the ApiWaitStrategy
     * @see ApiWaitStrategy
     */
    public static ApiWaitStrategy forEndpoint(String endpoint) {
        return new ApiWaitStrategy()
                .forPath(endpoint);
    }

    /**
     * Convenience method to return a FixedDurationWaitStrategy.
     *
     * @param duration the duration to wait
     * @return the FixedDurationWaitStrategy
     * @see ApiWaitStrategy
     */
    public static FixedDurationWaitStrategy forDuration(Duration duration) {
        return new FixedDurationWaitStrategy()
                .withDuration(duration);
    }    

}
