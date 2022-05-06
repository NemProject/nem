package org.nem.nis.controller.annotations;

import java.lang.annotation.*;

/**
 * Marks controller handlers that are publicly available (must take arguments as @RequestParam and should be GET-based) Any PublicApi is
 * also ClientApi
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PublicApi {
}
