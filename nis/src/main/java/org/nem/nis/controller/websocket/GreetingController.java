package org.nem.nis.controller.websocket;

import org.nem.nis.controller.requests.HelloModel;
import org.nem.nis.controller.viewmodels.GreetingViewModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class GreetingController {

	@MessageMapping("/hello")
	@SendTo("/blocks")
	public GreetingViewModel greeting(final HelloModel helloModel) throws Exception {
		Thread.sleep(300); // simulated delay
		return new GreetingViewModel("Hello, " + helloModel.getName() + "!");
	}

	/*
	@Autowired
	private SimpMessagingTemplate messagingTemplate;

	@MessageMapping("/hello")
	public void greeting(final HelloModel helloModel) throws Exception {
		Thread.sleep(3000); // simulated delay
		messagingTemplate.convertAndSend("/blocks", new GreetingViewModel("hello " + helloModel.getName() + ", what up?"));
	}
	*/
}