package etomica.lattice;
import etomica.*;

/**
 * Primitive group for an orthorhombic system.  All primitive
 * vectors orthogonal but not necessarily of equal length.
 */
public class PrimitiveOrthorhombic extends Primitive {
    
    public PrimitiveOrthorhombic(Space space) {
        super(space);
        //set up orthogonal vectors of unit size
        setSize(1.0);
    }
    
    /**
     * Sets the length of each primitive vector to the corresponding
     * value in the given array.
     */
    public void setSize(double[] size) {
        if(size.length != D) throw new IllegalArgumentException("Error in PrimitiveOrthorhombic.setSize: Number of sizes given is inconsistent with number of primitive vectors");
        for(int i=0; i<D; i++) {
            r[i].setComponent(i,size[i]);
            this.size[i] = size[i];
        }
    }
    
    /**
     * Sets the length of all primitive vectors to the given value.
     */
    public void setSize(double size) {
        for(int i=0; i<D; i++) {
            r[i].setComponent(i,size);
            this.size[i] = size;
        }
    }
    
    public int[] latticeIndex(Space.Vector q) {
        for(int i=0; i<D; i++) idx[i] = (int)(q.component(i)/size[i]);
        return idx;
    }
    
    public Primitive reciprocal() {
        throw new RuntimeException("method PrimitiveOrthorhombic.reciprocal not yet implemented");
    }
    
    public AtomFactory wignerSeitzCellFactory() {
        throw new RuntimeException("method PrimitiveOrthorhombic.wignerSeitzCell not yet implemented");
    }
    
    public AtomFactory unitCellFactory() {
        throw new RuntimeException("method PrimitiveOrthorhombic.unitCell not yet implemented");
    }
    
    private double[] size;

///////////////////////////////////////////////////////////////////////////////////////////

public class UnitCellFactory extends AtomFactory {

    AtomType atomType;
    
    public UnitCellFactory(Space space) {
        super(space);
        setType(new AtomType(this));//default
    }
    
    public void setType(AtomType t) {atomType = t;}
    public AtomType type() {return atomType;}

    /**
     * Builds a single unit cell.
     */
    protected Atom build() {
        return new UnitCell(space, atomType);
    }
    
}//end of UnitCellFactory

///////////////////////////////////////////////////////////////////////////////////////////

/**
 * A cubic unit cell.  Position of the cell is given by the vertex
 * in which each coordinate is minimized.
 */
public class UnitCell extends AbstractCell {
    
    public UnitCell(Space space, AtomType type) {
        super(space, type);
    }
    /**
     * Dimension of the space occupied by the cell
     */
     public int D() {return space.D();}
     
    /**
     * Returns the volume of the cubic cell.
     */
    public double volume() {
 /*       double sizeN = size;
        for(int i=D()-1; i>0; i--) sizeN *= size;
        return sizeN;*/
        return 0.0;
    }
    /**
     * Returns the positions of the vertices relative to the cell position.
     * Absolute positions are obtained by adding the coordinate.position vector.
     * Note that vertices might be computed on-the-fly, with each call of the method, rather than
     * computed once and stored; thus it may be worthwhile to store the values if using them often, 
     * but if doing so be careful to update them if any transformations are done to the lattice.
     */
    public Space.Vector[] vertex() {
        return null;
    }
    
    /**
     * Returns <code>true</code> if the given vector lies inside the cell, <code>false</code> otherwise.
     */
    public boolean inCell(Space.Vector v) {
  /*      double x = size;
        switch(D()) {
            case 3: x = v.component(2);
                    if(x < 0.0 || x > r[2].component(2)) return false;
            case 2: x = v.component(1);
                    if(x < 0.0 || x > r[1].component(1)) return false;
            case 1: x = v.component(0);
                    if(x < 0.0 || x > r[0].component(0)) return false;
                    break;
            default: throw new RuntimeException("PrimitiveCubic.UnitCell.inCell not implemented for given dimension");
        }*/
        return true;
    }
}//end of UnitCell

}//end of PrimitiveOrthorhombic
    
