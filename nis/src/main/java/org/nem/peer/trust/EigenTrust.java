package org.nem.peer.trust;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.nem.peer.Node;
import org.nem.peer.PeerNetwork;

public class EigenTrust extends BasicTrust implements Trust {
	private static final Logger LOGGER = Logger.getLogger(EigenTrust.class.getName());

	/**
	 * Error margin in convergence tests
	 */
	protected final double EPSILON = 0.0001; 
	
	/**
	 * Maximal number of iterations when computing the global trust vector
	 */
	protected final int MAX_ITERATIONS = 10; 
	
	/**
	 * Weighting constant for the pretrusted peers
	 */
	protected final double ALPHA = 0.05; 
	
	/**
	 * Initial trust vector
	 */
	protected double[] pretrustVector; 
	
	/**
	 * Trust matrix
	 */
	protected double[][] trustMatrix;
	
	/**
	 * Computes the global trust for the network nodes.
	 * The global trust values are stored in the localNode object of the network.
	 * 
	 * @param network    the network of nodes to analyze.
	 */
	@Override
	public void analyze(final PeerNetwork network) {
		if (network == null) {
			throw new IllegalArgumentException("Analyze requires a PeerNetwork object not equal to null as parameter.");
		}
		Node[] peers = network.getAllPeers().toArray(new Node[network.getAllPeers().size()+1]);
		peers[peers.length-1] = network.getLocalNode();

		// Compute the trust we have in other nodes due to our own experience
		computeLocalTrust(peers[peers.length-1], network.getInitialPeerAddr());
		
		// Check for corrupt data and fix it
		fixTrust(peers);
		
		// Compute pretrust vector
		pretrustVector = computePretrust(peers, network.getInitialPeerAddr());

		// Set up the transpose of the trust matrix
		boolean outputTrustMatrix = false;
		trustMatrix = new double[peers.length][peers.length];
		for (int i=0; i<peers.length; i++) {
			for (int j=0; j<peers.length; j++) {
				NodeExperience experience = peers[i].getNodeExperience(peers[j]);
				trustMatrix[j][i] = experience.getLocalTrust();
			}
		}
		if (outputTrustMatrix) {
			outputMatrix("Trust Matrix", trustMatrix);
		}
		
		// Calculate global trust
		double[] globalTrust = computeGlobalTrust();
		
		// You can discover the trust we have in a peer by 
		// calling getNodeExperience(address).getGlobalTrust() on the localNode object if you have the peer's address.
		for (int i=0; i<peers.length; i++) {
			NodeExperience experience = peers[peers.length-1].getNodeExperience(peers[i]);
			if (experience == null) {
				peers[peers.length-1].setNodeExperience(peers[i], new NodeExperience());
			}
			peers[peers.length-1].getNodeExperience(peers[i]).setGlobalTrust(globalTrust[i]);
		}
	}

	/**
	 * Temporary: only for debugging purposes!
	 */
	protected void outputMatrix(String matrixName, double[][] matrix) {
		DecimalFormat f = new DecimalFormat("#0.000");
		String row;
		System.out.println(matrixName);
		for (int i=0; i<matrix.length; i++) {
			row = "";
			for (int j=0; j<matrix.length; j++) {
				row += f.format(matrix[i][j]) + "  ";
			}
			System.out.println(row);
		}
		System.out.println("");
	}
	
	/**
	 * Since the peers could fool us by giving us invalid data,
	 * we have to check for severe problems and fix them. 
	 */
	@Override
	public void fixTrust(Node[] peers) {
		double[] vector = new double[peers.length];
		for (int i=0; i<peers.length; i++) {
			for (int j=0; j<peers.length; j++) {
				NodeExperience experience = peers[i].getNodeExperience(peers[j]);
				if (experience != null) {
					vector[j] = experience.getLocalTrust();
					if (Double.isNaN(vector[j]) || Double.isInfinite(vector[j])) {
						vector[j] = 0.0;
					}
					if (experience.getSuccessfulCalls() < 0) {
						experience.setSuccessfulCalls(0);
					}
					if (experience.getFailedCalls() < 0) {
						experience.setFailedCalls(0);
					}
				}
				else {
					vector[j] = 0.0;
					peers[i].setNodeExperience(peers[j], new NodeExperience());
				}
			}
			normalize(vector);
			for (int j=0; j<peers.length; j++) {
				peers[i].getNodeExperience(peers[j]).setLocalTrust(vector[j]);
			}
		}
	}
	
	/**
	 * Compute the pretrust vector 
	 *
	 */
	@Override
	public double[] computePretrust(final Node[] peers, final Set<String> initialPeerAddr) {
		double[] p = new double[peers.length];
		
		for (int i=0; i<peers.length; i++) {
			if (initialPeerAddr.size() > 0) {
				if (initialPeerAddr.contains(peers[i].getAddress())) {
					p[i] = 1.0/initialPeerAddr.size();
				}
				else {
					p[i] = 0.0;
				}
			}
			else {
				p[i] = 1.0/peers.length;
			}
		}
		
		return p;
	}
	
	/**
	 * Compute the trust a node has in other nodes due to its own experience with them.
	 */
	@Override
	public void computeLocalTrust(Node peer, final Set<String> initialPeerAddr) {
		long numCalls;
		double sum=0.0;
		HashMap<Node, Double> tmp = new HashMap<Node, Double>();
		for (Map.Entry<Node, NodeExperience> entry : peer.getNodeExperiences().entrySet()) {
			NodeExperience experience = entry.getValue();
			numCalls = experience.getSuccessfulCalls() + experience.getFailedCalls();
			if (numCalls > 0) {
				tmp.put(entry.getKey(),Math.max((double)(experience.getSuccessfulCalls() - experience.getFailedCalls())/(double)numCalls, 0.0));
			}
			else {
				if (initialPeerAddr.contains(entry.getKey().getAddress()) || peer == entry.getKey()) {
					tmp.put(entry.getKey(), 1.0);
				}
				else {
					tmp.put(entry.getKey(), 0.0);
				}
			}
			sum += Math.abs(tmp.get(entry.getKey()));
		}
		
		for (Map.Entry<Node, NodeExperience> entry : peer.getNodeExperiences().entrySet()) {
			NodeExperience experience = entry.getValue();
			experience.setLocalTrust(tmp.get(entry.getKey())/sum);
		}
	}
	
	@Override
	public void computeFeedbackCredibility(Node peer, final Node[] peers) {
		// Do nothing
	}
	
	/**
	 * Main loop for computing the global trust in a node
	 */
	@Override
	public double[] computeGlobalTrust() {
		double[] vector1, vector2;
		int max_iterations = MAX_ITERATIONS;
		vector1 = singleIteration(pretrustVector);
		do {
			vector2 = singleIteration(vector1);
			vector1 = singleIteration(vector2);
			max_iterations -= 2;
		} while((max_iterations > 0) && !hasConverged(vector1, vector2));
	
		return vector1.clone();
	}
	
	/**
	 * Single iteration for computing the global trust in a node
	 */
	protected double[] singleIteration(double[] vector) {
		double[] tmp1 = matrixMult(trustMatrix, vector);
		tmp1 = scalarMult((1-ALPHA), tmp1);
		double[] tmp2 = scalarMult(ALPHA, pretrustVector);
		
		return add(tmp1,tmp2);	
	}
	
	/**
	 * Test if the difference of two vectors is below a given threshold.
	 * (original paper proposes euclidean metric)
	 */
	protected boolean hasConverged(final double[] vector1, final double[] vector2) {
		for(int i=0; i < vector1.length; i++) {
			if (Math.abs(vector1[i]-vector2[i]) > this.EPSILON)
				return false;
		}
		return true;
	}

	/**
	 * Linear algebra stuff
	 */
	
	/**
	 * Normalize a vector
	 */
	protected void normalize(double[] vector) {
		double sum = 0.0;
		for (int i=0; i<vector.length; i++) {
			sum += Math.abs(vector[i]);
		}
		if (sum > 0.0) {
			for (int i=0; i<vector.length; i++) {
				vector[i] /= sum;
			}
		}
	}

	/**
	 * Transpose a (n x m)-matrix
	 */
	protected double[][] transpose(final double[][] matrix) {
		double[][] result = new double[matrix[0].length][matrix.length];
		for (int i=0; i<matrix.length; i++) {
			for (int j=0; j<matrix[0].length; j++) {
				result[j][i] = matrix[i][j];
			}
		}
		
		return result;
	}
	
	/**
	 * Add two vectors
	 */
	protected double[] add(final double[] vector1, final double[] vector2) {
		double[] result = new double[vector1.length];
		for (int i=0; i<vector1.length; i++) {
			result[i] = vector1[i] + vector2[i];
		}
		
		return result;
	}
	
	/**
	 * Scalar times vector
	 */
	protected double[] scalarMult(final double constant, final double[] vector) {
		double[] result = new double[vector.length];
		for (int i=0; i<vector.length; i++) {
			result[i] = vector[i] * constant;
		}
		
		return result;
	}
	
	/**
	 * Matrix times vector
	 */
	protected double[] matrixMult(final double[][] matrix, final double[] vector) {
		double[] result = new double[vector.length];
		for (int i=0; i<vector.length; i++) {
			result[i] = 0.0;
			for(int j=0; j < vector.length; j++) {
				result[i] += matrix[i][j] * vector[j];
			}
		}
		
		return result;
	}
	
	/**
	 * Normalize each column of a given matrix in case there is a non zero entry
	 */
	protected void normalizeColumns(double[][] matrix) {
		double sum=0.0;
		for (int i=0; i<matrix.length; i++) {
			sum=0.0;
			for (int j=0; j<matrix.length; j++) {
				sum += Math.abs(matrix[j][i]);
			}
			if (sum > 0.0) {
				for (int j=0; j<matrix.length; j++) {
					matrix[j][i] /= sum;
				}
			}
		}
	}
}
