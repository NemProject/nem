package org.nem.nis.controller.annotations;

import java.lang.annotation.*;

/**
 * Marks controller handlers that require a fully trusted server.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface TrustedApi {
}
