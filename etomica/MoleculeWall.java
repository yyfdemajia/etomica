package simulate;

public class MoleculeWall extends Molecule {
  
  public MoleculeWall(Species s, int n) {
    super(s, n);
  }
  
  public MoleculeWall(Species s) {this(s,1);}
  
  //AtomWall is an unconventional atom
  protected void makeAtoms() {
    atom = new AtomWall[nAtoms];
    for(int i=0; i<nAtoms; i++) {atom[i] = new AtomWall(this,i);}
  }
  
  public void inflate(double scale) {
    super.inflate(scale);
    for(Atom a=firstAtom(); a!=terminationAtom(); a=a.getNextAtom()) {  //scale length of wall
        a.setDiameter(scale*a.getDiameter());
    }
  }
  
}
