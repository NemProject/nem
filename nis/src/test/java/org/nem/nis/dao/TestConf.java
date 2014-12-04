package org.nem.nis.dao;

import org.flywaydb.core.Flyway;
import org.hibernate.SessionFactory;
import org.nem.nis.dbmodel.*;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.hibernate4.*;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.io.IOException;

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

		// TODO: it would be nicer, no get only hibernate props and add them all at once using .addProperties(properties);
		localSessionFactoryBuilder.setProperty("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
		localSessionFactoryBuilder.setProperty("hibernate.show_sql", "false");
		localSessionFactoryBuilder.setProperty("hibernate.use_sql_comments", "false");
		localSessionFactoryBuilder.setProperty("hibernate.jdbc.batch_size", "20");

		localSessionFactoryBuilder.addAnnotatedClasses(Account.class);
		localSessionFactoryBuilder.addAnnotatedClasses(Block.class);
		localSessionFactoryBuilder.addAnnotatedClasses(Transfer.class);
		localSessionFactoryBuilder.addAnnotatedClasses(ImportanceTransfer.class);
		localSessionFactoryBuilder.addAnnotatedClasses(MultisigSignerModification.class);
		localSessionFactoryBuilder.addAnnotatedClasses(MultisigTransaction.class);
		localSessionFactoryBuilder.addAnnotatedClasses(MultisigSignature.class);
		return localSessionFactoryBuilder.buildSessionFactory();
	}

	@Bean
	public HibernateTransactionManager transactionManager() throws IOException {
		return new HibernateTransactionManager(this.sessionFactory());
	}
}