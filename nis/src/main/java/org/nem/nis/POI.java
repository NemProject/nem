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
import org.nem.core.utils.ArrayUtils;

/**
 * This is a first draft of POI.
 * 
 * Because a lot of the infrastructure is not yet in place, I am making the
 * following assumptions in this code:
 * 
 * 1) This class is called with all the accounts.
 * 2) POI is calculated by the forager after processing new transactions. 
 *    This algorithm is not currently iterative, so importances are calculated from scratch every time. 
 *    I plan to make this iterative so that we update importances only for accounts affected by new transactions and their links.
 * 
 */
public class POI {

	public static final double EPSILON = .00000001;

	public static final double WEIGHT = .99;

	public double getImportance() {
		return calculateImportancesImpl();
	}

	// This is the draft implementation for calculating proof-of-importance
	private double[] calculateImportancesImpl(List<Account> accounts, int maxIter, double tol) {
//		 maxIter=100, tol=1.0e-8, weight='weight'
		
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
			LinkedList<AccountLink> outlinks = currAcct.getOutlinks();
			if (outlinks == null || outlinks.size() < 1) { //then we have a dangling account
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
			// NOTE: importances were initialized with acct balances, so this is why this works
			teleporations[acctNdx] = .7 + .25*(importances.getAt(acctNdx)/maxImportance); // the importance vector was already normalized to sum to 1 in the code above
		}
		
		int iterCount = 0;
		ColumnVector prevIterImportances;
		while (true) { // power iteration; do up to maxIter iterations
			
			prevIterImportances = importances.clone();// deep copy
//			importances = dict.fromkeys(prevIterImportances.keys(),0); //XXX:probably not needed in java impl
			
			double dangleSum = 0;
			for (Integer dangleNdx : dangleIndices) {
				dangleSum += prevIterImportances.getAt(dangleNdx)*teleporations[dangleNdx];
			}
			dangleSum = dangleSum*scale; //normalize this
			
			for (int ndx = 0; ndx < numAccounts; ndx++) {
				
				LinkedList<AccountLink> inlinks = accounts.get(ndx).getInlinks();
				
				for (AccountLink inlink : inlinks) {
					
				}
				
				
				for (int nbr = 0; nbr < outlinks.size(); nbr++) { //W are edge weights in right-stochastic form, meaning they sum to 1 for each account's outlinks
					importances.setAt(nbr, importances.getAt(nbr) + teleporations[ndx]*prevIterImportances.getAt(ndx)*W[ndx][nbr][weight]);
				}
				    
				importances.setAt(ndx, importances.getAt(ndx) + dangleSum + (1.0-teleporations[ndx]));
			}

			// normalize vector
			importances.normalize();

			// check convergence using l1 norm
			double err = prevIterImportances.l1Distance(importances);
			
			if (err < tol) { //we've made it
				break;
			} else if (iterCount > maxIter) { //pwned
				//TODO: make convergenceerror class
//				raise ConvergenceError('poi: power iteration failed to converge in %d iterations.'%(i-1));
			}
			iterCount += 1;
		}
		    
		// normalize with outlinks degree and median outlinking trans amt, otherwise people will hoard NEM
		double[] outlinkWeights = new double[numAccounts];
		for (int ndx = 0; ndx < numAccounts; ndx++) {
			LinkedList<AccountLink> outlinks = accounts.get(ndx).getOutlinks();
//			currNodeOut = [i[2]['weight'] for i in G.edges(data=True) if i[0] == ndx]
			
			if (outlinks != null) {
				double medianOutlinkStrength = np.median(outlinks); //TODO: calc median of accountlink strength
				double outDegree = 0; //outDegree is the sum of strengths for outlinks
				for (AccountLink outlink : outlinks){
					outDegree += outlink.getStrength();
				}

				double outlinkWeight = medianOutlinkStrength*outDegree;
				outlinkWeights[ndx] = outlinkWeight;
			} else {
				outlinkWeights[ndx] = 0;
			}
		}
		    
		// normalize outlink weights
		double maxRank          = importances.getMax();
		double maxOutlinkWeight = ArrayUtils.max(outlinkWeights);
		double maxBalance = ArrayUtils.max(balances); //XXX:balances need to be in coindays
		
		// We are going to calculate all of this now so we can use this for testing.
		double[] pois = new double[numAccounts];
		double[] ows = new double[numAccounts];
		double[] normBalances = new double[numAccounts];
	    
		// normalize importances
		for (int ndx = 0; ndx < numAccounts; ndx++) {
			importances.setAt(ndx, importances.getAt(ndx)/maxRank);
			
			pois[ndx] = importances.getAt(ndx);
			ows[ndx] = outlinkWeights[ndx] / maxOutlinkWeight;
			
			importances.setAt(ndx, importances.getAt(ndx) + (outlinkWeights[ndx] / maxOutlinkWeight));
	        
			//weight by balance at the end
			//TODO: XXX: balances should be in coindays
			importances.setAt(ndx, importances.getAt(ndx)*balances[ndx] / maxBalance);
			normBalances[ndx] = accounts.get(ndx).getBalance().getNumMicroNem() / maxBalance;
		}   
		
		return importances.getVector();//, pois, ows, normBalances
	}

//	private  getStochasticGraph(G, weight='weight') {
//	    /*"""Return a right-stochastic representation of G.
//
//	    A right-stochastic graph is a weighted graph in which all of
//	    the node (out) neighbors edge weights sum to 1.
//	    
//	    Parameters
//	    -----------
//	    G : graph
//	      A NetworkX graph, must have valid edge weights
//
//	    weight : key (optional)
//	      Edge data key used for weight.  If None all weights are set to 1.
//	    """        */
//        W=nx.DiGraph(G);
//
//	    degree=W.out_degree(weight=weight);
//	    for (u,v,d) in W.edges(data=True) {
//	        d[weight]=d.get(weight,1.0)/degree[u];
//	    }
//	    return W
//	}
}
