package org.nem.nis.dao;

import org.flywaydb.core.Flyway;
import org.hibernate.SessionFactory;
import org.nem.core.model.Address;
import org.nem.nis.cache.*;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.hibernate4.HibernateTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.Collection;
import java.util.function.Function;

@Configuration
@ComponentScan(basePackages = "org.nem.nis.dao", excludeFilters = @ComponentScan.Filter(type = FilterType.ANNOTATION, value = org.springframework.stereotype.Controller.class))
@EnableTransactionManagement
public class TestConf {
	@Bean
	public DataSource dataSource() {
		final DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName("org.h2.Driver");
		dataSource.setUrl("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1"); // in-memory only
		return dataSource;
	}

	@Bean(initMethod = "migrate")
	public Flyway flyway() {
		final Flyway flyway = new Flyway();
		flyway.setDataSource(this.dataSource());
		flyway.setLocations("db/h2");
		return flyway;
	}

	@Bean
	@DependsOn("flyway")
	public SessionFactory sessionFactory() throws IOException {
		return SessionFactoryLoader.load(this.dataSource());
	}

	@Bean
	public HibernateTransactionManager transactionManager() throws IOException {
		return new HibernateTransactionManager(this.sessionFactory());
	}

	@Bean
	public SynchronizedAccountStateCache accountStateCache() {
		return new SynchronizedAccountStateCache(new DefaultAccountStateCache());
	}

	@Bean
	public Function<Address, Collection<Address>> cosignatoryLookup() {
		return a -> this.accountStateCache().findStateByAddress(a).getMultisigLinks().getCosignatories();
	}

	@Bean
	public MosaicIdCache mosaicIdCache() {
		return new SynchronizedMosaicIdCache(new DefaultMosaicIdCache());
	}
}
