package org.nem.deploy;

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
	DeserializerHttpMessageConverter deserializerHttpMessageConverter;

	@Autowired
	SerializableEntityHttpMessageConverter serializableEntityHttpMessageConverter;

	@Override
	protected void configureMessageConverters(final List<HttpMessageConverter<?>> converters) {
		converters.add(this.deserializerHttpMessageConverter);
		converters.add(this.serializableEntityHttpMessageConverter);
		this.addDefaultHttpMessageConverters(converters);
	}
}