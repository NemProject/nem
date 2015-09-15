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
	private final SimpMessagingTemplate messagingTemplate;

	@Autowired(required = true)
	public GreetingController(final SimpMessagingTemplate messagingTemplate) {
		this.messagingTemplate = messagingTemplate;
	}

	@MessageMapping("/hello")
	public void greeting(final HelloModel helloModel) throws Exception {
		Thread.sleep(300); // simulated delay
		this.messagingTemplate.convertAndSend("/blocks", new GreetingViewModel("hello " + helloModel.getName() + ", what up?"));
		Thread.sleep(300); // simulated delay
		this.messagingTemplate.convertAndSend("/blocks", new GreetingViewModel("so interesting!"));
	}
}