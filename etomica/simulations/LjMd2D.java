package etomica.simulations;
import etomica.*;
import etomica.graphics.*;

/**
 * Simple Lennard-Jones molecular dynamics simulation in 2D
 */
 
public class LjMd2D extends SimulationGraphic {
    
    public IntegratorVelocityVerlet integrator;
    public SpeciesSpheresMono species;
    public Phase phase;
    public P2LennardJones potential;
    public Controller controller;
    public DisplayPhase display;
    public DisplayPlot plot;
    public MeterEnergy energy;

    public LjMd2D() {
        super(new Space2D());
        Simulation.instance = this;
	    integrator = new IntegratorVelocityVerlet(this);
	    species = new SpeciesSpheresMono(this);
	    phase = new Phase(this);
	    potential = new P2LennardJones();
	    controller = new Controller(this);
	    display = new DisplayPhase(this);
	    DisplayTimer timer = new DisplayTimer(integrator);
	    timer.setUpdateInterval(10);
		panel().setBackground(java.awt.Color.yellow);
		
		energy = new MeterEnergy(this);
		energy.setHistorying(true);
		energy.setActive(true);
		
		energy.getHistory().setNValues(500);
		
		plot = new DisplayPlot(this);
		plot.setLabel("Energy");
		plot.setDataSource(energy.getHistory());
		
		integrator.setSleepPeriod(2);
		
    }
    
    /**
     * Demonstrates how this class is implemented.
     */
    public static void main(String[] args) {
        SimulationGraphic sim = new LjMd2D();
		sim.elementCoordinator.go(); 
		sim.makeAndDisplayFrame();
    }//end of main
    
}