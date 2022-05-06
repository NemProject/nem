package org.nem.nis.controller.annotations;

import java.lang.annotation.*;

/**
 * Marks controller handlers that are authenticated.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuthenticatedApi {
}
