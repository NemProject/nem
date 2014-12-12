package org.nem.deploy.appconfig;

import org.flywaydb.core.Flyway;
import org.hibernate.SessionFactory;
import org.nem.core.deploy.*;
import org.nem.nis.cache.HashCache;
import org.nem.core.time.TimeProvider;
import org.nem.deploy.*;
import org.nem.nis.*;
import org.nem.nis.audit.AuditCollection;
import org.nem.nis.boot.PeerNetworkScheduler;
import org.nem.nis.cache.*;
import org.nem.nis.dao.*;
import org.nem.nis.dbmodel.*;
import org.nem.nis.harvesting.*;
import org.nem.nis.poi.*;
import org.nem.nis.secret.BlockTransactionObserverFactory;
import org.nem.nis.service.*;
import org.nem.nis.sync.*;
import org.nem.nis.validators.*;
import org.nem.peer.connect.*;
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

	@Autowired
	private ImportanceTransferDao importanceTransferDao;

	private static final int MAX_AUDIT_HISTORY_SIZE = 50;

	@Bean
	protected AuditCollection outgoingAudits() {
		return this.createAuditCollection();
	}

	@Bean
	protected AuditCollection incomingAudits() {
		return this.createAuditCollection();
	}

	private AuditCollection createAuditCollection() {
		return new AuditCollection(MAX_AUDIT_HISTORY_SIZE, this.timeProvider());
	}

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
		flyway.setValidateOnMigrate(Boolean.valueOf(prop.getProperty("flyway.validate")));
		return flyway;
	}

	@Bean
	@DependsOn("flyway")
	public SessionFactory sessionFactory() throws IOException {
		final LocalSessionFactoryBuilder localSessionFactoryBuilder = new LocalSessionFactoryBuilder(this.dataSource());
		localSessionFactoryBuilder.addProperties(this.getDbProperties(entry -> entry.startsWith("hibernate")));
		localSessionFactoryBuilder.addAnnotatedClasses(Account.class);
		localSessionFactoryBuilder.addAnnotatedClasses(Block.class);
		localSessionFactoryBuilder.addAnnotatedClasses(Transfer.class);
		localSessionFactoryBuilder.addAnnotatedClasses(ImportanceTransfer.class);
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
		return new BlockChain(
				this.blockChainLastBlockLayer,
				this.blockChainUpdater());
	}

	@Bean
	public BlockChainServices blockChainServices() {
		return new BlockChainServices(
				this.blockDao,
				this.blockTransactionObserverFactory(),
				this.blockValidatorFactory(),
				this.transactionValidatorFactory());
	}

	@Bean
	public BlockChainUpdater blockChainUpdater() {
		return new BlockChainUpdater(
				this.nisCache(),
				this.accountDao,
				this.blockChainLastBlockLayer,
				this.blockDao,
				this.blockChainContextFactory(),
				this.unconfirmedTransactions(),
				this.nisConfiguration());
	}

	@Bean
	public BlockChainContextFactory blockChainContextFactory() {
		return new BlockChainContextFactory(
				this.nisCache(),
				this.blockChainLastBlockLayer,
				this.blockDao,
				this.blockChainServices(),
				this.unconfirmedTransactions());
	}

	@Bean
	public BlockTransactionObserverFactory blockTransactionObserverFactory() {
		return new BlockTransactionObserverFactory();
	}

	@Bean
	public BlockValidatorFactory blockValidatorFactory() {
		return new BlockValidatorFactory(this.timeProvider());
	}

	@Bean
	public TransactionValidatorFactory transactionValidatorFactory() {
		return new TransactionValidatorFactory(this.timeProvider(), this.poiOptions());
	}

	@Bean
	public SingleTransactionValidator transactionValidator() {
		// this is only consumed by the TransactionController and used in transaction/prepare,
		// which doesn't require a hash check, so createSingle is used
		return this.transactionValidatorFactory().createSingle(this.accountStateCache());
	}

	@Bean
	public BatchTransactionValidator batchTransactionValidator() {
		return this.transactionValidatorFactory().createBatch(this.transactionHashCache());
	}

	@Bean
	public Harvester harvester() {
		final BlockGenerator generator = new BlockGenerator(
				this.nisCache(),
				this.unconfirmedTransactions(),
				this.blockDao,
				new BlockScorer(this.accountStateCache()),
				this.blockValidatorFactory().create(this.nisCache()));
		return new Harvester(
				this.accountCache(),
				this.timeProvider(),
				this.blockChainLastBlockLayer,
				this.unlockedAccounts(),
				generator);
	}

	@Bean
	public AccountCache accountCache() {
		return new AccountCache();
	}

	@Bean
	public AccountStateCache accountStateCache() {
		return new DefaultAccountStateCache();
	}

	@Bean
	public HashCache transactionHashCache() {
		return new HashCache(50000, this.nisConfiguration().getTransactionHashRetentionTime());
	}

	@Bean
	public SynchronizedPoiFacade poiFacade() {
		return new SynchronizedPoiFacade(new DefaultPoiFacade(this.importanceCalculator()));
	}

	@Bean
	public ReadOnlyNisCache nisCache() {
		return new DefaultNisCache(
				this.accountCache(),
				this.accountStateCache(),
				this.poiFacade(),
				this.transactionHashCache());
	}

	@Bean
	public ImportanceCalculator importanceCalculator() {
		return new PoiImportanceCalculator(new PoiScorer(), this.poiOptions());
	}

	@Bean
	public UnlockedAccounts unlockedAccounts() {
		return new UnlockedAccounts(
				this.accountCache(),
				this.accountStateCache(),
				this.blockChainLastBlockLayer,
				this.canHarvestPredicate(),
				this.nisConfiguration().getUnlockedLimit());
	}

	@Bean
	public CanHarvestPredicate canHarvestPredicate() {
		return new CanHarvestPredicate(this.poiOptions().getMinHarvesterBalance());
	}

	@Bean
	public PoiOptions poiOptions() {
		return new PoiOptionsBuilder().create();
	}

	@Bean
	public UnconfirmedTransactions unconfirmedTransactions() {
		return new UnconfirmedTransactions(
				this.transactionValidatorFactory(),
				this.nisCache());
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
				this.nisCache(),
				this.nisPeerNetworkHost(),
				this.nisConfiguration(),
				this.blockAnalyzer());
	}

	@Bean
	public BlockAnalyzer blockAnalyzer() {
		return new BlockAnalyzer(this.blockDao, this.blockChainUpdater(), this.blockChainLastBlockLayer);
	}

	@Bean
	public HttpConnectorPool httpConnectorPool() {
		final CommunicationMode communicationMode = this.nisConfiguration().useBinaryTransport()
				? CommunicationMode.BINARY
				: CommunicationMode.JSON;
		return new HttpConnectorPool(communicationMode, this.outgoingAudits());
	}

	@Bean
	public NisPeerNetworkHost nisPeerNetworkHost() {
		final PeerNetworkScheduler scheduler = new PeerNetworkScheduler(this.timeProvider(), this.blockChain(), this.harvester());
		final CountingBlockSynchronizer synchronizer = new CountingBlockSynchronizer(this.blockChain());

		return new NisPeerNetworkHost(
				this.nisCache(),
				synchronizer,
				scheduler,
				this.chainServices(),
				this.nisConfiguration(),
				this.httpConnectorPool(),
				this.incomingAudits(),
				this.outgoingAudits());
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

	@Bean
	public ChainServices chainServices() {
		return new ChainServices(this.blockChainLastBlockLayer, this.httpConnectorPool());
	}

	@Bean
	public CommonStarter commonStarter() {
		return CommonStarter.INSTANCE;
	}
}
