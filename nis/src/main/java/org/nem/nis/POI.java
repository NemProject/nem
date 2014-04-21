/**
 * 
 */
package org.nem.nis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.nem.core.math.ColumnVector;
import org.nem.core.model.Account;
import org.nem.core.model.AccountLink;

/**
 * This is a first draft of POI.
 * 
 * Because a lot of the infrastructure is not yet in place, I am making the
 * following assumptions in this code:
 * 
 * 1) 
 * 
 */
public class POI {

	public static final double EPSILON = .00000001;

	public static final double WEIGHT = .99;

	public double getImportance() {
		return calculateImportancesImpl();
	}

	
//this is some code i wrote for school before, just putting it in here temporarily for reference
//	public static void calcWeightedPageRanks(List gains,
//			HashMap<String, Integer> directions,
//			HashMap<String, Double> transitionProbs) {
//
//		ArrayList<ArrayList<Double>> ranks = new ArrayList<ArrayList<Double>>();
//		ArrayList<DecisionPoint> nodes = new ArrayList<DecisionPoint>();
//		ArrayList<List<DecisionPoint>> nodesPointingToMe = new ArrayList<List<DecisionPoint>>();
//		ArrayList<List<DecisionPoint>> nodesPointingFromMe = new ArrayList<List<DecisionPoint>>();
//		HashMap<String, Integer> nodeIndices = new HashMap<String, Integer>();
//
//		final int V = gains.size();
//		final double IV = 1.0 / V;
//
//		int count = 0;
//		/* initialize page ranks to info gain */
//		for (final  gain : gains) {
//			nodeIndices.put(gain.getPoint().toString(), count);
//			ArrayList<DecisionPoint> friends = new ArrayList<DecisionPoint>();
//			ArrayList<DecisionPoint> outLinks = new ArrayList<DecisionPoint>();
//
//			for (Edge e : gain.getPoint().getEdges()) {
//				Integer away = directions.get(gain.getPoint().toString()
//						+ e.getOtherPoint(gain.getPoint()).toString());
//				Integer toward = directions.get(e
//						.getOtherPoint(gain.getPoint()).toString()
//						+ gain.getPoint().toString());
//				if (toward != null && (away == null || toward >= away)) {
//					// if (!(toward != null && (away == null || toward >=
//					// away))) {
//					friends.add(e.getOtherPoint(gain.getPoint()));
//				} else {
//					outLinks.add(e.getOtherPoint(gain.getPoint()));
//				}
//			}
//			nodesPointingToMe.add(friends);
//			nodesPointingFromMe.add(outLinks);
//			nodes.add(gain.getPoint());
//			ranks.add(new ArrayList<Double>() {
//				{
//					add(0, gain.getInformationGain());
//				}
//			});
//			count++;
//		}
//
//		int currIter = 1;
//		int numRunning = gains.size();
//		double newRank;
//		double inSum = 0;
//
//		while (numRunning > 0) {
//			numRunning = gains.size();
//
//			for (int ndx = 0; ndx < gains.size(); ndx++) {
//				inSum = 0;
//				double probTot = 0;
//				for ( neighbor : nodesPointingToMe.get(ndx)) {
//					if (nodeIndices.get(neighbor.toString()) == null) {
//						continue;
//					}
//					probTot += gains.get(nodeIndices.get(neighbor.toString()))
//							.getProbability();
//				}
//				for ( neighbor : nodesPointingToMe.get(ndx)) {
//					if (nodeIndices.get(neighbor.toString()) == null) {
//						continue;
//					}
//					int nOut = 0;
//					for (Edge ne : neighbor.getEdges()) {
//						Integer away = directions.get(neighbor.toString()
//								+ ne.getOtherPoint(neighbor).toString());
//						Integer toward = directions.get(ne.getOtherPoint(
//								neighbor).toString()
//								+ neighbor.toString());
//						if (away != null && (toward == null || away >= toward)) {
//							nOut++;
//						}
//					}
//					if (nOut == 0) {
//						inSum += 0;
//					} else {
//						Double transProb = transitionProbs.get(neighbor
//								.toString()
//								+ gains.get(ndx).getPoint().toString());
//						transProb = transProb == null ? 0 : transProb;
//						inSum += (1.0 / nOut)
//								* ranks.get(
//										nodeIndices.get(neighbor.toString()))
//										.get(currIter - 1);
//					}
//				}
//
//				newRank = ((1.0 - WEIGHT) * IV) + (WEIGHT * inSum);
//				ranks.get(ndx).add(newRank);
//				
//				// check for stopping condition
//				if (currIter > 1
//						&& (ranks.get(ndx).get(currIter) - ranks.get(ndx).get(
//								currIter - 1)) < EPSILON) {
//					numRunning--;
//					continue;
//				}
//			}
//			currIter++;
//		}
//
//	}
	
	// This is the draft implementation for calculating proof-of-importance
	private double[] calculateImportancesImpl(List<Account> accounts, int maxIter, double tol, String weight) {
//		 maxIter=100, tol=1.0e-8, weight='weight'
		D=G;

		// create a copy in (right) stochastic form
		W = nx.stochastic_graph(D, weight=weight);
		
		int numAccounts = accounts.size();
		
		double scale = 1.0 /numAccounts;
		
		ColumnVector importances = new ColumnVector(numAccounts);
		
		// start each node's importance as its balance
		// also go through and find dangling accounts (with 0 outDegree)
		// "dangling" nodes, no links out from them; maybe we can skip these later; we should look into that
		ArrayList<Integer> dangleIndices = new ArrayList<Integer>();
		for (int ndx = 0; ndx < numAccounts; ndx++) {
			Account currAcct = accounts.get(ndx);
			importances.setAt(ndx, currAcct.getBalance().getNumMicroNem()); //XXX:can we do this or will there be precision errors?
			LinkedList<AccountLink> outLinks = currAcct.getOutLinks();
			if (outLinks == null || outLinks.size() < 1) { //then we have a dangling account
				dangleIndices.add(ndx);
			}
		}

		// normalize starting vector to sum to 1
		importances.normalize();
		
		double maxImportance = importances.getMax();

		// calculate teleportation probabilities based on normalized amount of NEM owned
		double[] teleporations = new double[numAccounts];
		for (int acctNdx=0; acctNdx < importances.getSize(); acctNdx++) {
	        // assign a value between .7 and .95 based on the amount of NEM in an account
			// more NEM = higher teleportation seems to work better
			teleporations[acctNdx] = .7 + .25*(importances.getAt(acctNdx)/maxImportance); // the importance vector was already normalized to sum to 1 in the code above
		}
		
		
		int iterCount = 0;
		ColumnVector prevIterImportances;
		while (true) { // power iteration; do up to maxIter iterations
			
			prevIterImportances = importances;//TODO: need to do a deep copy here
			importances = dict.fromkeys(prevIterImportances.keys(),0);
			
			double dangleSum = 0;
			for (Integer dangleNdx : dangleIndices) {
				dangleSum += prevIterImportances.getAt(dangleNdx)*scale;
			}
			dangleSum = dangleSum*teleporations[dangleNdx]*scale;
			
			for (int ndx = 0; ndx < numAccounts; ndx++) {
				// this matrix multiply looks odd because it is
				// doing a left multiply x^T=xlast^T*W
				for (nbr in W[n]) {
					importances.getAt(nbr) += teleporations[n]*prevIterImportances[n]*W[n][nbr][weight];
				}
				    
				importances[n] += danglesum+(1.0-teleporations[n]);
			}

			// normalize vector
			importances.normalize();

			// check convergence using l1 norm
			double err = prevIterImportances.l1Distance(importances);
			
			if (err < tol) { //we've made it
				break;
			} else if (iterCount > maxIter) { //pwned
//				raise ConvergenceError('poi: power iteration failed to converge in %d iterations.'%(i-1));
			}
			iterCount += 1;
		}
		    
		// normalize with outlinks degree and median outlinking trans amt; otherwise people will hoard NEM
		outlinkWeights = {}
		for (n in G) {
			currNodeOut = [i[2]['weight'] for i in G.edges(data=True) if i[0] == n]
			
			median = np.median(currNodeOut);
			outDegree = len(currNodeOut);

			outlinkWeight = median*outDegree;
			if (np.isnan(outlinkWeight)) {
				outlinkWeight = 0;
			}
			    
			outlinkWeights[n] = outlinkWeight;
		}
		    
		// normalize outlink weights
		double maxRank          = np.max(x.values());
		double maxOutlinkWeight = np.max(outlinkWeights.values());
		double maxBalance = np.max(balances.values());

		// normalize x
		
		// We are going to calculate all of this now so we can use this for testing.
		double[] pois = new double[];
		double[] ows = new double[];
		double[] normBalances = new double[];
	    
		for (n in importances) {
			x[n] /= maxRank;
			
			pois.append(x[n]);
			ows.append((outlinkWeights[n] / maxOutlinkWeight));
			
			x[n] += (outlinkWeights[n] / maxOutlinkWeight);
	        
			//weight by balance at the end
			x[n] *= (balances[n] / maxBalance);
			normBalances.append(balances[n] / maxBalance);
		}   
		
		return importances;//, pois, ows, normBalances
	}

	private  getStochasticGraph(G, weight='weight') {
	    /*"""Return a right-stochastic representation of G.

	    A right-stochastic graph is a weighted graph in which all of
	    the node (out) neighbors edge weights sum to 1.
	    
	    Parameters
	    -----------
	    G : graph
	      A NetworkX graph, must have valid edge weights

	    weight : key (optional)
	      Edge data key used for weight.  If None all weights are set to 1.
	    """        */

        W=nx.DiGraph(G);

	    degree=W.out_degree(weight=weight);
	    for (u,v,d) in W.edges(data=True) {
	        d[weight]=d.get(weight,1.0)/degree[u];
	    }
	    return W
	}
}
