package org.nem.specific.deploy;


import net.minidev.json.JSONArray;
import net.minidev.json.JSONValue;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;
import org.springframework.web.socket.sockjs.frame.AbstractSockJsMessageCodec;

import java.io.IOException;
import java.io.InputStream;

@Configuration
@ComponentScan("org.nem.nis.controller.websocket")
@EnableWebSocketMessageBroker
public class NisWebAppWebsocketInitializer extends AbstractWebSocketMessageBrokerConfigurer  {

	@Override
	public void configureMessageBroker(final MessageBrokerRegistry registry) {
		registry.enableSimpleBroker("/topic");
		registry.setApplicationDestinationPrefixes("/w/api");
	}

	@Override
	public void registerStompEndpoints(final StompEndpointRegistry registry) {
		registry.addEndpoint("/hello").withSockJS().setMessageCodec(
				new AbstractSockJsMessageCodec() {
					@Override
					public String[] decode(String s) throws IOException {
						return new String[] { (String) ((JSONArray)JSONValue.parse(s)).get(0)};
					}

					@Override
					public String[] decodeInputStream(InputStream inputStream) throws IOException {
						return new String[0];
					}

					@Override
					protected char[] applyJsonQuoting(String s) {
						return JSONValue.escape(s).toCharArray();
					}
				}
		);
	}
}
