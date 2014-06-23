package org.nem.deploy;

import org.nem.core.serialization.AccountLookup;
import org.nem.nis.audit.AuditCollection;
import org.nem.nis.controller.interceptors.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.*;

import java.util.List;

@Configuration
@ComponentScan(basePackages={"org.nem.nis.controller", "org.nem.nis.a"})
//@EnableWebMvc // this cannot be present, when using WebMvcConfigurationSupport
public class NisWebAppInitializer extends WebMvcConfigurationSupport  {
	@Autowired
	private AccountLookup accountLookup;

	@Autowired
	private AuditCollection auditCollection;

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
	}

	@Override
	protected void addInterceptors(final InterceptorRegistry registry) {
		registry.addInterceptor(new LocalHostInterceptor());
		registry.addInterceptor(new AuditInterceptor(this.auditCollection));
		super.addInterceptors(registry);
	}
}