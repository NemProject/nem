package org.nem.specific.deploy;

import org.nem.core.serialization.AccountLookup;
import org.nem.deploy.*;
import org.nem.nis.boot.NisPeerNetworkHost;
import org.nem.nis.controller.interceptors.*;
import org.nem.nis.service.BlockChainLastBlockLayer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import java.util.*;

/**
 * Class supplying Spring MVC configuration.
 */
@Configuration
@ComponentScan(basePackages = {
		"org.nem.nis.controller", "org.nem.nis.a"
})
// @EnableWebMvc // this cannot be present, when using WebMvcConfigurationSupport
public class NisWebAppInitializer extends WebMvcConfigurationSupport {
	@Autowired
	private AccountLookup accountLookup;

	@Autowired
	private NisPeerNetworkHost host;

	@Autowired
	private NisConfiguration nisConfiguration;

	@Autowired
	private BlockChainLastBlockLayer lastBlockLayer;

	@Autowired
	private LocalHostDetector localHostDetector;

	@Override
	protected void configureMessageConverters(final List<HttpMessageConverter<?>> converters) {
		addConvertersForPolicy(converters, new JsonSerializationPolicy(this.accountLookup));
		addConvertersForPolicy(converters, new BinarySerializationPolicy(this.accountLookup));
		this.addDefaultHttpMessageConverters(converters);
	}

	private static void addConvertersForPolicy(final List<HttpMessageConverter<?>> converters, final SerializationPolicy policy) {
		converters.add(new DeserializerHttpMessageConverter(policy));
		converters.add(new SerializableEntityHttpMessageConverter(policy));
		converters.add(new DeserializableEntityMessageConverter(policy));
	}

	@Override
	protected void addInterceptors(final InterceptorRegistry registry) {
		registry.addInterceptor(new LocalHostInterceptor(this.localHostDetector));
		registry.addInterceptor(this.createAuditInterceptor());
		registry.addInterceptor(new BlockLoadingInterceptor(this.lastBlockLayer));
		super.addInterceptors(registry);
	}

	private HandlerInterceptorAdapter createAuditInterceptor() {
		return new AuditInterceptor(Arrays.asList(this.nisConfiguration.getNonAuditedApiPaths()), this.host.getIncomingAudits());
	}
}
