package etomica.data.meter;
import etomica.api.IBox;
import etomica.api.IPotentialMaster;
import etomica.atom.iterator.IteratorDirective;
import etomica.data.DataSourceScalar;
import etomica.integrator.IntegratorBox;
import etomica.potential.PotentialCalculationVirialSum;
import etomica.space.ISpace;
import etomica.units.Pressure;

/**
 * Meter for evaluation of the soft-potential pressure in a box.
 * Requires that temperature be set in order to calculation ideal-gas
 * contribution to pressure; default is to use zero temperature, which
 * causes this contribution to be omitted.
 *
 * @author David Kofke
 */
 
public class MeterPressure extends DataSourceScalar {
    
    public MeterPressure(ISpace space) {
    	super("Pressure",Pressure.dimension(space.D()));
    	dim = space.D();
        iteratorDirective = new IteratorDirective();
        iteratorDirective.includeLrc = true;
        virial = new PotentialCalculationVirialSum();
    }

    /**
     * Sets the integrator associated with this instance.  The pressure is 
     * calculated for the box the integrator acts on and integrator's 
     * temperature is used for the ideal gas contribution.  Alternatively, you
     * can set the potentialMaster, temperature and box separately.
     */
    public void setIntegrator(IntegratorBox newIntegrator) {
        integrator = newIntegrator;
    }

    public void setPotentialMaster(IPotentialMaster newPotentialMaster) {
        potentialMaster = newPotentialMaster;
    }
    
    public void setTemperature(double newTemperature) {
        temperature = newTemperature;
    }

    public void setBox(IBox newBox) {
        box = newBox;
    }

    /**
     * Returns the integrator associated with this instance.  The pressure is 
     * calculated for the box the integrator acts on and integrator's 
     * temperature is used for the ideal gas contribution.
     */
    public IntegratorBox getIntegrator() {
        return integrator;
    }

    /**
     * Sets flag indicating whether calculated energy should include
     * long-range correction for potential truncation (true) or not (false).
     */
    public void setIncludeLrc(boolean b) {
    	iteratorDirective.includeLrc = b;
    }
    
    /**
     * Indicates whether calculated energy should include
     * long-range correction for potential truncation (true) or not (false).
     */
    public boolean isIncludeLrc() {
    	return iteratorDirective.includeLrc;
    }

	 /**
	  * Computes total pressure in box by summing virial over all pairs, and adding
	  * ideal-gas contribution.
	  */
    public double getDataAsScalar() {
        if (integrator == null && (potentialMaster == null || box == null)) {
            throw new IllegalStateException("You must call setIntegrator before using this class");
        }
    	virial.zeroSum();
    	IBox b = box;
    	if (b == null) {
    	    b = integrator.getBox();
    	}
        if (potentialMaster != null) {
            potentialMaster.calculate(b, iteratorDirective, virial);
        }
        else {
            integrator.getPotentialMaster().calculate(b, iteratorDirective, virial);
        }
        double temp = (integrator != null) ? integrator.getTemperature() : temperature;
        //System.out.println("fac="+(1/(box.getBoundary().volume()*box.getSpace().D())));
        return (b.getMoleculeList().getMoleculeCount() / b.getBoundary().volume())*temp - virial.getSum()/(b.getBoundary().volume()*dim);
    }

    private IntegratorBox integrator;
    private IteratorDirective iteratorDirective;
    private final PotentialCalculationVirialSum virial;
    protected IPotentialMaster potentialMaster;
    protected double temperature;
    protected IBox box;
    private final int dim;
}
