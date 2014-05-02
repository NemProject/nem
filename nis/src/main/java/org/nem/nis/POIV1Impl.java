package org.nem.nis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.math3.stat.descriptive.rank.Median;
import org.nem.core.math.ColumnVector;
import org.nem.core.model.Account;
import org.nem.core.model.AccountLink;
import org.nem.core.utils.ArrayUtils;

/**
 * This is a first draft implementation of the POI importance calculation.
 * 
 * Because a lot of the infrastructure is not yet in place, I am making the
 * following assumptions in this code:
 * 
 * 1) This class's calculateImportancesImpl is called with a list of all the accounts.
 * 2) POI is calculated by the forager after processing new transactions. 
 *    This algorithm is not currently iterative, so importances are calculated from scratch every time. 
 *    I plan to make this iterative so that we update importances only for accounts affected by new transactions and their links.
 * 
 */
public class POIV1Impl implements POI {

	public static final int DEFAULT_MAX_ITERS = 100;
	
	public static final double EPSILON = .00000001;

	public static final double WEIGHT = .99;
	
	public static final double MIN_TELEPORTATION_PROB = .7;
	
	public static final double ADDITIVE_TELEPORTATION = .25;
	
	public static final double MAX_PROB = 1.0;
	
	public static final double DEFAULT_POWER_ITERATION_TOL = 1.0e-8;

	public ColumnVector getAccountImportances(List<Account> accounts) {
		return calculateImportancesImpl(accounts, DEFAULT_MAX_ITERS, DEFAULT_POWER_ITERATION_TOL);
	}

	// This is the draft implementation for calculating proof-of-importance
	private ColumnVector calculateImportancesImpl(List<Account> accounts, int maxIters, double tol) {
		
		int numAccounts = accounts.size();
		double scale = MAX_PROB /numAccounts;
		
		//XXX: okay, it sucks that we have to do this, but let's just do this for now;
		//eventually we should try to create a better structure for the graph
		HashMap<String, Integer> acctMap = new HashMap<String, Integer>();
		for (int acctNDX = 0; acctNDX < numAccounts; acctNDX++) {
			acctMap.put(accounts.get(acctNDX).getAddress().toString(), acctNDX);
		}
		
		ColumnVector importances = new ColumnVector(numAccounts);
		
		// start each node's importance as its balance
		// also go through and find dangling accounts (with 0 outDegree)
		// "dangling" nodes, no links out from them; maybe we can skip these later; we should look into that
		long[] coindayBalances = new long[numAccounts];
		ArrayList<Integer> dangleIndices = new ArrayList<Integer>();
		for (int ndx = 0; ndx < numAccounts; ndx++) {
			Account currAcct = accounts.get(ndx);
			
			coindayBalances[ndx] = currAcct.getCoinDayWeightedBalance().getNumMicroNem();
			
			importances.setAt(ndx, currAcct.getBalance().getNumMicroNem()); //XXX:can we do this or will there be precision errors?
			List<AccountLink> outlinks = currAcct.getOutlinks();
			if (outlinks == null || outlinks.size() < 1) { //then we have a dangling account
				dangleIndices.add(ndx);
			}
		}
		
		//Prepare outlink weights
		double[][] outlinkWeights = new double[numAccounts][];
		for (int ndx = 0; ndx < numAccounts; ndx++) {
			
			List<AccountLink> outlinks = accounts.get(ndx).getOutlinks();
			if (outlinks == null || outlinks.size() < 1) {
				continue;
			}
			
			double[] weights = getRightStochasticWeights(outlinks);
			outlinkWeights[ndx] = weights;
		}

		// normalize starting vector to sum to 1
		importances.normalize();
		
		double maxImportance = importances.max();

		// calculate teleportation probabilities based on normalized amount of NEM owned
		double[] teleporations = new double[numAccounts];
		for (int acctNdx = 0; acctNdx < importances.getSize(); acctNdx++) {
	        // Assign a value between .7 and .95 based on the amount of NEM in an account.
			// It seems that more NEM = higher teleportation seems to work better.
			// NOTE: importances were initialized with acct balances, so this is why this works;
			// also, the importance vector was already normalized to sum to 1 in the code above.
			teleporations[acctNdx] = MIN_TELEPORTATION_PROB 
					+ ADDITIVE_TELEPORTATION*(importances.getAt(acctNdx)/maxImportance);
		}
		
		int iterCount = 0;
		ColumnVector prevIterImportances;
		while (true) { // power iteration; do up to maxIter iterations
			
			prevIterImportances = importances; //no need to do a deep copy because we create a new ColumnVector in the next line
			importances = new ColumnVector(numAccounts);
			
			double dangleSum = 0;
			for (Integer dangleNdx : dangleIndices) {
				dangleSum += prevIterImportances.getAt(dangleNdx)*teleporations[dangleNdx];
			}
			dangleSum *= scale; //normalize this
			
			for (int ndx = 0; ndx < numAccounts; ndx++) {
				
				List<AccountLink> outlinks = accounts.get(ndx).getOutlinks();
				if (outlinks == null || outlinks.size() < 1) {
					continue;
				}
				
				double[] weights = outlinkWeights[ndx];
				
				//distribute importance to outlinking accounts
				for (AccountLink outlink : outlinks) {
					int otherAcctNdx = acctMap.get(outlink.getOtherAccount().toString());
					//not the prettiest code ever
					importances.setAt(otherAcctNdx, 
									  importances.getAt(otherAcctNdx) + 
									  teleporations[ndx]*prevIterImportances.getAt(ndx)*weights[otherAcctNdx]);
				}
				
				importances.setAt(ndx, importances.getAt(ndx) + dangleSum + (MAX_PROB-teleporations[ndx]));
			}

			// normalize vector
			importances.normalize();

			// check convergence using l1 norm
			double err = prevIterImportances.l1Distance(importances);
			
			if (err < tol) { // we've made it
				break;
			} else if (iterCount > maxIters) { // pwned
				throw new IllegalStateException(
						"POI: power iteration failed to converge in " + maxIters
								+ " iterations");
			}
			iterCount += 1;
		}
		    
		// normalize with outlinks degree and median outlinking trans amt, otherwise people will hoard NEM
		double[] outlinkScores = new double[numAccounts];
		for (int ndx = 0; ndx < numAccounts; ndx++) {
			List<AccountLink> outlinks = accounts.get(ndx).getOutlinks();
			
			if (outlinks != null) {
				Median median = new Median();
				
				double medianOutlinkStrength = median.evaluate(outlinkWeights[ndx]);
				double outDegree = 0; //outDegree is the sum of strengths for outlinks, not the graphical degree
				for (AccountLink outlink : outlinks) {
					outDegree += outlink.getStrength();
				}

				double outlinkScore = medianOutlinkStrength*outDegree;
				outlinkScores[ndx] = outlinkScore;
			} else {
				outlinkScores[ndx] = 0;
			}
		}
		    
		// normalize outlink weights
		double maxRank          = importances.max();
		double maxOutlinkScore  = ArrayUtils.max(outlinkScores);
		long maxBalance         = ArrayUtils.max(coindayBalances);
		
		// We are going to calculate all of this now so we can use these for testing.
		//TODO: take out unused arrays later
		double[] pois         = new double[numAccounts];
		double[] ows          = new double[numAccounts];
		double[] normBalances = new double[numAccounts];
	    
		// normalize importances
		for (int ndx = 0; ndx < numAccounts; ndx++) {
			importances.setAt(ndx, importances.getAt(ndx)/maxRank);
			
			pois[ndx] = importances.getAt(ndx);
			ows[ndx]  = outlinkScores[ndx] / maxOutlinkScore;
			
			importances.setAt(ndx, importances.getAt(ndx) + (outlinkScores[ndx] / maxOutlinkScore));
	        
			//weight by balance at the end
			importances.setAt(ndx, importances.getAt(ndx)*coindayBalances[ndx] / maxBalance);
			normBalances[ndx] = accounts.get(ndx).getBalance().getNumMicroNem() / maxBalance;
		}   
		
		return importances;//, pois, ows, normBalances
	}

	/**
	 * Right-stochastic form means that all of the weights in <code>edges</code> sum to 1.
	 * 
	 * @param edges - list of <code>AccountLink</code>s, defining edges linking accounts
	 * @return array of weights in right-stochastic form
	 */
	private double[] getRightStochasticWeights(List<AccountLink> edges) {

		if (edges == null || edges.size() < 1) {
			return null;
		}
		
		int numEdges = edges.size();
		double[] weights = new double[numEdges];
		
		double sumOutWeights = 0;
	    for (int edgeNDX = 0; edgeNDX < numEdges; edgeNDX++) {
	    	double weight = edges.get(edgeNDX).getStrength();
	    	sumOutWeights += weight;
	    	weights[edgeNDX] = weight;
	    }
	    
	    //now normalize to sum to 1
	    for (int ndx = 0; ndx < weights.length; ndx++){
	    	weights[ndx] /= sumOutWeights;
	    }
	    
	    return weights;
	}
}
