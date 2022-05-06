package org.nem.specific.deploy;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import org.nem.core.serialization.Deserializer;
import org.nem.core.serialization.JsonDeserializer;
import org.nem.core.serialization.SerializableEntity;
import org.nem.deploy.JsonSerializationPolicy;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.web.socket.config.annotation.*;
import org.springframework.web.socket.sockjs.frame.AbstractSockJsMessageCodec;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

@Configuration
@ComponentScan("org.nem.nis.websocket")
@EnableWebSocketMessageBroker
public class NisWebAppWebsocketInitializer extends AbstractWebSocketMessageBrokerConfigurer {

	@Override
	public void configureMessageBroker(final MessageBrokerRegistry registry) {
		registry.enableSimpleBroker("/blocks", "/unconfirmed", "/errors", "/account", "/transactions", "/recenttransactions", "/node");
		registry.setApplicationDestinationPrefixes("/w/api");
	}

	@Override
	public boolean configureMessageConverters(final List<MessageConverter> converters) {
		converters.add(new MessageConverter() {
			@Override
			public Object fromMessage(final Message<?> message, Class<?> targetClass) {
				final Object parsedObject = JSONValue.parse((byte[]) message.getPayload());
				if (!(parsedObject instanceof JSONObject)) {
					throw new RuntimeException(String.format("unexpected data: %s", parsedObject));

				}
				final Deserializer deserializer = new JsonDeserializer((JSONObject) parsedObject, null);
				return this.createInstance(targetClass, deserializer);
			}

			private Constructor<?> getConstructor(final Class<?> aClass) {
				try {
					return aClass.getConstructor(Deserializer.class);
				} catch (final NoSuchMethodException e) {
					return null;
				}
			}

			private Object createInstance(final Class<?> aClass, final Deserializer deserializer) {
				try {
					final Constructor<?> constructor = this.getConstructor(aClass);
					if (null == constructor) {
						throw new UnsupportedOperationException("could not find compatible constructor");
					}

					return constructor.newInstance(deserializer);
				} catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
					if (e.getCause() instanceof RuntimeException) {
						throw (RuntimeException) e.getCause();
					}

					throw new UnsupportedOperationException("could not instantiate object");
				}
			}
			@Override
			public Message<?> toMessage(final Object payload, MessageHeaders header) {
				final JsonSerializationPolicy policy = new JsonSerializationPolicy(null);
				return new GenericMessage<>(policy.toBytes((SerializableEntity) payload), header);
			}
		});
		return true;
	}

	@Override
	public void registerStompEndpoints(final StompEndpointRegistry registry) {
		registry.addEndpoint("/messages").setAllowedOrigins("*").withSockJS().setMessageCodec(new AbstractSockJsMessageCodec() {
			@Override
			public String[] decode(String s) {
				return new String[]{
						(String) ((JSONArray) JSONValue.parse(s)).get(0)
				};
			}

			@Override
			public String[] decodeInputStream(InputStream inputStream) throws IOException {
				return new String[0];
			}

			@Override
			protected char[] applyJsonQuoting(String s) {
				return JSONValue.escape(s).toCharArray();
			}
		});
	}
}
