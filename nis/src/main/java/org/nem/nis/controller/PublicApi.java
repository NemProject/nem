package org.nem.nis.controller;

/**
 * Marks controller handlers that are publicly available
 * (must take arguments as @RequestParam and should be GET-based)
 *
 * Any PublicApi is also ClientApi
 */
public @interface PublicApi {
}
