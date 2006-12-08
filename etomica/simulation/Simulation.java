package etomica.simulation;

import java.util.LinkedList;

import etomica.EtomicaInfo;
import etomica.action.activity.Controller;
import etomica.atom.AtomTreeNodeGroup;
import etomica.atom.SpeciesRoot;
import etomica.atom.iterator.AtomIteratorArrayListSimple;
import etomica.data.DataSource;
import etomica.integrator.Integrator;
import etomica.phase.Phase;
import etomica.potential.PotentialMaster;
import etomica.space.Space;
import etomica.space2d.Space2D;
import etomica.species.Species;
import etomica.util.Arrays;
import etomica.util.Default;
import etomica.util.NameMaker;

/**
 * The main class that organizes the elements of a molecular simulation.
 * Holds a single Space instance that is referenced in
 * many places to obtain spatial elements such as vectors and boundaries.
 */

public class Simulation extends EtomicaInfo implements java.io.Serializable  {
    
    
    
    /**
     * Constructs a default 2D, dynamic simulation.
     */
    public Simulation() {
        this(Space2D.getInstance());
    }
    
    /**
     * Creates a new simulation using the given space, with a default
     * setting of isDynamic = true.
     */
    public Simulation(Space space) {
        this(space, true, new PotentialMaster(space));
    }
    
    public Simulation(Space space, boolean isDynamic, PotentialMaster potentialMaster) {
        this(space, isDynamic, potentialMaster, Default.BIT_LENGTH, new Default());
    }
    
    public Simulation(Space space, boolean isDynamic, PotentialMaster potentialMaster, int[] bitLength, Default defaults) {
        this.space = space;
        this.dynamic = isDynamic;
        this.defaults = defaults;
        setName(NameMaker.makeName(this.getClass()));
        this.potentialMaster = potentialMaster;
        setController(new Controller());
        speciesRoot = new SpeciesRoot((int[])bitLength.clone());
        potentialMaster.setSimulation(this);
    }
                 
    public final Space space() {return space;}

    /**
     * Returns an array of Phases contained in the Simulation
     */
    public final Phase[] getPhases() {
        int nPhases = ((AtomTreeNodeGroup)speciesRoot.getNode()).getChildList().size();
        Phase[] phases = new Phase[nPhases];
        AtomIteratorArrayListSimple listIterator = new AtomIteratorArrayListSimple(((AtomTreeNodeGroup)speciesRoot.getNode()).getChildList());
        listIterator.reset();
        int i=0;
        while(listIterator.hasNext()) {
            phases[i++] = listIterator.nextAtom().getNode().parentPhase();
        }
        return phases;
    }
    
    /**
     * Returns a list of Integrators that have been registered with the Simulation
     */
    public final LinkedList getIntegratorList() {return integratorList;}
    
    /**
     * Returns an array of Species contained in the Simulation
     */
    public final Species[] getSpecies() {
        return speciesRoot.getSpecies();
    }
    
    /**
     * Returns an array of DataStreamsHeaders for data streams that have been
     * registered with the simulation.
     */
    public DataStreamHeader[] getDataStreams() {
        return (DataStreamHeader[])dataStreams.clone();
    }
    
    /**
     * Clears the list of data streams that have been registered with the 
     * Simulation.
     */
    public void clearDataStreams() {
        dataStreams = new DataStreamHeader[0];
    }
    
    /**
     * Clears the list of Integrators that have been registered with the 
     * Simulation.
     */
    public void clearIntegrators() {
        integratorList.clear();
    }
    
    /**
     * Add the given Integrator to a list kept by the simulation.  No other 
     * effect results from registering the Integrator. The list of registered 
     * Integrators may be retrieved via the getIntegrators method.  An 
     * individual Integrator may be removed from the list via the unregister 
     * method.
     */
    public void register(Integrator integrator) {
        integratorList.add(integrator);
    }

    /**
     * Add the given DataSource and client (the object which calls the 
     * DataSource's getData method) to a list of data streams kept by the 
     * simulation.  No other effect results from registering the data stream. 
     * The list of registered data streams may be retrieved via the 
     * getDataStreams method.  A single data stream may be removed from the 
     * list via the unregister method.
     */
    public void register(DataSource dataSource, Object client) {
        for (int i=0; i<dataStreams.length; i++) {
            if (dataStreams[i].getDataSource() == dataSource && dataStreams[i].getClient() == client) {
                return;
            }
        }
        dataStreams = (DataStreamHeader[])Arrays.addObject(dataStreams,new DataStreamHeader(dataSource,client));
    }
    
    /**
     * Removes the given Integrator from the list of Integrators kept by the 
     * simulation.  No other action results upon removing it from this list.  
     * If the given Integrator is not in the list already, the method returns 
     * without taking any action.
     */
    public void unregister(Integrator integrator) {
        integratorList.remove(integrator);
    }
    
    /**
     * Removes the given data stream that represents the given DataSource and 
     * client from the list of data streams kept by the simulation.  No other 
     * action results upon removing it from this list.  If the given data 
     * stream has not been registered, the method returns without taking any 
     * action.
     */
    public void unregister(DataSource dataSource, Object client) {
        for (int i=0; i<dataStreams.length; i++) {
            if (dataStreams[i].getDataSource() == dataSource && dataStreams[i].getClient() == client) {
                dataStreams = (DataStreamHeader[])Arrays.removeObject(dataStreams,dataStreams[i]);
                return;
            }
        }
    }
    
    /**
     * Returns the Controller used to run the simulation's Actions and 
     * Activities.
     */
	public Controller getController() {
		return controller;
	}
	
	//TODO transfer control from old to new controller (copy over integrators, etc)
    //AJS really?
	public void setController(Controller controller) {
		this.controller = controller;
	}
    
    /**
     * Accessor method of the name of this simulation.
     * 
     * @return The given name of this phase
     */
    public final String getName() {return name;}

    /**
     * Method to set the name of this simulation. The simulation's name
     * provides a convenient way to label output data that is associated with
     * it.  This method might be used, for example, to place a heading on a
     * column of data. Default name is the base class followed by the integer
     * index of this simulation.
     * 
     * @param name The name string to be associated with this element
     */
    public void setName(String name) {this.name = name;}

    /**
     * Overrides the Object class toString method to have it return the output of getName
     * 
     * @return The name given to the phase
     */
    public String toString() {return getName();}
    
    /**
     * @return Returns a flag indicating whether the simulation involves molecular dynamics.
     */
    public boolean isDynamic() {
        return dynamic;
    }

    /**
     * @return Returns the defaults.
     */
    public Default getDefaults() {
        return defaults;
    }
    /**
     * @param defaults The defaults to set.
     */
    public void setDefaults(Default defaults) {
        this.defaults = defaults;
    }
    
    /**
     * @return the potentialMaster
     */
    public final PotentialMaster getPotentialMaster() {
        return potentialMaster;
    }

    /**
     * @return the space
     */
    public final Space getSpace() {
        return space;
    }

    /**
     * @return the speciesRoot
     */
    public final SpeciesRoot getSpeciesRoot() {
        return speciesRoot;
    }

    public static final java.util.Random random = new java.util.Random();
//    public static final java.util.Random random = new java.util.Random(1);
        
    /**
     * Integer array indicating the maximum number of atoms at each depth in the
     * atom hierarchy.  Maximum depth is given by the size of the array.  Each
     * element of array is log2 of the maximum number of child atoms permitted
     * under one atom.  This is used to assign index values to each atom when it
     * is made.  The indexes permit quick comparison of the relative ordering
     * and/or hierarchical relation of any two atoms.  Sum of digits in array
     * should not exceed 31. For example, {5, 16, 7, 3} indicates 31
     * speciesAgents maximum, 65,536 molecules each, 128 groups per molecule, 8
     * atoms per group (number of species agents is one fewer, because index 0
     * is assigned to species master)
     */
    // powers of 2, for reference:
    //  n | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 | 11 | 12 | 13 |  14  |  15  |  16  |  17   |  18   |  19   |   20    |
    // 2^n| 2 | 4 | 8 | 16| 32| 64|128|256|512|1024|2048|4096|8192|16,384|32,768|65,536|131,072|262,144|524,288|1,048,576|
    // {speciesRoot, phases, species, molecules, groups, atoms}
    public int[] DEFAULT_BIT_LENGTH = new int[] {1, 4, 4, 14, 6, 3};

    private static final long serialVersionUID = 1L;
    protected final PotentialMaster potentialMaster;
    protected final Space space;
    protected final SpeciesRoot speciesRoot;
    private final boolean dynamic;
    private Controller controller;     
    private final LinkedList integratorList = new LinkedList();
    private DataStreamHeader[] dataStreams = new DataStreamHeader[0];
    private String name;
    protected Default defaults;
}
