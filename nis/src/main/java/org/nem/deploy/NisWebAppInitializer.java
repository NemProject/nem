package org.nem.deploy;

import org.nem.core.deploy.*;
import org.nem.core.serialization.AccountLookup;
import org.nem.nis.NisPeerNetworkHost;
import org.nem.nis.controller.interceptors.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.*;

import java.util.List;

/**
 * Class supplying Spring MVC configuration.
 */
@Configuration
@ComponentScan(basePackages = { "org.nem.nis.controller", "org.nem.nis.a" })
//@EnableWebMvc // this cannot be present, when using WebMvcConfigurationSupport
public class NisWebAppInitializer extends WebMvcConfigurationSupport {
	@Autowired
	private AccountLookup accountLookup;

	@Autowired
	private NisPeerNetworkHost host;

	@Override
	protected void configureMessageConverters(final List<HttpMessageConverter<?>> converters) {
		addConvertersForPolicy(converters, new JsonSerializationPolicy(this.accountLookup));
		addConvertersForPolicy(converters, new BinarySerializationPolicy(this.accountLookup));
		this.addDefaultHttpMessageConverters(converters);
	}

	private static void addConvertersForPolicy(
			final List<HttpMessageConverter<?>> converters,
			final SerializationPolicy policy) {
		converters.add(new DeserializerHttpMessageConverter(policy));
		converters.add(new SerializableEntityHttpMessageConverter(policy));
		converters.add(new DeserializableEntityMessageConverter(policy));
	}

	@Override
	protected void addInterceptors(final InterceptorRegistry registry) {
		registry.addInterceptor(new LocalHostInterceptor());
		registry.addInterceptor(new AuditInterceptor(this.host.getIncomingAudits()));
		super.addInterceptors(registry);
	}
}