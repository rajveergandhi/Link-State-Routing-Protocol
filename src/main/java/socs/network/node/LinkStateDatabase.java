package socs.network.node;

import socs.network.message.LSA;
import socs.network.message.LinkDescription;
import java.util.List;
import java.util.ArrayList;

import java.util.HashMap;

public class LinkStateDatabase {

  //linkID => LSAInstance
  HashMap<String, LSA> _store = new HashMap<String, LSA>();

  private RouterDescription rd = null;

  public LinkStateDatabase(RouterDescription routerDescription) {
    rd = routerDescription;
    LSA l = initLinkStateDatabase();
    _store.put(l.linkStateID, l);
  }

  /**
   * output the shortest path from this router to the destination with the given IP address
   */
  String getShortestPath(String destinationIP) {
    //TODO: fill the implementation here
    Graph graph = new Graph(_store.size());
    int vertex = 0;
    for (LSA lsa: _store.values()) {
      graph.setLabel(vertex, lsa.linkStateID);
      vertex++;
    }
    for (LSA lsa: _store.values()) {
      for (LinkDescription ld: lsa.links) {
        graph.addEdge(graph.getVertex(lsa.linkStateID), graph.getVertex(ld.linkID), ld.tosMetrics);
      }
    }

    int[] prev = Dijikstra(graph);
    int src = graph.getVertex(rd.simulatedIPAddress);
    int dst = graph.getVertex(destinationIP);
    if (dst != -1) {
      List<LinkDescription> shortestPath = new ArrayList<LinkDescription>();
      int nextDst = prev[dst];

      while (dst != src) {
        LinkDescription newLink = new LinkDescription();
        newLink.linkID = graph.getLabel(dst);
        newLink.tosMetrics = graph.getWeight(nextDst, dst);
        shortestPath.add(0, newLink);
        dst = nextDst;
        nextDst = prev[nextDst];
      }
      System.out.print(rd.simulatedIPAddress);
      for (LinkDescription ld: shortestPath) {
        System.out.print(" ->(" + ld.tosMetrics + ") " + ld.linkID);
      }
    }
    return "";
  }

  //initialize the linkstate database by adding an entry about the router itself
  private LSA initLinkStateDatabase() {
    LSA lsa = new LSA();
    lsa.linkStateID = rd.simulatedIPAddress;
    lsa.lsaSeqNumber = Integer.MIN_VALUE;
    LinkDescription ld = new LinkDescription();
    ld.linkID = rd.simulatedIPAddress;
    ld.portNum = -1;
    ld.tosMetrics = 0;
    lsa.links.add(ld);
    return lsa;
  }


  public int[] Dijikstra(Graph g) {
    int s = g.getVertex(rd.simulatedIPAddress);
    int[] distance = new int[_store.size()];
    int[] prev = new int[_store.size()];
    for(int i = 0; i < prev.length; i++)
      prev[i] = -1;
    boolean[] visited = new boolean[_store.size()];
    for (int i = 0; i < distance.length; i++)
      // all nodes initialized with max value to simulate
      // an infinite distance in the initialization step
      distance[i] = Integer.MAX_VALUE;
    // source distance = 0
    distance[s] = 0;
    for (int i = 0; i < distance.length; i++) {
      // returns the node with the lowest distance from nodes not visited.
      int next = g.getLowestDistance(distance, visited);
      if (next == -1)
        return prev;
      visited[next] = true;
      int[] neighbors = g.neighbors(next);
      for (int j = 0; j < neighbors.length; j++) {
        int node = neighbors[j];
        int distanceToNext = distance[next] + g.getWeight(next, node);
        if(distance[node] > distanceToNext) {
          distance[node] = distanceToNext;
          prev[node] = next;
        }
      }
    }
    return prev;
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (LSA lsa: _store.values()) {
      sb.append(lsa.linkStateID).append("(" + lsa.lsaSeqNumber + ")").append(":\t");
      for (LinkDescription ld : lsa.links) {
        sb.append(ld.linkID).append(",").append(ld.portNum).append(",").
                append(ld.tosMetrics).append("\t");
      }
      sb.append("\n");
    }
    return sb.toString();
  }

}
