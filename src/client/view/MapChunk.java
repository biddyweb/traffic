package client.view;

import java.util.ArrayList;
import java.util.List;

import client.ClientApp;
import data.ClientMapWay;
import data.LatLongPoint;

/**
 * Represents a chunk of data consisting of MapWays to draw to screen
 * 
 * @author dgattey
 */
public class MapChunk implements Runnable {
	
	public static final double			CHUNKSIZE	= 0.01;
	private final List<ClientMapWay>	ways;
	private boolean						isAdding;
	private final ClientApp				app;
	private final LatLongPoint			min;
	private final LatLongPoint			max;
	
	/**
	 * Constructor takes an app for its hub reference, a min and a max, and spawns a new thread to find that chunk from
	 * file
	 * 
	 * @param app the app running this
	 * @param c the canvas to repaint
	 * @param min the minimum location
	 * @param max the maximum location
	 */
	public MapChunk(final ClientApp app, final MapView c, final LatLongPoint min, final LatLongPoint max) {
		ways = new ArrayList<>();
		this.app = app;
		this.min = min;
		this.max = max;
		
	}
	
	/**
	 * Returns all ways represented in this chunk if they aren't currently being paged in
	 * 
	 * @return a list of MapWays
	 */
	public List<ClientMapWay> getWays() {
		if (!isAdding) {
			return ways;
		}
		return new ArrayList<>();
	}
	
	/**
	 * Pages in map data for the given min and max and repaints the canvas
	 */
	@Override
	public void run() {
		final List<ClientMapWay> found = app.getHub().getChunk(min, max);
		isAdding = true;
		ways.addAll(found);
		isAdding = false;
		app.getViewController().repaintMap();
	}
	
}
