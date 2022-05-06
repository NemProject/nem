package org.nem.nis.controller.annotations;

import java.lang.annotation.*;

/**
 * Marks controller handlers that can be used by client application.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ClientApi {
}
