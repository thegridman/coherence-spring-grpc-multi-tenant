/*
 * Copyright (c) 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.guides.client.controller;

import com.oracle.coherence.guides.client.errors.NotFoundException;
import com.oracle.coherence.guides.client.model.TenantMetaData;

import com.oracle.coherence.spring.configuration.annotation.CoherenceMap;

import com.tangosol.net.NamedMap;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * An admin CRUD controller to manage tenant metadata.
 */
@RestController
@RequestMapping(path="/tenants")
public class TenantController
    {
    /**
     * The map of tenant meta-data, this will be a Coherence cache, injected by Spring.
     */
    @CoherenceMap
    private NamedMap<String, TenantMetaData> tenants;

    @GetMapping(value = "{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TenantMetaData> get(@PathVariable("id") String id)
        {
        TenantMetaData metaData = tenants.get(id);
        if (metaData == null)
            {
            throw new NotFoundException("Unknown tenant " + id);
            }
        return ResponseEntity.ok(metaData);
        }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TenantMetaData> create(@RequestBody TenantMetaData metaData)
        {
        tenants.put(metaData.getTenant(), metaData);
        return ResponseEntity.ok(metaData);
        }

    @PutMapping(value = "{id}",
                consumes = MediaType.APPLICATION_JSON_VALUE,
                produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TenantMetaData> update(@PathVariable("id") String id,
                                       @RequestBody TenantMetaData updatedMetaData)
        {
        TenantMetaData metaData = tenants.get(id);
        if (metaData == null)
            {
            throw new NotFoundException("Unknown tenant " + id);
            }

        String type = updatedMetaData.getType();
        if (type != null && !type.isBlank())
            {
            metaData.setType(type);
            }

        int port = updatedMetaData.getPort();
        if (port > 0)
            {
            metaData.setPort(port);
            }

        tenants.put(id, metaData);

        return ResponseEntity.ok(metaData);
        }

    @DeleteMapping("{id}")
    public ResponseEntity<TenantMetaData> delete(@PathVariable("id") String id)
        {
        TenantMetaData metaData = tenants.remove(id);
        if (metaData == null)
            {
            throw new NotFoundException("Unknown tenant " + id);
            }

        return ResponseEntity.ok(metaData);
        }
    }
