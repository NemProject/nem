package org.nem.deploy;

import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;

import java.awt.BorderLayout;

import javax.swing.JLabel;

import java.awt.Color;
import java.awt.Font;

import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.UIManager;
import javax.swing.SwingConstants;

import org.eclipse.jetty.server.Server;
import org.nem.core.metadata.ApplicationMetaData;

import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NISApp implements WindowListener {
	private static final Logger LOGGER = Logger.getLogger(NISApp.class.getName());

	public static final NISApp INSTANCE = createApplicationWindow();

	private Server server;
	private boolean isAvailable;

	private JFrame frmNemInfrastructureServer;
	private JLabel lbPeers;
	private JLabel lbLastBlock;
	private JLabel lbHarvested;
	private JLabel lbApplication;
	private JLabel lbVersion;

	/**
	 * Create the application.
	 */
	protected NISApp() {

	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static NISApp createApplicationWindow() {
		NISApp result = new NISApp();
		try {
			final Class serviceManager = Class.forName("javax.jnlp.ServiceManager");
			final Method lookUpMethod = serviceManager.getMethod("lookup", String.class);
			lookUpMethod.invoke(serviceManager, "javax.jnlp.BasicService");
			result.setAvailable(true);
		} catch (Exception ex) {
			// handle exception case
			LOGGER.info("Assuming WebStart not available. GUI will not be started.");
			result.setAvailable(false);
		}

		return result;
	}

	public void setServer(Server server) {
		this.server = server;
	}

	public JLabel getLbPeers() {
		return lbPeers;
	}

	public JLabel getLbLastBlock() {
		return lbLastBlock;
	}

	public JLabel getLbHarvested() {
		return lbHarvested;
	}

	public JLabel getLbApplication() {
		return lbApplication;
	}

	public JLabel getLbVersion() {
		return lbVersion;
	}

	public boolean isAvailable() {
		return isAvailable;
	}

	public void setAvailable(boolean isAvailable) {
		this.isAvailable = isAvailable;
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		// Main Colors
		Color greenColor = new Color(0x41, 0xce, 0x7d);
		Color orangeColor = new Color(0xe2, 0xa9, 0x2b);
		Font font = new Font("Tunga", Font.PLAIN, 18);
		Font fontBig = new Font("Tunga", Font.PLAIN, 24);
		Border border = UIManager.getBorder("TitledBorder.border");

		frmNemInfrastructureServer = new JFrame();
		frmNemInfrastructureServer.getContentPane().setFont(font);
		frmNemInfrastructureServer.getContentPane().setBackground(new Color(211, 211, 211));
		frmNemInfrastructureServer.setBounds(100, 100, 376, 194);
		frmNemInfrastructureServer.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frmNemInfrastructureServer.addWindowListener(this);
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[] { 120, 120, 120, 0 };
		gridBagLayout.rowHeights = new int[] { 34, 80, 30, 0 };
		gridBagLayout.columnWeights = new double[] { 0.0, 0.0, 0.0, Double.MIN_VALUE };
		gridBagLayout.rowWeights = new double[] { 0.0, 0.0, 0.0, Double.MIN_VALUE };
		frmNemInfrastructureServer.getContentPane().setLayout(gridBagLayout);

		JPanel panel = new JPanel();
		FlowLayout flowLayout_4 = (FlowLayout) panel.getLayout();
		flowLayout_4.setVgap(1);
		flowLayout_4.setHgap(1);
		//
		panel.setBackground(greenColor);
		GridBagConstraints gbc_panel = new GridBagConstraints();
		gbc_panel.fill = GridBagConstraints.BOTH;
		gbc_panel.insets = new Insets(0, 0, 5, 0);
		gbc_panel.gridwidth = 3;
		gbc_panel.gridx = 0;
		gbc_panel.gridy = 0;
		frmNemInfrastructureServer.getContentPane().add(panel, gbc_panel);

		lbApplication = new JLabel("NEM Cloud");
		lbApplication.setFont(font);
		panel.add(lbApplication);

		JPanel panel_1 = new JPanel();
		panel_1.setBorder(new TitledBorder(null, "Connected", TitledBorder.CENTER, TitledBorder.TOP, font, null));
		panel_1.setBackground(orangeColor);
		GridBagConstraints gbc_panel_1 = new GridBagConstraints();
		gbc_panel_1.anchor = GridBagConstraints.NORTH;
		gbc_panel_1.fill = GridBagConstraints.HORIZONTAL;
		gbc_panel_1.insets = new Insets(0, 0, 5, 5);
		gbc_panel_1.gridx = 0;
		gbc_panel_1.gridy = 1;
		frmNemInfrastructureServer.getContentPane().add(panel_1, gbc_panel_1);
		panel_1.setLayout(new BorderLayout(0, 0));

		lbPeers = new JLabel("#Peers");
		lbPeers.setHorizontalAlignment(SwingConstants.CENTER);
		lbPeers.setFont(fontBig);
		panel_1.add(lbPeers);

		JPanel panel_2 = new JPanel();
		panel_2.setBorder(new TitledBorder(border, "Last Block", TitledBorder.CENTER, TitledBorder.TOP, font, null));
		panel_2.setBackground(orangeColor);
		GridBagConstraints gbc_panel_2 = new GridBagConstraints();
		gbc_panel_2.anchor = GridBagConstraints.NORTH;
		gbc_panel_2.fill = GridBagConstraints.HORIZONTAL;
		gbc_panel_2.insets = new Insets(0, 0, 5, 5);
		gbc_panel_2.gridx = 1;
		gbc_panel_2.gridy = 1;
		frmNemInfrastructureServer.getContentPane().add(panel_2, gbc_panel_2);
		panel_2.setLayout(new BorderLayout(0, 0));

		lbLastBlock = new JLabel("n/a");
		lbLastBlock.setFont(fontBig);
		lbLastBlock.setHorizontalAlignment(SwingConstants.CENTER);
		panel_2.add(lbLastBlock);

		JPanel panel_3 = new JPanel();
		panel_3.setBorder(new TitledBorder(border, "Harvested", TitledBorder.CENTER, TitledBorder.TOP, font, null));
		panel_3.setBackground(orangeColor);
		GridBagConstraints gbc_panel_3 = new GridBagConstraints();
		gbc_panel_3.anchor = GridBagConstraints.NORTH;
		gbc_panel_3.gridwidth = 0;
		gbc_panel_3.gridheight = 0;
		gbc_panel_3.fill = GridBagConstraints.HORIZONTAL;
		gbc_panel_3.insets = new Insets(0, 0, 5, 0);
		gbc_panel_3.gridx = 2;
		gbc_panel_3.gridy = 1;
		frmNemInfrastructureServer.getContentPane().add(panel_3, gbc_panel_3);
		panel_3.setLayout(new BorderLayout(0, 0));

		lbHarvested = new JLabel("n/a");
		lbHarvested.setHorizontalAlignment(SwingConstants.CENTER);
		lbHarvested.setFont(fontBig);
		panel_3.add(lbHarvested);

		JPanel panel_4 = new JPanel();
		FlowLayout flowLayout = (FlowLayout) panel_4.getLayout();
		flowLayout.setVgap(0);
		flowLayout.setAlignment(FlowLayout.RIGHT);
		panel_4.setBackground(greenColor);
		GridBagConstraints gbc_panel_4 = new GridBagConstraints();
		gbc_panel_4.anchor = GridBagConstraints.NORTH;
		gbc_panel_4.fill = GridBagConstraints.HORIZONTAL;
		gbc_panel_4.gridwidth = 3;
		gbc_panel_4.gridx = 0;
		gbc_panel_4.gridy = 2;
		frmNemInfrastructureServer.getContentPane().add(panel_4, gbc_panel_4);

		lbVersion = new JLabel("Version");
		lbVersion.setFont(font);
		panel_4.add(lbVersion);
	}

	public void showWindow() {
		if (!isAvailable) {
			return;
		}

		LOGGER.info("Open window scheduled.");
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				LOGGER.info("Processing open window.");
				try {
					initialize();
					ApplicationMetaData amd = CommonStarter.META_DATA;
					getLbVersion().setText(amd.getVersion());
					frmNemInfrastructureServer.setTitle(amd.getAppName());
					frmNemInfrastructureServer.pack();
					frmNemInfrastructureServer.setVisible(true);
				} catch (Exception e) {
					LOGGER.log(Level.WARNING, "Could not start GUI.", e);
					isAvailable = false;
				}
			}
		});
	}

	public void shutdown() {
		LOGGER.info("Shutting down Jetty Server.");
		try {
			server.stop();
		} catch (Exception e) {
			//
			LOGGER.log(Level.WARNING, "Jetty Server could not be stopped.", e);
		}
	}

	@Override
	public void windowOpened(WindowEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void windowClosing(WindowEvent e) {
		//
		LOGGER.info("shutting down...");
		shutdown();
	}

	@Override
	public void windowClosed(WindowEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void windowIconified(WindowEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void windowDeiconified(WindowEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void windowActivated(WindowEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void windowDeactivated(WindowEvent e) {
		// TODO Auto-generated method stub

	}
}
