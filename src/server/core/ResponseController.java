package server.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Writer;
import java.util.List;

import server.autocorrect.ACController;
import server.graph.GraphController;
import server.io.DataSetException;
import server.io.IOController;
import server.kdtree.KDTreeController;
import data.LatLongPoint;
import data.MapException;
import data.MapNode;
import data.MapWay;
import data.ParseException;
import data.ProtocolManager;

public class ResponseController {
	
	static ACController		_autocorrect;
	static KDTreeController	_kdtree;
	
	// TODO: These synchronized?
	public ResponseController(final String ways, final String nodes, final String index, final String hostName,
			final int trafficPort, final int serverPort) throws MapException, IOException {
		IOController.setup(ways, nodes, index);
		_kdtree = new KDTreeController();
		_autocorrect = new ACController();
		System.out.println("Loaded server!");
	}
	
	/**
	 * Parses request and produces autocorrect response
	 * 
	 * @param r
	 * @param w
	 * @throws IOException
	 */
	public synchronized static void autocorrectResponse(final BufferedReader r, final Writer w) throws IOException {
		try {
			// Get street name
			final String input = ProtocolManager.parseStreetName(r);
			
			// Find suggestions
			final List<String> sugg = _autocorrect.suggest(input);
			ProtocolManager.checkForResponseFooter(r.readLine());
			
			// Build Response
			final StringBuilder response = new StringBuilder(256);
			response.append(ProtocolManager.AC_R);
			response.append("\n");
			response.append(ProtocolManager.encodeSuggestions(sugg));
			response.append(ProtocolManager.FOOTER);
			response.append("\n");
			w.write(response.toString());
			w.flush();
		} catch (final ParseException e) {
			errorResponse(w, e);
		}
	}
	
	/**
	 * Parse request and produces routes from names
	 * 
	 * @param c
	 * @throws IOException
	 */
	public synchronized static void routeFromNamesResponse(final BufferedReader r, final Writer w) throws IOException {
		try {
			// Get four street names
			final String street1 = ProtocolManager.parseStreetName(r);
			final String street2 = ProtocolManager.parseStreetName(r);
			final String street3 = ProtocolManager.parseStreetName(r);
			final String street4 = ProtocolManager.parseStreetName(r);
			ProtocolManager.checkForResponseFooter(r.readLine());
			
			// Find their intersections
			final MapNode inter1 = IOController.findIntersection(street1, street2);
			final MapNode inter2 = IOController.findIntersection(street3, street4);
			
			// Find the shortest route
			final List<MapWay> path = GraphController.getShortestPathWays(inter1, inter2);
			// Build Response according to protocol
			final StringBuilder response = new StringBuilder(256);
			response.append(ProtocolManager.RS_R);
			response.append("\n");
			response.append(ProtocolManager.encodeMapWayList(path));
			response.append(ProtocolManager.FOOTER);
			response.append("\n");
			w.write(response.toString());
			w.flush();
		} catch (final MapException | ParseException e) {
			errorResponse(w, e);
		}
		
	}
	
	/**
	 * @param r
	 * @param w
	 * @throws IOException
	 */
	public synchronized static void routeFromClicksResponse(final BufferedReader r, final Writer w) throws IOException {
		try {
			// Parse two points
			final LatLongPoint p1 = ProtocolManager.parseLatLongPoint(r);
			final LatLongPoint p2 = ProtocolManager.parseLatLongPoint(r);
			
			// Find Closest Neighbors
			final MapNode n1 = _kdtree.getNeighbor(p1);
			final MapNode n2 = _kdtree.getNeighbor(p2);
			
			// Find Route
			final List<MapWay> route = GraphController.getShortestPathWays(n1, n2);
			
			// Build Response
			final StringBuilder response = new StringBuilder(256);
			response.append(ProtocolManager.RP_R);
			response.append("\n");
			response.append(ProtocolManager.encodeMapWayList(route));
			response.append(ProtocolManager.FOOTER);
			response.append("\n");
			w.write(response.toString());
			w.flush();
		} catch (final ParseException | MapException e) {
			errorResponse(w, e);
		}
		
	}
	
	public synchronized static void mapDataResponse(final BufferedReader r, final Writer w) throws IOException {
		try {
			// Find min LatLongPoint of mapchunk to be generated
			final LatLongPoint p1 = ProtocolManager.parseLatLongPoint(r);
			final LatLongPoint p2 = ProtocolManager.parseLatLongPoint(r);
			
			// Find corresponding mapchunk
			final List<MapWay> chunk = IOController.getChunkOfWays(p1, p2);
			ProtocolManager.checkForResponseFooter(r.readLine());
			
			// Build Response
			final StringBuilder response = new StringBuilder(256);
			response.append(ProtocolManager.MC_R);
			response.append("\n");
			response.append(ProtocolManager.encodeMapWayList(chunk));
			response.append(ProtocolManager.FOOTER);
			response.append("\n");
			w.write(response.toString());
			w.flush();
		} catch (final ParseException | DataSetException e) {
			errorResponse(w, e);
		}
		
	}
	
	public static void errorResponse(final Writer w, final Exception e) throws IOException {
		// Build Response
		final StringBuilder response = new StringBuilder(256);
		response.append(ProtocolManager.ER_R);
		response.append(ProtocolManager.encodeError(e == null ? "No message" : e.getMessage()));
		response.append(ProtocolManager.FOOTER);
		response.append("\n");
		w.write(response.toString());
		w.flush();
	}
	
	/**
	 * Checks whether whole controller is setup
	 * 
	 * @return if IOController, KDTree, and Autocorrect is all setup
	 */
	public boolean isReady() {
		return IOController.isSetup() && _kdtree != null && _autocorrect != null;
	};
	
}
