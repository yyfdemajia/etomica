package etomica.normalmode;

import etomica.api.IData;
import etomica.data.DataTag;
import etomica.data.IEtomicaDataInfo;
import etomica.data.IEtomicaDataSource;
import etomica.data.meter.MeterPotentialEnergy;
import etomica.data.types.DataDouble;
import etomica.data.types.DataDouble.DataInfoDouble;
import etomica.potential.PotentialMaster;
import etomica.units.Null;

/**
 * Meter used for overlap sampling in the harmonic-sampled system.  The meter
 * measures the ratio of the Boltzmann factors for the harmonic and target
 * potentials.
 * 
 * @author Andrew Schultz & Tai Boon Tan
 */
public class MeterWorkHarmonicBennet implements IEtomicaDataSource {
    
    public MeterWorkHarmonicBennet(MCMoveHarmonic mcMoveHarmonic, PotentialMaster potentialMaster, double ref) {
        this.mcMoveHarmonic = mcMoveHarmonic;
        this.refPref = ref;
        meterEnergy = new MeterPotentialEnergy(potentialMaster);
        meterEnergy.setBox(mcMoveHarmonic.getBox());
        data = new DataDouble();
        numSum = 0;
        denomSum = 0;
        dataInfo = new DataInfoDouble("Scaled Harmonic and soft sphere Energies", Null.DIMENSION);

        tag = new DataTag();
    }

    public void setTemperature(double newTemperature) {
        temperature = newTemperature;
    }
    
    public double getTemperature() {
        return temperature;
    }
    
    public IData getData() {
    	double e0 = Math.exp( -mcMoveHarmonic.getLastTotalEnergy()/ temperature);
    	double e1 = Math.exp( -(meterEnergy.getDataAsScalar() - latticeEnergy)/ temperature);
    	
    	double ratio = e0/(1+refPref*(e0/e1));     //e0*e1/(e1+refPref*e0);
    	double overlapEnergy = -Math.log(ratio);
        data.x = overlapEnergy - mcMoveHarmonic.getLastTotalEnergy()/temperature;
      	//System.out.println("uHarmonic: "+ mcMoveHarmonic.getLastTotalEnergy()+" uTarget: "+meterEnergy.getDataAsScalar()
      	//					+" ,ratio: "+ratio+ " ,overlapEnergy: "+overlapEnergy);
      	
    	denomSum += ratio/e0;
    	numSum += data.x*(ratio/e0);
    	
        return data;
    }
    
    public Double getDataReweighted(){
    	
    	return numSum/denomSum;
    }
    
    public void setLatticeEnergy(double newLatticeEnergy) {
        latticeEnergy = newLatticeEnergy;
    }
    
    public IEtomicaDataInfo getDataInfo() {
        return dataInfo;
    }

    public DataTag getTag() {
        return tag;
    }
    
	public double getRefPref() {
		return refPref;
	}

	public void setRefPref(double refPref) {
		this.refPref = refPref;
	}

    protected double temperature;
    protected final MeterPotentialEnergy meterEnergy;
    protected final MCMoveHarmonic mcMoveHarmonic;
    protected final DataDouble data;
    protected final DataInfoDouble dataInfo;
    protected final DataTag tag;
    protected double latticeEnergy;
    protected double numSum, denomSum;
    public double refPref;

}
