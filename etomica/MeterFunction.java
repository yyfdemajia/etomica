package simulate;

import simulate.units.Dimension;
import simulate.utility.Histogram;

/**
 * Meter for recording and averaging an array of values of type double
 * Default is not to have meter do averages in response to integrationIntervalEvents.
 * Anticipated use of function meters is to measure some functional quantity (e.g. g(r)) when asked 
 * by a Display object.  To enable averages, call setActive(true)
 */
public abstract class MeterFunction extends MeterAbstract {
    
    /**
     * Abscissa values of the function
     */
    protected double[] x;
    /**
     * Ordinate values of the function
     */
    protected double[] y;
    /**
     * Simulation average of function
     */
    protected double[] average;
    /**
     * Confidence limits of averages
     */
    protected double[] error;
    /**
     * Number of function values tabulated
     */
    protected int nPoints = 0;
    /**
     * Minimum abscissa value
     */
    protected double xMin;
    /**
     * Maximum abscissa value
     */
    protected double xMax;
    
    protected double deltaX;
    /**
     * Optional label for the abscissa
     */
    protected String xLabel;
    /**
     * Array of accumulators used to keep averages for all ordinate values
     */
    protected MeterAbstract.Accumulator[] accumulator;
    
    /**
     * Default constructor.  Sets active false, xlabel to "x", 100 abscissa values from 0 to 1.0
     */
    public MeterFunction(Simulation sim) {
        super(sim);
        setX(0.0, 1.0, 100);
        setXLabel("x");
	    setActive(false);  
    }
    
    /**
     * Returns the dimensions of the abscissa.
     * Note that the ordinate dimensions are given by the getDimension method inherited from MeterAbstract.
     */
    public abstract Dimension getXDimension();
    
	/**
	 * This is the method defined by the subclass to specify what function property is measured by the meter
	 * The method is called by the updateSums method  to add the current value to the accumulator.
	 * This method may be called directly by any object to obtain the current value of the function
	 * property.  A call to currentValue does nothing
	 * to the sums kept for averages.  To obtain the current value and increment the sums at the same time,
	 * call updateSums() and then mostRecent().
	 */
    public abstract double[] currentValue();  //in subclasses usually will update and return the y array
    
    /**
     * Method to update running sums for averages and error statistics
     * Called by integrationIntervalAction every updateInterval times an integration event is received
     */
	public void updateSums() {
	    double[] values = currentValue();
	    for(int i=0; i<nPoints; i++) {
	        accumulator[i].add(values[i]);
	    }
	}
		
    /**
     * Returns the current value of the averages
     */
	public double[] average() {
	    for(int i=0; i<nPoints; i++) {average[i] = accumulator[i].average();}
	    return average;
	}
	
	/**
	 * Returns the current value of the error bars (67% confidence limit)
	 */
	public double[] error() {
	    for(int i=0; i<nPoints; i++) {error[i] = accumulator[i].error();}
	    return error;
	}
	/**
	 * Returns the value passed most recently obtained via an integrator intervalEvent
	 * (does not give the value last returned by any direct call to currentValue
	 *  i.e. by a call to currentValue that was not made in response to an interval event)
	 */
	public double[] mostRecent() {
	    for(int i=0; i<nPoints; i++) {y[i] = accumulator[i].mostRecent();}
	    return y;
	}
	/**
	* Accessor method to indicate if the meter should keep a histogram of all measured values.
	* Default is false (do not keep histogram).
	*/
	public boolean isHistogramming() {
	    if(accumulator[0] != null) return accumulator[0].isHistogramming();
	    else return false;
	}
    	 
	/**
	* Accessor method to indicate if the meter should keep a histogram of all measured values.
	* Default is false (do not keep histogram).
	*/
	public void setHistogramming(boolean b) {
	    for(int i=0; i<nPoints; i++) {accumulator[i].setHistogramming(b);}
	}
	/**
	 * Returns the current histograms of measured values.
	 * Histograms are recorded only if histogramming is set to true.  
	 * If histogram was never kept for this meter, all-zero histograms are returned.
	 */
	 public Histogram[] histogram() {
	    Histogram[] hist = new Histogram[nPoints];
	    for(int i=0; i<nPoints; i++) {hist[i] = accumulator[i].histogram();}
	    return hist;
	 }
	 
	/**
	* Accessor method to indicate if the meter should keep a histogram of all measured values.
	* Default is false (do not keep histogram).
	*/
	public boolean isHistogramming(int i) {
	    if(i < accumulator.length && accumulator[i] != null) return accumulator[i].isHistogramming();
	    else return false;
	}
    	 
	/**
	* Accessor method to indicate if the meter should keep a histogram of all measured values.
	* Default is false (do not keep histogram).
	*/
	public void setHistogramming(int i, boolean b) {
	    if(i < accumulator.length && accumulator[i] != null) accumulator[i].setHistogramming(b);
	}
	
	/**
	 * Returns the current histogram of the given measured value.
	 * Histograms are recorded only if histogramming is set to true.  
	 * If histogram was never kept for this meter, all-zero histograms are returned.
	 */
	 public Histogram histogram(int i) {
	    if(i < accumulator.length && accumulator[i] != null) return accumulator[i].histogram();
	    else return null;
	 }
	 
	/**
	 * Zeros all sums used for averages
	 */
	public void reset() {
	    if (!isActive()) {return;}
	    for(int i=0; i<nPoints; i++) {accumulator[i].reset();}
	}

    /**
     * The abscissa values corresponding to the array of measured Y values
     */
    public double[] X() {return x;}
    
    /**
     * Accessor method for the number of points tabulated in the function
     */
    public int getNPoints() {return nPoints;}
    /**
     * Sets the number of points tabulated in the function and recalculates the abscissa values
     */
    public void setNPoints(int n) {setX(xMin, xMax, n);}
    /**
     * Sets the minimum value of the abscissa and recalculates all abscissa values
     */
    public void setXMin(double xm) {setX(xm,xMax,nPoints);}
    /**
     * Sets the maximum value of the abscissa and recalculates all abscissa values
     */
    public void setXMax(double xm) {setX(xMin,xm,nPoints);}
    /**
     * Accessor method for the minimum abscissa value
     */
    public double getXMin() {return xMin;}
    /**
     * Accessor method for the maximum abscissa value
     */
    public double getXMax() {return xMax;}
    
    public Dimension getXMinDimension() {return getXDimension();}
    public Dimension getXMaxDimension() {return getXDimension();}
    
    public int getXIndex(double x) {
       int index = (int)((x - xMin)/deltaX);
       if (index >= nPoints) {return nPoints - 1;}
       else if (index < 0) {return 0;}
       else {return index;}
    }
    
    /**
     * Sets the label for the function
     *
     * @see #label
     */
    public void setXLabel(String s) {xLabel = s;}
    /**
     * Accessor method for label
     * 
     * @see #label
     */
    public String getXLabel() {return xLabel;}
    
    /**
     * Overrides super.setActive to ensure that abscissa values are computed and all accumulators are constructed when the meter is active
     *
     * @see MeterAbstract#setActive
     */
    public void setActive(boolean b) {
        super.setActive(b);
        if(isActive()) resizeArrays();
        
    }
    
    /**
     * Computes the X (abscissa) vector by simple linear scaling of n values between min and max
     * If n is changed from present value, new accumulators are made for recording averages
     * First and last values of x array are a half-interval distant from given values of min and max
     *   For future development: give more flexibility in setting whether min and max are inclusive or exclusive
     */
    public void setX(double min, double max, int n) {
        if(n != nPoints) {
            nPoints = n;
            resizeArrays(); //this calls setX to finish the method, so return immediately
            return;
        }
        xMin = min;
        xMax = max;
        deltaX = (xMax - xMin)/(double)(nPoints);
        for(int i=0; i<nPoints; i++) {x[i] = xMin + (i+0.5)*deltaX;}
        
    }
    
    protected void resizeArrays() {
        int n = nPoints;
        x = new double[n];
        y = new double[n];
//        if(isActive()) {
            average = new double[n];
            error = new double[n];
            accumulator = new MeterAbstract.Accumulator[n];
            for(int i=0; i<n; i++) {
                accumulator[i] = new MeterAbstract.Accumulator();
            }
 //       }
        setX(xMin, xMax, nPoints);
    }
    
	/**
	 * Interface to indicate an object that interacts with a MeterFunction.
	 */
	 public interface User {
	    public void setMeterFunction(MeterFunction m);
	    public MeterFunction getMeterFunction();
	 }
	 
	/**
	 * Interface to indicate an object that interacts with multiple MeterFunctions.
	 */
	 public interface MultiUser {
	    public void setMeterFunctions(MeterFunction[] m);
	    public MeterFunction[] getMeterFunctions();
	    public void addMeterFunction(MeterFunction m);
	 }
	 
    
}