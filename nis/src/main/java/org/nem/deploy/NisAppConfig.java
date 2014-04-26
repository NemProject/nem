package org.nem.deploy;

import com.googlecode.flyway.core.Flyway;
import org.hibernate.SessionFactory;
import org.nem.nis.*;
import org.nem.nis.controller.utils.RequiredBlockDaoAdapter;
import org.nem.nis.dao.BlockDao;
import org.nem.nis.dbmodel.Account;
import org.nem.nis.dbmodel.Block;
import org.nem.nis.dbmodel.Transfer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.hibernate4.HibernateTransactionManager;
import org.springframework.orm.hibernate4.LocalSessionFactoryBuilder;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.Properties;

@Configuration
@ComponentScan(basePackages = "org.nem.nis", excludeFilters = {
		@ComponentScan.Filter(type = FilterType.ANNOTATION, value = org.springframework.stereotype.Controller.class)
})
@EnableTransactionManagement
public class NisAppConfig {

	@Autowired
	private BlockDao blockDao;

	@Bean
	public DataSource dataSource() throws IOException {
		final Properties prop = new Properties();
		prop.load(NisAppConfig.class.getClassLoader().getResourceAsStream("db.properties"));

		final DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName(prop.getProperty("jdbc.driverClassName"));
		dataSource.setUrl(prop.getProperty("jdbc.url"));
		dataSource.setUsername(prop.getProperty("jdbc.username"));
		dataSource.setPassword(prop.getProperty("jdbc.password"));
		return dataSource;
	}

	@Bean(initMethod = "migrate")
	public Flyway flyway() throws IOException {
		final Properties prop = new Properties();
		prop.load(NisAppConfig.class.getClassLoader().getResourceAsStream("db.properties"));

		final Flyway flyway = new Flyway();
		flyway.setDataSource(this.dataSource());
		flyway.setLocations(prop.getProperty("flyway.locations"));
		return flyway;
	}

	@Bean
	@DependsOn("flyway")
	public SessionFactory sessionFactory() throws IOException {
		final LocalSessionFactoryBuilder localSessionFactoryBuilder = new LocalSessionFactoryBuilder(this.dataSource());
		localSessionFactoryBuilder.addAnnotatedClasses(Account.class);
		localSessionFactoryBuilder.addAnnotatedClasses(Block.class);
		localSessionFactoryBuilder.addAnnotatedClasses(Transfer.class);
		return localSessionFactoryBuilder.buildSessionFactory();
	}

	@Bean
	public BlockChain blockChain() {
		return new BlockChain();
	}

	@Bean
	public Foraging foraging() {
		return new Foraging();
	}

	@Bean
	public AccountAnalyzer accountAnalyzer() {
		return new AccountAnalyzer();
	}

	@Bean
	public HibernateTransactionManager transactionManager() throws IOException {
		return new HibernateTransactionManager(this.sessionFactory());
	}

	@Bean
	public NisMain nisMain() {
		return new NisMain();
	}

	@Bean
	public NisPeerNetworkHost nisPeerNetworkHost() {
		return new NisPeerNetworkHost();
	}

	@Bean
	public RequiredBlockDaoAdapter requiredBlockDaoAdapter() {
		return new RequiredBlockDaoAdapter(this.blockDao);
	}

	@Bean
	public DeserializerHttpMessageConverter deserializerHttpMessageConverter() {
		return new DeserializerHttpMessageConverter(this.accountAnalyzer());
	}

	@Bean
	public SerializableEntityHttpMessageConverter serializableEntityHttpMessageConverter() {
		return new SerializableEntityHttpMessageConverter(this.deserializerHttpMessageConverter());
	}
}
