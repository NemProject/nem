package org.nem.nis.controller;

import org.apache.commons.codec.DecoderException;
import org.springframework.web.bind.annotation.*;

import java.security.InvalidParameterException;
import java.util.MissingResourceException;
import java.util.logging.*;

@ResponseBody
@ControllerAdvice
public class ExceptionControllerAdvice {

    private static final Logger LOGGER = Logger.getLogger(NcsMainController.class.getName());

    @ExceptionHandler(MissingResourceException.class)
    public String handleMissingResourceException(final Exception e) {
        return Utils.jsonError(404, "invalid json"); // 404
    }

    @ExceptionHandler({ InvalidParameterException.class, DecoderException.class })
    public String handleInvalidParameterException(final Exception e) {
        return Utils.jsonError(400, "invalid json"); // 400
    }

    @ExceptionHandler(Exception.class)
    public String handleException(final Exception e) {
        // NullPointerException

        return Utils.jsonError(500, "invalid json"); // 400
    }
}
