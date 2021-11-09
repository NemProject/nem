package org.nem.nis.controller.annotations;

import java.lang.annotation.*;

/**
 * Marks controller handlers used for peer-to-peer communication
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface P2PApi {
}
