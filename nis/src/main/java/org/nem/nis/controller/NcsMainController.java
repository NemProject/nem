package org.nem.nis.controller;

import java.util.logging.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
public class NcsMainController {
	private static final Logger LOGGER = Logger.getLogger(NcsMainController.class.getName());
	
    @RequestMapping(value="/nis", method = RequestMethod.POST)
    public String index(@RequestBody String body) {
    	LOGGER.info(body);
    	return "forward:/error";
    }
}
