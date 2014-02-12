package org.nem.nis.controller;

import net.minidev.json.JSONObject;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InfoController {

    @RequestMapping(value="/getInfo", method = RequestMethod.POST)
    public String getInfo(@RequestBody String body)
    {
    	JSONObject obj=new JSONObject();
		obj.put("protocol",new Integer(1));
		obj.put("scheme","http");
		obj.put("application","NIS");
		obj.put("version","0.1.0");
		obj.put("platform", "PC x64");
		obj.put("port","7890");
		obj.put("shareAddress", new Boolean(true));
		
        return obj.toJSONString() + "\r\n";
    }
	
}
