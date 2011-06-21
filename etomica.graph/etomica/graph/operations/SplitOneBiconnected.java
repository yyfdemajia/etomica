package etomica.graph.operations;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import etomica.graph.iterators.RangePermutator;
import etomica.graph.model.Graph;
import etomica.graph.model.Permutator;
import etomica.graph.operations.Factor.BCVisitor;
import etomica.graph.operations.SplitOne.SplitOneParameters;
import etomica.graph.traversal.Biconnected;

/**
 * Performs the substitution 1=a+b within each biconnected component.  Can be
 * used to perform
 * 1 = e + (-f)
 * (subsequently, you'll need to replace (-f) with (f) and
 * multiply the diagram by -1 if it had an odd number of f bonds).
 *
 * @author Andrew Schultz
 */
public class SplitOneBiconnected implements Unary {

  public Set<Graph> apply(Set<Graph> argument, Parameters params) {

    assert (params instanceof SplitParameters);
    Set<Graph> result = new HashSet<Graph>();
    for (Graph g : argument) {
      Set<Graph> newSet = apply(g, (SplitOneParameters) params);
      if (newSet != null) {
        result.addAll(newSet);
      }
    }
    IsoFree isoFree = new IsoFree();
    result = isoFree.apply(result, null);
    return result;
  }

  public Set<Graph> apply(Graph graph, SplitOneParameters params) {

    List<List<Byte>> multiBiComponents = new ArrayList<List<Byte>>();
    multiBiComponents.clear();
    BCVisitor bcv = new BCVisitor(multiBiComponents);
    new Biconnected().traverseAll(graph, bcv);
    Set<Graph> result = new HashSet<Graph>();

    // collect the Ids of all edges we must replace
    List<Byte> edges = new ArrayList<Byte>();

    for (int i=0; i<multiBiComponents.size(); i++) {
      List<Byte> biComp = multiBiComponents.get(i);
      for (int n1=0; n1<biComp.size()-1; n1++) {
        byte id1 = biComp.get(n1);
        for (int n2=n1+1; n2<biComp.size(); n2++) {
          byte id2 = biComp.get(n2);
          byte edgeId = graph.getEdgeId(id1,id2);
          if (!graph.hasEdge(edgeId)) {
            edges.add(edgeId);
          }
        }
      }
    }

    // compute all possible permutations of length |edges| consisting of two colors
    // such that each of the colors can appear any number of times from 0 to |edges|
    Permutator permutations = new RangePermutator(edges.size(), 0, edges.size());
    // each permutation is a color assignment for the edges
    while (permutations.hasNext()) {
      // copy the graph
      Graph newGraph = graph.copy();
      byte[] permutation = permutations.next();
      // modify edge colors: partition 0 => newColor0, partition 1 => newColor1
      for (byte edgePtr = 0; edgePtr < edges.size(); edgePtr++) {
        char newColor = permutation[edgePtr] == 0 ? params.newColor0() : params.newColor1();
        byte edgeId = edges.get(edgePtr);
        newGraph.putEdge(edgeId);
        newGraph.getEdge(edgeId).setColor(newColor);
      }
      result.add(newGraph);
    }
    return result;
  }
}