package client.hub;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import client.ClientApp;
import data.ClientMapWay;
import data.LatLongPoint;
import data.ProtocolManager;

/**
 * @author dgattey
 */
public class HubController implements Controllable {
	
	boolean					isReady;
	private LatLongPoint	appLoadPoint;
	
	private final String	hostName;
	private final int		serverPort;
	
	/**
	 * Main Constructor for HubController
	 * 
	 * @param hostName the host name to use as the server's hostname
	 * @param serverPort the server port to use when connecting to the server
	 * @param guiApp the app that's running this hub controller
	 */
	public HubController(final String hostName, final int serverPort, final ClientApp guiApp) {
		this.hostName = hostName;
		this.serverPort = serverPort;
		// TODO: setup a test socket and make sure it's connectable - perhaps to set appLoadPoint?
		// TODO: Set isReady
	}
	
	/**
	 * Public getter for the ready attribute
	 * 
	 * @return if the app is ready
	 */
	public boolean isReady() {
		return isReady;
	}
	
	/**
	 * Gets the public load point of the app (used as center)
	 * 
	 * @return the center point of the app
	 */
	public LatLongPoint getAppLoadPoint() {
		return appLoadPoint;
	}
	
	@Override
	public List<ClientMapWay> getRoute(final LatLongPoint a, final LatLongPoint b) {
		if (a == null || b == null) {
			throw new IllegalArgumentException("null points in route finding");
		}
		// TODO: connect to server, get route for 2 points
		return null;
	}
	
	@Override
	public List<ClientMapWay> getRoute(final String streetA1, final String streetA2, final String streetB1,
			final String streetB2) {
		if (streetA1 == null || streetA1.isEmpty() || streetA2 == null || streetA2.isEmpty() || streetB1 == null
			|| streetB1.isEmpty() || streetB2 == null || streetB2.isEmpty()) {
			throw new IllegalArgumentException("empty or null street names");
		}
		// TODO: connect to server, get route for 4 streets
		// TODO: throw illegal exception for no intersections to display (from backend)
		return null;
		
	}
	
	@Override
	public List<String> getSuggestions(final String input) {
		// TODO: connect to server, get suggestions
		if (isReady) {
			
		}
		return null;
	}
	
	@Override
	public List<ClientMapWay> getChunk(final LatLongPoint min, final LatLongPoint max) {
		List<ClientMapWay> ret = null;
		if (isReady) {
			final ExecutorService executor = Executors.newSingleThreadExecutor();
			final Callable<List<ClientMapWay>> callable = new Callable<List<ClientMapWay>>() {
				
				@Override
				public List<ClientMapWay> call() throws Exception {
					final ClientCommunicator comm = new ClientCommunicator(hostName, serverPort);
					List<ClientMapWay> chunk = null;
					try {
						comm.connect();
						comm.write(min);
						comm.write(max);
						chunk = ProtocolManager.parseMapChunk(comm.getReader());
						comm.disconnect();
					} catch (final IOException e) {
						// TODO: Fix up exception handling
						e.printStackTrace();
					}
					return chunk;
				}
			};
			final Future<List<ClientMapWay>> future = executor.submit(callable);
			try {
				ret = future.get();
			} catch (InterruptedException | ExecutionException e) {
				// TODO: Fix up exception handling
				e.printStackTrace();
			}
			executor.shutdown();
		}
		return ret;
	}
}
