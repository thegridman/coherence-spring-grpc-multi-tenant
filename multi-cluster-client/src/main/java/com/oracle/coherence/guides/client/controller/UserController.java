/*
 * Copyright (c) 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.guides.client.controller;

import com.oracle.coherence.client.GrpcSessionConfiguration;
import com.oracle.coherence.guides.client.errors.BadRequestException;
import com.oracle.coherence.guides.client.errors.NotFoundException;
import com.oracle.coherence.guides.client.model.TenantMetaData;
import com.oracle.coherence.guides.client.model.User;
import com.oracle.coherence.spring.configuration.annotation.CoherenceMap;
import com.tangosol.net.Coherence;
import com.tangosol.net.NamedCache;
import com.tangosol.net.NamedMap;
import com.tangosol.net.Session;
import com.tangosol.net.SessionConfiguration;
import io.grpc.ChannelCredentials;
import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.ManagedChannel;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;

/**
 * A basic set if REST endpoints to perform CRUD operations on
 * the {@link User} entity.
 */
@RestController
@RequestMapping(path="/users")
public class UserController
    {
    /**
     * The name of the request header holding the tenant identifier.
     */
    public static final String TENANT_HEADER = "tenant";

    /**
     * The map of tenant meta-data, this will be a Coherence cache, injected by Spring.
     */
    @CoherenceMap("tenants")
    private NamedMap<String, TenantMetaData> tenants;

    /**
     * Handle a User get request.
     *
     * @param id      the user identifier
     * @param tenant  the tenant id from the request header
     *
     * @return  the response
     */
    @GetMapping(value = "{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<User> get(@PathVariable("id") String id, @RequestHeader(TENANT_HEADER) String tenant)
        {
        Session session = ensureSession(tenant);

        NamedCache<String, User> users = session.getCache("users");
        User user = users.get(id);
        if (user == null)
            {
            throw new NotFoundException("Unknown user " + id);
            }

        return ResponseEntity.ok(user);
        }

    /**
     * Handle a User create request.
     *
     * @param newUser  the new {@link User}
     * @param tenant   the tenant id from the request header
     *
     * @return  the response
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<User> create(@RequestBody User newUser, @RequestHeader(TENANT_HEADER) String tenant)
        {
        Session session = ensureSession(tenant);
        NamedCache<String, User> users = session.getCache("users");

        String id = newUser.getFirstName() + "." + newUser.getLastName();
        newUser.setId(id.toLowerCase(Locale.ROOT));

        users.put(newUser.getId(), newUser);
        return ResponseEntity.ok(newUser);
        }

    /**
     * Handle a User update request.
     *
     * @param id       the user identifier
     * @param newUser  the new {@link User}
     * @param tenant   the tenant id from the request header
     *
     * @return  the response
     */
    @PutMapping(value = "{id}",
                consumes = MediaType.APPLICATION_JSON_VALUE,
                produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<User> update(@PathVariable("id") String id,
                                       @RequestBody User newUser,
                                       @RequestHeader(TENANT_HEADER) String tenant)
        {
        Session session = ensureSession(tenant);
        NamedCache<String, User> users = session.getCache("users");
        User user = users.get(id);
        if (user == null)
            {
            throw new NotFoundException("Unknown user " + id);
            }

        String firstName = newUser.getFirstName();
        if (firstName != null && !firstName.isBlank())
            {
            user.setFirstName(firstName);
            }

        String lastName = newUser.getLastName();
        if (lastName != null && !lastName.isBlank())
            {
            user.setLastName(lastName);
            }

        String email = newUser.getEmail();
        if (email != null && !email.isBlank())
            {
            user.setEmail(email);
            }

        users.put(id, user);

        return ResponseEntity.ok(user);
        }

    /**
     * Handle a User delete request.
     *
     * @param id      the user identifier
     * @param tenant  the tenant id from the request header
     */
    @DeleteMapping
    public void delete(@PathVariable("id") String id, @RequestHeader(TENANT_HEADER) String tenant)
        {
        Session session = ensureSession(tenant);
        NamedCache<String, User> users = session.getCache("users");
        User user = users.remove(id);
        if (user == null)
            {
            throw new NotFoundException("Unknown user " + id);
            }
        }

    /**
     * Obtain a {@link Session} for a tenant.
     *
     * @param tenant  the name of the tenant
     *
     * @return the {@link Session} for the tenant, or {@code null} if no
     *         {@link Session} is available for the tenant
     */
    private Session ensureSession(String tenant)
        {
        if (tenant == null || tenant.isBlank())
            {
            throw new BadRequestException("Missing tenant identifier");
            }

        TenantMetaData metaData = tenants.get(tenant);
        if (metaData == null)
            {
            throw new BadRequestException("Unknown tenant identifier \"" + tenant + "\"");
            }

        Coherence coherence = Coherence.getInstance();
        return coherence.getSessionIfPresent(tenant)
                .orElseGet(() -> createSession(coherence, metaData));
        }

    /**
     * Create a {@link Session} for a tenant
     *
     * @param coherence  the {@link Coherence} instance that will own the session
     * @param metaData   the {@link TenantMetaData tenant meta-data}
     *
     * @return the {@link Session} for the tenant
     */
    private Session createSession(Coherence coherence, TenantMetaData metaData)
        {
        String tenant = metaData.getTenant();
        coherence.addSessionIfAbsent(tenant, () -> createGrpcConfiguration(metaData));
        return coherence.getSession(tenant);
        }

    /**
     * Create a {@link Session} that will connect as a gRPC client.
     *
     * @param metaData   the {@link TenantMetaData tenant meta-data}
     *
     * @return the {@link Session} for the tenant
     */
    private SessionConfiguration createGrpcConfiguration(TenantMetaData metaData)
        {
        String hostName = metaData.getHostName();
        int port = metaData.getPort();
        ChannelCredentials credentials = InsecureChannelCredentials.create();

        ManagedChannel channel = Grpc.newChannelBuilderForAddress(hostName, port, credentials)
                .build();

        return  GrpcSessionConfiguration.builder(channel)
                .named(metaData.getTenant())
                .withSerializerFormat(metaData.getSerializer())
                .build();
        }
    }
