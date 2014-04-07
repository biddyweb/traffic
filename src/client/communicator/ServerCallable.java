package client.communicator;

import java.io.IOException;
import java.util.concurrent.Callable;

import data.ParseException;

/**
 * Main callable class to use in creating information to send to server - should be subclassed
 * 
 * @author dgattey
 * @param <V> the type that the callable will return when it reads from the server
 */
public abstract class ServerCallable<V> implements Callable<V> {
	
	private final String	hostName;
	private final int		serverPort;
	
	/**
	 * Sets necessary information
	 * 
	 * @param hostName the name of the server to connect to
	 * @param serverPort the port of the server to connect to
	 */
	public ServerCallable(final String hostName, final int serverPort) {
		this.hostName = hostName;
		this.serverPort = serverPort;
	}
	
	/**
	 * Connects to the server, writes information and gets information back dynamically, and disconnects
	 */
	@Override
	public V call() throws IOException, ParseException {
		final CommController comm = new CommController(hostName, serverPort);
		V thing = null;
		comm.connect();
		thing = writeAndGetInfo(comm);
		comm.disconnect();
		
		return thing;
	}
	
	/**
	 * Here's where to write data you want the server to have and get whatever you need back (don't forget the header
	 * and footer after you write!)
	 * 
	 * @param comm the communication controller to use
	 * @return a V object representing what you want
	 * @throws IOException if there was some server error
	 * @throws ParseException if there was an error reading from the server
	 */
	protected abstract V writeAndGetInfo(CommController comm) throws IOException, ParseException;
	
}
