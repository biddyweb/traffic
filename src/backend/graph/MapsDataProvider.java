package backend.graph;

import hub.MapNode;
import hub.MapWay;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import backend.io.DataSetException;
import backend.io.IOController;

public class MapsDataProvider implements DataProvider<MapNode, MapWay> {
	
	// Stored objects to avoid re-reading files
	private final HashMap<String, GraphNode<MapNode, MapWay>>	graphNodeStore;
	
	/**
	 * Constructs a MapsDataProvider that can be used to access the large datasets and dynamically construct the Maps
	 * graph
	 * 
	 * @param waysFile
	 * @param nodesFile
	 * @param indexFile
	 * @throws DataSetException
	 * @throws IOException
	 */
	public MapsDataProvider() {
		graphNodeStore = new HashMap<>();
		
	}
	
	@Override
	public GraphNode<MapNode, MapWay> getNode(final MapNode t) {
		if (t == null) {
			return null;
		}
		// Returns same GraphNode if it exists
		GraphNode<MapNode, MapWay> toReturn = graphNodeStore.get(t.getID());
		if (toReturn == null) {
			toReturn = new GraphNode<>(t);
			graphNodeStore.put(t.getID(), toReturn);
		}
		return toReturn;
	}
	
	@Override
	public List<GraphEdge<MapNode, MapWay>> getNeighborVertices(final GraphNode<MapNode, MapWay> cur)
			throws DataProviderException, IOException {
		final List<GraphEdge<MapNode, MapWay>> edges = new ArrayList<>();
		final List<String> curWays = cur.getValue().getWays();
		// Traverses list of films, constructing edges and nodes for each target actor in each film
		try {
			for (final String wayID : curWays) {
				final MapWay way = IOController.getMapWay(wayID);
				// Invalid records would fail to create MapWay objects
				if (way == null) {
					continue;
				}
				final MapNode curDest = way.getEnd();
				// Should not connect to itself
				if (curDest == cur.getValue()) {
					continue;
				}
				final GraphNode<MapNode, MapWay> destination = getNode(curDest);
				final GraphEdge<MapNode, MapWay> curNewEdge = new GraphEdge<>(way, cur, destination);
				edges.add(curNewEdge);
			}
		} catch (final DataSetException e) {
			throw new DataProviderException("File Parser failed with: " + e.getMessage());
		}
		return edges;
	}
	
}
