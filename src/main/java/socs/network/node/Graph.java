package socs.network.node;

public class Graph {
    private int[][] edges;
    private String[] labels;

    public Graph(int n) {
        edges = new int[n][n];
        labels = new String[n];
    }

    public int size() {
        return labels.length;
    }
    public void setLabel(int vertex, String label) {
        labels[vertex] = label;
    }
    public String getLabel(int vertex) {
        return labels[vertex];
    }

    public void addEdge(int src, int dst, int weight) {
        edges [src][dst] = weight;
    }
    public boolean isEdge(int src, int dst) {
        return edges[src][dst] > 0;
    }
    public void removeEdge(int src, int dst) {
        edges[src][dst] = 0;
    }

    public int getVertex(Object label) {
        for (int i=0; i < labels.length; i++) {
            if (labels[i].equals(label))
                return i;
        }
        return -1;
    }
    public int getWeight(int src, int dst) {
        return edges[src][dst];
    }

    public int getLowestDistance(int[] distance, boolean[] visited) {
        int min = Integer.MAX_VALUE;
        int destination = -1;
        for (int i = 0; i < distance.length; i++) {
            if ((visited[i] == false) && (distance[i] < min)) {
                destination = i;
                min = distance[i];
            }
        }
        return destination;
    }

    public int[] neighbors(int vertex) {
        int count = 0;
        for (int i=0; i<edges[vertex].length; i++) {
            if (edges[vertex][i] > 0)
                count++;
        }
        final int[] answer = new int[count];
        count = 0;
        for (int i=0; i<edges[vertex].length; i++) {
            if (edges[vertex][i] > 0)
                answer[count++] = i;
        }
        return answer;
    }

    public void print() {
        for (int j=0; j<edges.length; j++) {
            System.out.println(labels[j] + ": ");
            for (int i=0; i<edges[j].length; i++) {
                if (edges[j][i] > 0)
                    System.out.println(labels[i] + ": " + edges[j][i] + " ");
            }
            System.out.println();
        }
    }

}
