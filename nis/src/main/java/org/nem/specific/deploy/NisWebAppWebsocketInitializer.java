package org.nem.specific.deploy;

import org.nem.core.model.Account;
import org.nem.core.model.Address;
import org.nem.core.serialization.SimpleAccountLookup;
import org.nem.deploy.*;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;
import org.springframework.web.socket.sockjs.frame.AbstractSockJsMessageCodec;
import org.springframework.web.socket.sockjs.frame.SockJsMessageCodec;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Configuration
@ComponentScan("org.nem.nis.controller.websocket")
@EnableWebSocketMessageBroker
public class NisWebAppWebsocketInitializer extends AbstractWebSocketMessageBrokerConfigurer  {

	@Override
	public void configureMessageBroker(final MessageBrokerRegistry registry) {
		registry.enableSimpleBroker("/topic");
		registry.setApplicationDestinationPrefixes("/app");
	}

	@Override
	public void registerStompEndpoints(final StompEndpointRegistry registry) {
		registry.addEndpoint("/hello").withSockJS().setMessageCodec(
				new AbstractSockJsMessageCodec() {
					@Override
					public String[] decode(String s) throws IOException {
						return new String[0];
					}

					@Override
					public String[] decodeInputStream(InputStream inputStream) throws IOException {
						return new String[0];
					}

					@Override
					protected char[] applyJsonQuoting(String s) {
						return new char[0];
					}
				}
		);
	}
}
