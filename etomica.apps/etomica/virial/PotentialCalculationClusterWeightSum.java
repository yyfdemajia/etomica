package etomica.virial;

import etomica.api.IAtomSet;
import etomica.api.IPotential;
import etomica.potential.PotentialCalculation;

/**
 * Calculates the cluster weight associated with current configuration.
 */

public class PotentialCalculationClusterWeightSum implements PotentialCalculation {
        
    public void reset() {
        weight = 1.0;
    }
    public double weight() {
        return weight;
    }

    public void doCalculation(IAtomSet atoms, IPotential potential) {
        // we'll also get intramolecular potentials... ignore them
        if (potential instanceof P0Cluster) {
            weight *= ((P0Cluster)potential).weight();
        }
	}

    protected double weight;
}
