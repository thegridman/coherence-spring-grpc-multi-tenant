/*
 * Copyright (c) 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.guides.client;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * A multi-tenant Spring Boot Coherence gRPC client application.
 */
@SpringBootApplication
public class Application
        implements ApplicationRunner
    {
    @Override
    public void run(ApplicationArguments args) throws Exception
        {
        }

    public static void main(String[] args)
        {
        SpringApplication.run(Application.class, args);
        }
    }
