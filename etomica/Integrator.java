package simulate;
import java.awt.event.*;
import java.util.*;
import simulate.units.*;

public abstract class Integrator implements Simulation.Element, Runnable, java.io.Serializable {

  transient public Thread runner = new Thread(this);
  public boolean running = false;
  private boolean haltRequested = false;
  private boolean resetRequested = false;
  protected int maxSteps = Integer.MAX_VALUE;
  protected int stepCount = 0;
  private String name;
 
  protected Phase firstPhase;
  protected Phase[] phase;
  int phaseCount = 0;
  int phaseCountMax = 1;
  protected int sleepPeriod = 10;
  private Vector intervalListeners = new Vector();
  int interval = 10;  // number of steps between IntervalEvent firing
  int integrationCount = 0;
  boolean doSleep = true;
  boolean paused = false;
  protected double temperature = Default.TEMPERATURE;
  protected boolean isothermal = false;
  private boolean initialized = false;
  
  IntervalEvent intervalEvent = new IntervalEvent();
  public Controller parentController;
  private final Simulation parentSimulation;
  private boolean added = false;

  public Integrator(Simulation sim) {
    parentSimulation = sim;
    phase = new Phase[phaseCountMax];
    parentSimulation.register(this);
  }
  
    public final Simulation parentSimulation() {return parentSimulation;}
    public final Class baseClass() {return Integrator.class;}
    public final boolean wasAdded() {return added;}
    public final void setAdded(boolean b) {added = b;}
    
    // abstract methods
    public abstract void doStep();
    protected abstract void doReset(); //protected because runs on integrator's thread
    public abstract Agent makeAgent(Atom a);
  
  public void initialize() {
    deployAgents();
    initialized = true;
    doReset();
  }
  public boolean isInitialized() {return initialized;}
  
  public void setController(Controller c) {parentController = c;}
  
  //how do agents get placed in atoms made during the simulation?
  protected void deployAgents() {  //puts an Agent of this integrator in each atom of all phases
    for(int i=0; i<phaseCount; i++) {
        Phase p = phase[i];
        for(Atom a=p.firstAtom(); a!=null; a=a.nextAtom()) {
            a.setIntegratorAgent(makeAgent(a));
        }
    }
  }
  
  public final void setTemperature(double t) {temperature = t;}
  public final double getTemperature() {return temperature;}
  public final double temperature() {return temperature;}
  public Dimension getTemperatureDimension() {return Dimension.TEMPERATURE;}

  //Other introspected properties
  public void setIsothermal(boolean b) {isothermal = b;}
  public boolean isIsothermal() {return isothermal;}
    
  public final int getInterval() {return interval;}
  public final void setInterval(int interval) {this.interval = interval;}
  
  public final int getSleepPeriod() {return sleepPeriod;}
  public final void setSleepPeriod(int s) {sleepPeriod = s;}

  public void setDoSleep(boolean b) {doSleep = b;}
  public boolean isDoSleep() {return doSleep;}

  /**
   * @return true if integrator can perform integration of another phase, 
   *         false if the integrator has all the phases it was built to handle
   */
  public boolean wantsPhase() {return phaseCount < phaseCountMax;}
  
  /**
   * Calls the setPhase method with the given phase.
   * @deprecated  instead use setIntegrator method in phase.
   */
  public void registerPhase(Phase p) {
    setPhase(p);
  }
  
  /**
   * @deprecated  instead use setIntegrator method in phase.
   */
  public void setPhase(Phase p) {
    addPhase(p);
  }
  
  /**
   * Performs activities needed to set up integrator to work on given phase.
   * This method should not be called directly; instead it is invoked by the phase in its setIntegrator method.
   * @return true if the phase was successfully added to the integrator; false otherwise
   */
   //perhaps should throw an exception rather than returning a boolean "false"
  public boolean addPhase(Phase p) {
    for(int i=0; i<phaseCount; i++) {if(phase[i]==p) return false;}  //check that phase is not already registered
    if(!this.wantsPhase()) {return false;}  //if another phase not wanted, return false
    phase[phaseCount] = p;
    phaseCount++;
    firstPhase = phase[0];
    makeIterators(p.iteratorFactory());
	p.iteratorFactoryMonitor.addObserver(iteratorFactoryObserver());
    return true;
  }
  
  /**
   * Performs activities needed to disconnect integrator from given phase.
   * This method should not be called directly; instead it is invoked by the phase in its setIntegrator method
   */
  public void removePhase(Phase p) {
    for(int i=0; i<phaseCount; i++) {
        if(phase[i]==p) {//phase found; remove it
            phase[i] = null;
            phaseCount--;
            if(phaseCount > 0) phase[i] = phase[phaseCount];
            firstPhase = phase[0];
            break;
        }
    }
  }

	/**
	 * Method performs no action, but can be overridden in subclasses to handle setting or change of iteratorFactory in phase
	 */
	 //this should be made abstract
	protected void makeIterators(IteratorFactory i) {}

	/**
	 * Returns an observer that can be registered with the phase's iteratorFactoryMonitor.
	 * In this way this integrator is informed if the iteratorFactory object changes to another 
	 * instance in the phase.
	 */
	protected Observer iteratorFactoryObserver() {
	    return new Observer() {
	        //This is the action that is to happen if phase takes a new iteratorFactory
	        public void update(Observable o, Object arg) {
	            makeIterators((IteratorFactory)arg);
	        }
	    };
	}
  public synchronized void addIntervalListener(IntervalListener iil) {
    intervalListeners.addElement(iil);
  }

  public synchronized void removeIntervalListener(IntervalListener iil) {
    intervalListeners.removeElement(iil);
  }

    //may want to rewrite this so not synchronized
  public void fireIntervalEvent(IntervalEvent iie) {
    Vector currentListeners = null;
    synchronized(this){
        currentListeners = (Vector)intervalListeners.clone();
    }
    for(int i = 0; i < currentListeners.size(); i++) {
        IntervalListener listener = (IntervalListener)currentListeners.elementAt(i);
        listener.intervalAction(iie);
    }
  }
  
    public int getMaxSteps() {return maxSteps;}
    public void setMaxSteps(int m) {maxSteps = m;}
    
    public void start() {
        haltRequested = false;
        this.initialize();
        runner = new Thread(this);
        runner.start();
    }

    public void run() {
        stepCount = 0;
        int iieCount = interval+1;
        while(stepCount < maxSteps) {
            while(paused) doWait();//keep this before resetRequest, since need for reset might naturally follow completion of pause
            if(resetRequested) {doReset(); resetRequested = false;}
            if(haltRequested) break;
            this.doStep();
            if(--iieCount == 0) {
                fireIntervalEvent(intervalEvent);
                iieCount = interval;
            }
            if(doSleep) {
                try { Thread.sleep(sleepPeriod); }
                catch (InterruptedException e) { }
            }
            stepCount++;
        }//end of while loop
    }//end of run method
    
    public void reset() {resetRequested = true;}
    
    protected synchronized void doWait() {
        try {
            wait();
        } catch(InterruptedException e) {}
    }
    
    //suspend and resume functions
    public void pause() {paused = true;}
    public synchronized void unPause() {paused = false; notify();}
    
    //stop function
    public void halt() {
        haltRequested = true;
        if(paused) unPause();
    }
    
    /**
     * Method to make the calling thread wait until this integrator thread has finished.
     */
    public void join() {
        try {
            runner.join();
        } catch(InterruptedException e) {}
    }
    
    /**
     * Accessor method of the name of this object
     * 
     * @return The given name
     */
    public final String getName() {return name;}

    /**
     * Method to set the name of this object
     * 
     * @param name The name string to be associated with this object
     */
    public final void setName(String name) {this.name = name;}

    /**
     * Overrides the Object class toString method to have it return the output of getName
     * 
     * @return The name given to the object
     */
    public String toString() {return getName();}  //override Object method
          
    
// Class generated by integrator as one of the properties of each atom

    interface Agent extends java.io.Serializable {
        /**
         * Integrator agent that holds a force vector.  Used to indicate that an atom could be
         * under the influence of a force.
         */
        interface Forcible {
            public Space.Vector force();
        }
    }
    
    public class IntervalEvent extends EventObject{
        
        public IntervalEvent() {
            super(Integrator.this);
        }
    }
    
    public interface IntervalListener extends java.util.EventListener {
        public void intervalAction(IntervalEvent evt);
    }

}

