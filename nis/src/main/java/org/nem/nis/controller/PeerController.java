package org.nem.nis.controller;

import net.minidev.json.JSONObject;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PeerController {

    @RequestMapping(value="/peer/new", method = RequestMethod.POST)
    public String getInfo(@RequestBody String body)
    {
    	JSONObject obj=new JSONObject();
    	
		obj.put("error",new Integer(1));
		obj.put("reason","trust no one");
		
        return obj.toJSONString() + "\r\n";
    }
	
}
