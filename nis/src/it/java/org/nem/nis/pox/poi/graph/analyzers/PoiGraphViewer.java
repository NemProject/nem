package org.nem.nis.pox.poi.graph.analyzers;

import edu.uci.ics.jung.algorithms.layout.*;
import edu.uci.ics.jung.graph.*;
import edu.uci.ics.jung.graph.util.EdgeType;
import edu.uci.ics.jung.visualization.*;
import edu.uci.ics.jung.visualization.control.*;
import edu.uci.ics.jung.visualization.decorators.*;
import edu.uci.ics.jung.visualization.picking.*;
import edu.uci.ics.jung.visualization.renderers.DefaultVertexLabelRenderer;
import edu.uci.ics.jung.visualization.renderers.Renderer.VertexLabel.Position;
import org.apache.commons.collections15.Transformer;
import org.nem.core.crypto.Hashes;
import org.nem.core.math.Matrix;
import org.nem.core.math.Matrix.ReadOnlyElementVisitorFunction;
import org.nem.core.model.primitive.*;
import org.nem.core.utils.ByteUtils;
import org.nem.nis.pox.poi.graph.ClusteringResult;

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
	public static final int CIRCLE_LAYOUT = 1;
	public static final int FRUCHTERMAN_REINGOLD_LAYOUT = 2;
	public static final int KAMADA_KAWAI_LAYOUT = 3;
	public static final int ISOM_LAYOUT = 4;
	public static final int SPRING_LAYOUT = 5;
	public static final int STATIC_LAYOUT = 6;

	public static final int EDGE_TYPE_UNDIRECTED = 1;
	public static final int EDGE_TYPE_DIRECTED = 2;

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
	 * The clustering result.
	 */
	private ClusteringResult clusteringResult = null;

	/**
	 * Creates a new poi graph viewer.
	 *
	 * @param adjacencyMatrix The matrix containing all vertex/edge information.
	 * @param params A map containing additional information about the graph.
	 */
	public PoiGraphViewer(
			final Matrix adjacencyMatrix,
			final PoiGraphParameters params,
			final ClusteringResult clusteringResult) {
		if (null == adjacencyMatrix || null == params) {
			throw new IllegalArgumentException("Adjacency matrix and parameters cannot be null.");
		}

		this.graph = new SparseGraph<>();
		this.adjacencyMatrix = adjacencyMatrix;
		this.params = params;
		this.clusteringResult = clusteringResult;

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

		this.viewer = new VisualizationViewer<>(this.layout, new Dimension(800, 800));
		this.viewer.setPreferredSize(new Dimension(width, height));

		final Color bgColor = Color.decode(this.params.get("bgColor", "0xFFFFFF"));
		this.viewer.setBackground(bgColor);

		final PickedState<Integer> pickedVertexState = new MultiPickedState<>();
		final PickedState<Number> pickedEdgeState = new MultiPickedState<>();

		this.viewer.setPickedVertexState(pickedVertexState);
		this.viewer.setPickedEdgeState(pickedEdgeState);

		// Transformer maps the vertex number to a vertex property
		final Transformer<Integer, Paint> vertexColor = i -> this.getClusterColor(clusteringResult.getIdForNode(new NodeId(i)));
		this.viewer.getRenderContext().setEdgeDrawPaintTransformer(
				new PickableEdgePaintTransformer<>(this.viewer.getPickedEdgeState(), Color.black, Color.red));
		this.viewer.getRenderContext().setVertexFillPaintTransformer(vertexColor);
		//this.viewer.getRenderContext().setVertexFillPaintTransformer(
		//		new PickableVertexPaintTransformer<Integer>(this.viewer.getPickedVertexState(), Color.green, Color.yellow));

		this.viewer.getRenderContext().setVertexLabelTransformer(new ToStringLabeller<>());
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
	 * Set the clustering result.
	 */
	public ClusteringResult getClusteringResult() {
		return this.clusteringResult;
	}

	private Color getClusterColor(final ClusterId clusterId) {
		if (null == clusterId) {
			return Color.black;
		}
		if (this.clusteringResult.isRegularCluster(clusterId)) {
			final byte[] hash = Hashes.sha3_256(ByteUtils.intToBytes(clusterId.getRaw()));
			return new Color(hash[0] & 0xff, hash[1] & 0xff, hash[2] & 0xff);
		}
		if (this.clusteringResult.isHub(clusterId)) {
			return Color.red;
		}

		return Color.gray;
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
			if (!this.clusteringResult.isOutlier(this.clusteringResult.getIdForNode(new NodeId(i)))) {
				this.graph.addVertex(i);
			}
		}

		adjacencyMatrix.forEach(new ReadOnlyElementVisitorFunction() {
			int edgeCount = 0;

			@Override
			public void visit(final int row, final int col, final double value) {
				if (!PoiGraphViewer.this.getClusteringResult().isOutlier(PoiGraphViewer.this.clusteringResult.getIdForNode(new NodeId(row))) &&
						!PoiGraphViewer.this.getClusteringResult().isOutlier(PoiGraphViewer.this.clusteringResult.getIdForNode(new NodeId(col)))) {
					PoiGraphViewer.this.getGraph().addEdge(this.edgeCount++, col, row, edgeType);
				}
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
				new VisualizationImageServer<>(this.layout,
						this.viewer.getGraphLayout().getSize());

		// Configure the VisualizationImageServer
		vis.setBackground(Color.WHITE);
		vis.setPreferredSize(new Dimension(850, 850));
		vis.getRenderContext().setEdgeShapeTransformer(new EdgeShape.Line<>());
		vis.getRenderContext().setVertexLabelTransformer(new ToStringLabeller<>());
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
