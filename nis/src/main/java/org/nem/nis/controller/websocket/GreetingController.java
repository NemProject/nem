package org.nem.nis.controller.websocket;

import org.nem.nis.controller.requests.HelloModel;
import org.nem.nis.controller.viewmodels.GreetingViewModel;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class GreetingController {
	@MessageMapping("/hello")
	@SendTo("/topic/greetings")
	public GreetingViewModel greeting(final HelloModel helloModel) throws Exception {
		Thread.sleep(3000); // simulated delay
		return new GreetingViewModel("Hello, " + helloModel.getName() + "!");
	}

}