package etomica.normalmode;

import etomica.phase.Phase;
import etomica.space.Vector;

/**
 * Interface for a class the returns the appropriate wave vectors for a phase
 * and primitive
 *
 * @author Andrew Schultz
 */
public interface WaveVectorFactory {

    /**
     * Tells the instance to make wave vectors appropraite for the given phase. 
     * The wave vectors will not include the 0 vector, any two  vectors which 
     * are opposites or any wave vector that is redundant with another.
     */
    public void makeWaveVectors(Phase phase);
    
    /**
     * Returns the wave vectors generated by makeWaveVectors
     */
    public Vector[] getWaveVectors();

    /**
     * Returns the coefficients for the wave vectors returned by 
     * getWaveVectors.  If two of the wave vectors are redundant, their 
     * coefficient will be 0.5.  Others will have a coefficient of 1.0.
     */
    public double[] getCoefficients();

}