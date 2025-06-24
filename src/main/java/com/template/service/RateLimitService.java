package com.template.service;

public interface RateLimitService {
    /**
     * Check if the request is allowed for the given key (IP address)
     * 
     * @param key The key to check (usually IP address)
     * @return true if request is allowed, false if rate limited
     */
    boolean isAllowed(String key);

    /**
     * Get remaining requests for the given key
     * 
     * @param key The key to check
     * @return Number of remaining requests
     */
    long getRemainingRequests(String key);

    /**
     * Get the time until reset for the given key
     * 
     * @param key The key to check
     * @return Time in seconds until reset
     */
    long getTimeUntilReset(String key);
}