package org.nem.nis.dao;

import org.flywaydb.core.Flyway;
import org.hibernate.SessionFactory;
import org.nem.deploy.appconfig.NisAppConfig;
import org.nem.nis.dbmodel.*;
import org.nem.nis.mappers.TransactionRegistry;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.hibernate4.*;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.Properties;
import java.util.function.Predicate;

@Configuration
@ComponentScan(basePackages = "org.nem.nis.dao", excludeFilters = {
		@ComponentScan.Filter(type = FilterType.ANNOTATION, value = org.springframework.stereotype.Controller.class)
})
@EnableTransactionManagement
public class TestConf {
	@Bean
	public DataSource dataSource() throws IOException {
		final DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName("org.h2.Driver");
		dataSource.setUrl("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1"); // in-memory only
		return dataSource;
	}

	@Bean(initMethod = "migrate")
	public Flyway flyway() throws IOException {
		final Flyway flyway = new Flyway();
		flyway.setDataSource(this.dataSource());
		flyway.setLocations("db/h2");
		return flyway;
	}

	@Bean
	@DependsOn("flyway")
	public SessionFactory sessionFactory() throws IOException {
		final LocalSessionFactoryBuilder localSessionFactoryBuilder = new LocalSessionFactoryBuilder(this.dataSource());
		localSessionFactoryBuilder.addProperties(this.getDbProperties(entry -> entry.startsWith("hibernate")));

		localSessionFactoryBuilder.addAnnotatedClasses(DbAccount.class);
		localSessionFactoryBuilder.addAnnotatedClasses(DbBlock.class);

		localSessionFactoryBuilder.addAnnotatedClasses(DbMultisigModification.class);
		localSessionFactoryBuilder.addAnnotatedClasses(DbMultisigSignatureTransaction.class);
		localSessionFactoryBuilder.addAnnotatedClasses(DbSend.class);
		for (final TransactionRegistry.Entry<?, ?> entry : TransactionRegistry.iterate()) {
			localSessionFactoryBuilder.addAnnotatedClasses(entry.dbModelClass);
		}

		return localSessionFactoryBuilder.buildSessionFactory();
	}

	private Properties getDbProperties(final Predicate<String> filter) throws IOException {
		final Properties dbProperties = new Properties();
		final Properties properties = new Properties();
		dbProperties.load(NisAppConfig.class.getClassLoader().getResourceAsStream("db.properties"));
		dbProperties.stringPropertyNames().stream()
				.filter(filter)
				.forEach(entry -> properties.setProperty(entry, dbProperties.getProperty(entry)));

		return properties;
	}

	@Bean
	public HibernateTransactionManager transactionManager() throws IOException {
		return new HibernateTransactionManager(this.sessionFactory());
	}
}