package etomica;
import java.util.EventObject;

/**
 * Event that conveys some activity with respect to a phase or the things it contains.
 *
 * @see PhaseListener
 * @see DisplayPhaseListener
 */
public class PhaseEvent extends java.util.EventObject {
    
    protected Phase phase;
    protected Atom atom;
    protected Space.Vector point;
    
    public PhaseEvent(Object source) {
        super(source);
    }
    public static int POINT_SELECTED = 0;
    public static int ATOM_SELECTED = 10;
    public static int ATOM_RELEASED = 11;
    
    public final void setPhase(Phase p) {phase = p;}
    public final Phase getPhase() {return phase;}
    
    public void setPoint(Space.Vector p) {point = p;}
    public Space.Vector getPoint() {return point;}
    
    public void setAtom(Atom a) {atom = a;}
    public Atom getAtom() {return atom;}
}
    