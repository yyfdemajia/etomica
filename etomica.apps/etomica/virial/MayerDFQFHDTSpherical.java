package etomica.virial;

import etomica.api.IBox;
import etomica.api.IMoleculeList;
import etomica.api.IPotential;
import etomica.potential.P2EffectiveFeynmanHibbs;
import etomica.potential.Potential2Spherical;
/**
 * Required for computing temperature derivatives of virial coefficients computed with quadratic Feynmann-Hibbs modification 
 * to the potential.
 * @author kate
 */
public class MayerDFQFHDTSpherical implements MayerFunction {

	/**
	 * Constructor for MayerESpherical.
	 */
	public MayerDFQFHDTSpherical(P2EffectiveFeynmanHibbs potential) {
		this.potential = potential;
	
	}

	/**
	 * @see etomica.virial.MayerFunctionSpherical#f(etomica.AtomPair, double, double)
	 */
	public double f(IMoleculeList pair, double r2, double beta) {
		double u = potential.u(r2);
		if (Double.isInfinite(u)) {
			return 0;
		}
		
		double dudkT = potential.dudkT(r2);
		double dfdkT = Math.exp(-beta*u)*(u*beta*beta -beta*dudkT);
		
		if (Double.isNaN(dfdkT)) {
			throw new RuntimeException ("dfdT is NaN");
		}
		if (Double.isInfinite(dfdkT)) {
			throw new RuntimeException ("dfdT is Infinite");
		}
		return dfdkT;
	}
	
	public void setBox(IBox newBox) {
	    potential.setBox(newBox);
	}

	private final P2EffectiveFeynmanHibbs potential;

	public IPotential getPotential() {
		return potential;
	}

}
