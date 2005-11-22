package etomica.modules.chainequilibrium;

import javax.swing.JPanel;

import etomica.action.PhaseImposePbc;
import etomica.action.activity.ActivityIntegrate;
import etomica.action.activity.Controller;
import etomica.atom.Atom;
import etomica.atom.AtomAgentManager;
import etomica.config.ConfigurationSequential;
import etomica.data.meter.MeterTemperature;
import etomica.integrator.IntegratorHard;
import etomica.phase.Phase;
import etomica.simulation.Simulation;
import etomica.space2d.Space2D;
import etomica.space3d.Space3D;
import etomica.species.Species;
import etomica.species.SpeciesSpheresMono;

public class ReactionEquilibrium extends Simulation implements Atom.AgentSource {

	public MeterChainLength molecularCount;
	public Controller controller1;
	public JPanel panel = new JPanel(new java.awt.BorderLayout());
	public IntegratorHard integratorHard1;
	public java.awt.Component display;
	public Phase phase1;
	public etomica.action.SimulationRestart restartAction;
	public boolean initializing = true;
	public MeterTemperature thermometer;
	public SpeciesSpheresMono speciesA;
	public SpeciesSpheresMono speciesB;
	public P2SquareWellBonded AAbonded;
	public P2SquareWellBonded ABbonded;
	public P2SquareWellBonded BBbonded;
    public AtomAgentManager agentManager;
    public Atom[] agents;
	
    public ReactionEquilibrium() {
        super(Space2D.getInstance());
        controller1 = getController();

        double diameter = 1.0;
        
        molecularCount = new MeterChainLength(this);
		

        getDefaults().atomSize = diameter;

        integratorHard1 = new IntegratorHard(this);
        integratorHard1.setIsothermal(true);

        phase1 = new Phase(this);
        integratorHard1.setPhase(phase1);	
        speciesA = new SpeciesSpheresMono(this);
        speciesB = new SpeciesSpheresMono(this);
        speciesA.setDiameter(diameter);
        speciesA.setNMolecules(10);      
        speciesB.setNMolecules(40);      
        phase1.makeMolecules();
        new ConfigurationSequential(space).initializeCoordinates(phase1);

        molecularCount.setPhase(phase1);
    	
		//potentials
        AAbonded = new P2SquareWellBonded(space, this, 0.5 * getDefaults().atomSize, 
                2.0, getDefaults().potentialWell);
		ABbonded = new P2SquareWellBonded(space, this, 0.5 * getDefaults().atomSize,
		        2.0, getDefaults().potentialWell);
		BBbonded = new P2SquareWellBonded(space, this, 0.5 * getDefaults().atomSize,
		        2.0, getDefaults().potentialWell);

		potentialMaster.setSpecies(AAbonded,
		        new Species[] { speciesA, speciesA });
		potentialMaster.setSpecies(ABbonded,
		        new Species[] { speciesA, speciesB });
		
		potentialMaster.setSpecies(BBbonded,new Species[] { speciesB, speciesB });


		// **** Setting Up the thermometer Meter *****
		
		thermometer = new MeterTemperature();
		thermometer.setPhase(phase1);
        
		ActivityIntegrate activityIntegrate = new ActivityIntegrate(integratorHard1,true,true);
		activityIntegrate.setDoSleep(true);
		activityIntegrate.setSleepPeriod(1);
		getController().addAction(activityIntegrate);
		integratorHard1.addListener(new PhaseImposePbc(phase1));
	}
    
    public Atom[][] getAgents(Phase phase) {
        // the other classes don't know it, but there's only one phase.  :)
        if (agentManager == null) {
          agentManager = new AtomAgentManager(this,phase);
        }
        return (Atom[][])agentManager.getAgents();
    }

	/**
	 * Implementation of Atom.AgentSource interface, returning null. Agent in
	 * atom is used to hold bonding partner.
	 * 
	 * @param a
	 *            ignored
	 * @return Object always null
	 */
	public Object makeAgent(Atom a) {
		
		return new Atom[4];
	}
    
    public void releaseAgent(Object agent) {}
}
