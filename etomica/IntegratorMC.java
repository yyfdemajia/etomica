package etomica;

import java.util.Random;

public class IntegratorMC extends Integrator implements EtomicaElement {
    
    public String version() {return "IntegratorMC:01.02.10"+Integrator.VERSION;}
    
    private final Random rand = new Random();
    private MCMove firstMove, lastMove;
    private int frequencyTotal;
    private int moveCount;
    
//need to update to include makeIterators
    public IntegratorMC() {
        this(Simulation.instance);
    }
    public IntegratorMC(Simulation sim) {
        super(sim);
    }
    
    public static EtomicaInfo getEtomicaInfo() {
        EtomicaInfo info = new EtomicaInfo("General Monte Carlo simulation");
        return info;
    }
    
    /**
     * Sets moves in array to be integrator's set of moves, deleting any existing moves.
     */
    public void setMCMoves(MCMove[] moves) {
        firstMove = null;
        moveCount = 0;
        for(int i=0; i<moves.length; i++) {
            add(moves[i]);
        }
    }
    
    /**
     * Constructs and returns array of all moves added to the integrator.  
     */
    public MCMove[] getMCMoves() {
        MCMove[] moves = new MCMove[moveCount];
        int i=0;
        for(MCMove move=firstMove; move!=null; move=move.nextMove()) {moves[i++]=move;}
        return moves;
    }

    /**
     * Adds a basic MCMove to the set of moves performed by the integrator
     */
    public void add(MCMove move) {
        if(firstMove == null) {firstMove = move;}
        else {lastMove.setNextMove(move);}
        lastMove = move;
        move.setParentIntegrator(this);
        move.setPhase(phase);
        moveCount++;
    }
    
    /**
     * Invokes superclass method and informs all MCMoves about the new phase.
     */
    public boolean addPhase(Phase p) {
        if(!super.addPhase(p)) return false;
        for(MCMove move=firstMove; move!=null; move=move.nextMove()) {move.setPhase(phase);}
        return true;
    }
    
    /**
     * Method to select and perform an elementary Monte Carlo move.  
     * The type of move performed is chosen from all MCMoves that have been added to the
     * integrator.  Each MCMove has associated with it a (unnormalized) frequency, which
     * when weighed against the frequencies given the other MCMoves, determines
     * the likelihood that the move is selected.
     */
    public void doStep() {
        int i = (int)(rand.nextDouble()*frequencyTotal);
        MCMove trialMove = firstMove;
        while((i-=trialMove.getFrequency()) >= 0) {
            trialMove = trialMove.nextMove();
        }
        trialMove.doTrial();
    }
    
    
    protected void doReset() {
        frequencyTotal = 0;
        for(MCMove m=firstMove; m!=null; m=m.nextMove()) {
            m.resetFrequency();
            frequencyTotal += m.getFrequency();
        }
    }
    
    public Integrator.Agent makeAgent(Atom a) {
        return null;
    }
    
    public class Agent implements Integrator.Agent {
        public Atom atom;
        public Agent(Atom a) {atom = a;}
    }
}