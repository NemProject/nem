package org.nem.deploy.appconfig;

import org.flywaydb.core.Flyway;
import org.hibernate.SessionFactory;
import org.nem.core.deploy.*;
import org.nem.core.time.TimeProvider;
import org.nem.deploy.*;
import org.nem.nis.*;
import org.nem.nis.dao.*;
import org.nem.nis.dbmodel.*;
import org.nem.nis.poi.*;
import org.nem.nis.service.BlockChainLastBlockLayer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.hibernate4.*;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.Properties;
import java.util.function.Predicate;

@Configuration
@ComponentScan(
		basePackages = { "org.nem.nis" },
		excludeFilters = { @ComponentScan.Filter(type = FilterType.ANNOTATION, value = org.springframework.stereotype.Controller.class)
		})
@EnableTransactionManagement
public class NisAppConfig {

	@Autowired
	private AccountDao accountDao;

	@Autowired
	private BlockDao blockDao;

	@Autowired
	private BlockChainLastBlockLayer blockChainLastBlockLayer;

	@Autowired
	private TransferDao transferDao;

	@Bean
	public DataSource dataSource() throws IOException {
		final NisConfiguration configuration = this.nisConfiguration();
		final String nemFolder = configuration.getNemFolder();
		final Properties prop = new Properties();
		prop.load(NisAppConfig.class.getClassLoader().getResourceAsStream("db.properties"));
		//Replace ${nisFolder} with the value from configuration
		final String jdbcUrl = prop.getProperty("jdbc.url").replace("${nem.folder}", nemFolder);

		final DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName(prop.getProperty("jdbc.driverClassName"));
		dataSource.setUrl(jdbcUrl);
		dataSource.setUsername(prop.getProperty("jdbc.username"));
		dataSource.setPassword(prop.getProperty("jdbc.password"));
		return dataSource;
	}

	@Bean(initMethod = "migrate")
	public Flyway flyway() throws IOException {
		final Properties prop = new Properties();
		prop.load(NisAppConfig.class.getClassLoader().getResourceAsStream("db.properties"));

		final org.flywaydb.core.Flyway flyway = new Flyway();
		flyway.setDataSource(this.dataSource());
		flyway.setClassLoader(NisAppConfig.class.getClassLoader());
		flyway.setLocations(prop.getProperty("flyway.locations"));
		// TODO-CR: 20140817 J->B why are different line-endings causing validation to fail?
		flyway.setValidateOnMigrate(false);
		return flyway;
	}

	@Bean
	@DependsOn("flyway")
	public SessionFactory sessionFactory() throws IOException {
		final LocalSessionFactoryBuilder localSessionFactoryBuilder = new LocalSessionFactoryBuilder(this.dataSource());

		// TODO: it would be nicer, no get only hibernate props and add them all at once using .addProperties(properties);
		// BR: like this?
		localSessionFactoryBuilder.addProperties(this.getDbProperties(entry -> entry.startsWith("hibernate")));

		localSessionFactoryBuilder.addAnnotatedClasses(Account.class);
		localSessionFactoryBuilder.addAnnotatedClasses(Block.class);
		localSessionFactoryBuilder.addAnnotatedClasses(Transfer.class);
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
	public BlockChain blockChain() {
		return new BlockChain(this.accountAnalyzer(), this.accountDao, this.blockChainLastBlockLayer, this.blockDao, this.transferDao, this.foraging());
	}

	@Bean
	public Foraging foraging() {
		return new Foraging(
				this.accountCache(),
				this.poiFacade(),
				this.blockDao,
				this.blockChainLastBlockLayer,
				this.transferDao);
	}

	@Bean
	public AccountCache accountCache() {
		return new AccountCache();
	}

	@Bean
	public PoiFacade poiFacade() {
		return new PoiFacade(new PoiAlphaImportanceGeneratorImpl());
	}

	public AccountAnalyzer accountAnalyzer() {
		return new AccountAnalyzer(this.accountCache(), this.poiFacade());
	}

	@Bean
	public HibernateTransactionManager transactionManager() throws IOException {
		return new HibernateTransactionManager(this.sessionFactory());
	}

	@Bean
	public NisMain nisMain() {
		return new NisMain(
				this.accountDao,
				this.blockDao,
				this.accountAnalyzer(),
				this.blockChain(),
				this.nisPeerNetworkHost(),
				this.blockChainLastBlockLayer,
				this.nisConfiguration());
	}

	@Bean
	public NisPeerNetworkHost nisPeerNetworkHost() {
		return new NisPeerNetworkHost(this.accountCache(), this.blockChain(), this.nisConfiguration());
	}

	@Bean
	public NisConfiguration nisConfiguration() {
		return new NisConfiguration();
	}

	@Bean
	public TimeProvider timeProvider() {
		return CommonStarter.TIME_PROVIDER;
	}

	@Bean
	public NemConfigurationPolicy configurationPolicy() {
		return new NisConfigurationPolicy();
	}
}
