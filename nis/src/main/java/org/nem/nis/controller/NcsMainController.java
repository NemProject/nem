package org.nem.nis.controller;

import java.io.IOException;

import java.util.logging.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
public class NcsMainController {
	private static final Logger logger = Logger.getLogger(NcsMainController.class.getName());
	
    @RequestMapping(value="/nis", method = RequestMethod.POST)
    public String index(@RequestBody String body) throws IOException
    {
    	logger.info(body);
    	
    	return "forward:/error";
    }
}
