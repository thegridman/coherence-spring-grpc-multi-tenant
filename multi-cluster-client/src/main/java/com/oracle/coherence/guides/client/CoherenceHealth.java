/*
 * Copyright (c) 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.guides.client;

import com.tangosol.net.Coherence;
import com.tangosol.net.management.Registry;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * An example of integrating Coherence ready check into the Spring Boot health check.
 * <p>
 * for example, running {@code curl http://localhost:8080/actuator/health} will
 * run the {@link #health()} method below, as well as other Spring Boot health
 * checks and should respond with a 200 response and message {@code "status":"UP"}.
 */
@Component
public class CoherenceHealth
        implements HealthIndicator
    {
    /**
     * The running Coherence instance.
     */
    private final Coherence coherence;

    /**
     * Create a {@link CoherenceHealth} component.
     * <p>
     * Spring will inject the running {@link Coherence} instance.
     *
     * @param coherence  the running Coherence instance
     */
    public CoherenceHealth(Coherence coherence)
        {
        this.coherence = coherence;
        }

    @Override
    public Health health()
        {
        if (coherence.isStarted())
            {
            Registry management = coherence.getManagement();
            if (management.allHealthChecksReady())
                {
                return Health.up()
                        .withDetail("Ready", true)
                        .withDetail("Started", management.allHealthChecksStarted())
                        .withDetail("Live", management.allHealthChecksLive())
                        .withDetail("Safe", management.allHealthChecksSafe())
                        .build();
                }
            else
                {
                return Health.down()
                        .withDetail("Ready", false)
                        .withDetail("Started", management.allHealthChecksStarted())
                        .withDetail("Live", management.allHealthChecksLive())
                        .withDetail("Safe", management.allHealthChecksSafe())
                        .build();
                }
            }
        return Health.down()
                .withDetail("Coherence", false)
                .build();
        }
    }
