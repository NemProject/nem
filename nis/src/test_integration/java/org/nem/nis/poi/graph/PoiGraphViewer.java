package org.nem.nis.poi.graph;

import edu.uci.ics.jung.algorithms.layout.*;
import edu.uci.ics.jung.graph.*;
import edu.uci.ics.jung.graph.util.EdgeType;
import edu.uci.ics.jung.visualization.*;
import edu.uci.ics.jung.visualization.control.*;
import edu.uci.ics.jung.visualization.decorators.*;
import edu.uci.ics.jung.visualization.picking.*;
import edu.uci.ics.jung.visualization.renderers.DefaultVertexLabelRenderer;
import edu.uci.ics.jung.visualization.renderers.Renderer.VertexLabel.Position;
import org.nem.core.math.Matrix;
import org.nem.core.math.Matrix.ReadOnlyElementVisitorFunction;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.*;

/**
 * Utility class for visualizing matrices as graphs. This is extremely useful for testing.
 */
public class PoiGraphViewer {
	//layouts
	public final static int CIRCLE_LAYOUT = 1;
	public final static int FRUCHTERMAN_REINGOLD_LAYOUT = 2;
	public final static int KAMADA_KAWAI_LAYOUT = 3;
	public final static int ISOM_LAYOUT = 4;
	public final static int SPRING_LAYOUT = 5;
	public final static int STATIC_LAYOUT = 6;

	public final static int EDGE_TYPE_UNDIRECTED = 1;
	public final static int EDGE_TYPE_DIRECTED = 2;

	/**
	 * The graph to show.
	 */
	private final Graph<Integer, Number> graph;

	/**
	 * The layout for the graph.
	 */
	private final Layout<Integer, Number> layout;

	/**
	 * The viewer.
	 */
	private final VisualizationViewer<Integer, Number> viewer;

	/**
	 * The adjacency matrix from which the graph is created.
	 */
	private final Matrix adjacencyMatrix;

	/**
	 * The optional parameters for the graph.
	 */
	private final PoiGraphParameters params;

	/**
	 * The cluster to be shown.
	 */
	private Cluster cluster = null;

	/**
	 * Creates a new poi graph viewer.
	 *
	 * @param adjacencyMatrix The matrix containing all vertex/edge information.
	 * @param params A map containing additional information about the graph.
	 */
	public PoiGraphViewer(final Matrix adjacencyMatrix, final PoiGraphParameters params) {
		if (null == adjacencyMatrix || null == params) {
			throw new IllegalArgumentException("Adjacency matrix and parameters cannot be null.");
		}

		this.graph = new SparseGraph<Integer, Number>();
		this.adjacencyMatrix = adjacencyMatrix;
		this.params = params;

		switch (params.getAsInteger("layout", KAMADA_KAWAI_LAYOUT)) {
			case CIRCLE_LAYOUT:
				this.layout = new CircleLayout<>(this.graph);
				break;
			case FRUCHTERMAN_REINGOLD_LAYOUT:
				this.layout = new FRLayout<>(this.graph);
				break;
			case ISOM_LAYOUT:
				this.layout = new ISOMLayout<>(this.graph);
				break;
			case SPRING_LAYOUT:
				this.layout = new edu.uci.ics.jung.algorithms.layout.SpringLayout<>(this.graph);
				break;
			case STATIC_LAYOUT:
				this.layout = new StaticLayout<>(this.graph);
				break;
			case KAMADA_KAWAI_LAYOUT:
			default:
				this.layout = new KKLayout<>(this.graph);
				break;
		}
		final int width = this.params.getAsInteger("width", 800);
		final int height = this.params.getAsInteger("height", 800);
		this.layout.setSize(new Dimension(width, height));

		this.viewer = new VisualizationViewer<Integer, Number>(this.layout, new Dimension(800, 800));
		this.viewer.setPreferredSize(new Dimension(width, height));

		final Color bgColor = Color.decode(this.params.get("bgColor", "0xFFFFFF"));
		this.viewer.setBackground(bgColor);

		final PickedState<Integer> pickedVertexState = new MultiPickedState<Integer>();
		final PickedState<Number> pickedEdgeState = new MultiPickedState<Number>();

		this.viewer.setPickedVertexState(pickedVertexState);
		this.viewer.setPickedEdgeState(pickedEdgeState);

		this.viewer.getRenderContext().setEdgeDrawPaintTransformer(
				new PickableEdgePaintTransformer<Number>(this.viewer.getPickedEdgeState(), Color.black, Color.red));
		this.viewer.getRenderContext().setVertexFillPaintTransformer(
				new PickableVertexPaintTransformer<Integer>(this.viewer.getPickedVertexState(), Color.green, Color.yellow));

		this.viewer.getRenderContext().setVertexLabelTransformer(new ToStringLabeller<Integer>());
		this.viewer.getRenderer().getVertexLabelRenderer().setPosition(Position.CNTR);
		this.viewer.getRenderContext().setLabelOffset(20);
		this.viewer.getRenderContext().getMultiLayerTransformer().getTransformer(Layer.LAYOUT).setScale(0.95, 0.95, this.viewer.getCenter());

		this.buildGraph(this.adjacencyMatrix, this.params);
	}

	/**
	 * Return the graph object.
	 */
	public Graph<Integer, Number> getGraph() {
		return this.graph;
	}

	/**
	 * Set the cluster.
	 */
	public void setCluster(final Cluster cluster) {
		this.cluster = cluster;
	}

	/**
	 * Builds the graph.
	 *
	 * @param adjacencyMatrix The matrix containing all vertex/edge information.
	 * @param params A map containing additional information about the graph.
	 */
	private void buildGraph(final Matrix adjacencyMatrix, final PoiGraphParameters params) {
		final EdgeType edgeType = (EDGE_TYPE_UNDIRECTED == params.getAsInteger("edgeType", EDGE_TYPE_UNDIRECTED)) ? EdgeType.UNDIRECTED : EdgeType.DIRECTED;
		for (int i = 0; i < adjacencyMatrix.getColumnCount(); i++) {
			this.graph.addVertex((Integer)i);
		}

		adjacencyMatrix.forEach(new ReadOnlyElementVisitorFunction() {
			int edgeCount = 0;

			@Override
			public void visit(final int row, final int col, final double value) {
				PoiGraphViewer.this.getGraph().addEdge(this.edgeCount++, col, row, edgeType);
			}
		});
	}

	/**
	 * Shows the graph in a window.
	 * The implementation is very basic ;)
	 * The method returns when the window is closed.
	 */
	public void showGraph() {
		final JPanel panel = new JPanel(new GridLayout(1, 1));
		panel.add(new GraphZoomScrollPane(this.viewer), BorderLayout.CENTER);

		final DefaultModalGraphMouse<Integer, Number> gm = new DefaultModalGraphMouse<>();
		gm.setMode(ModalGraphMouse.Mode.TRANSFORMING);
		this.viewer.setGraphMouse(gm);

		final JPanel modePanel = new JPanel();
		modePanel.setBorder(BorderFactory.createTitledBorder("Mouse Mode"));
		gm.getModeComboBox().addItemListener(gm.getModeListener());
		modePanel.add(gm.getModeComboBox());

		final JFrame frame = new JFrame("Poi Graph");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setLayout(new BorderLayout());
		frame.getContentPane().add(panel, BorderLayout.CENTER);
		frame.getContentPane().add(modePanel, BorderLayout.SOUTH);
		frame.pack();
		frame.setVisible(true);
		while (true) {
			try {
				Thread.sleep(1000);
			} catch (final InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Writes the graph to a png file.
	 *
	 * @throws IOException if there is an IO error.
	 */
	public void saveGraph() throws IOException {
		// Create the VisualizationImageServer
		final VisualizationImageServer<Integer, Number> vis =
				new VisualizationImageServer<Integer, Number>(this.layout,
						this.viewer.getGraphLayout().getSize());

		// Configure the VisualizationImageServer
		vis.setBackground(Color.WHITE);
		vis.setPreferredSize(new Dimension(850, 850));
		vis.getRenderContext().setEdgeShapeTransformer(new EdgeShape.Line<Integer, Number>());
		vis.getRenderContext().setVertexLabelTransformer(new ToStringLabeller<Integer>());
		vis.getRenderContext().setVertexLabelRenderer(new DefaultVertexLabelRenderer(Color.green));

		// Create the buffered image
		final BufferedImage image = (BufferedImage)vis.getImage(
				new Point2D.Double(this.viewer.getGraphLayout().getSize().getWidth() / 2,
						this.viewer.getGraphLayout().getSize().getHeight() / 2),
				new Dimension(this.viewer.getGraphLayout().getSize()));

		// Write a png file
		final File outputfile = new File("graph.png");

		ImageIO.write(image, "png", outputfile);
	}
}
