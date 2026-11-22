package com.benzaazaa.codsettlement.common.error;

/**
 * The resource does not exist <em>for the caller</em>. A merchant asking for another
 * merchant's order gets this too (404, not 403), so IDs of other tenants never leak.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resource, Object id) {
        super(resource + " " + id + " not found");
    }
}
