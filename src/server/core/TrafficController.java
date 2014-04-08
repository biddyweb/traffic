package server.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import main.Utils;

public class TrafficController {
	
	Map<String, Double>				trafficMap;
	private final Socket			trafficSock;
	private final BufferedReader	input;
	private final ClientPool		clients;
	
	public TrafficController(final String hostname, final int port) throws UnknownHostException, IOException {
		if (port < 1025) {
			throw new IllegalArgumentException("<TrafficController> Ports under 1025 are reserved");
		}
		if (Utils.isNullOrEmpty(hostname)) {
			throw new IllegalArgumentException("<TrafficController> Non-null, non-empty hostname required");
		}
		trafficMap = new HashMap<>();
		trafficSock = new Socket(hostname, port);
		input = new BufferedReader(new InputStreamReader(trafficSock.getInputStream()));
		clients = new ClientPool();
	}
	
	/**
	 * Returns the client pool
	 * 
	 * @return the clientpool
	 */
	public ClientPool getPool() {
		return clients;
	}
	
	public void parseAndUpdateMap(final String data) {
		if (data == null) {
			throw new IllegalArgumentException("<TrafficController> (internal) argument should not be null");
		}
		final String[] arr = data.split("\\t");
		if (arr.length != 2) {
			return; // Ignore bad data quietly
		}
		try {
			trafficMap.put(arr[0], Double.parseDouble(arr[1]));
		} catch (final NumberFormatException e) {
			Utils.printError("<TrafficController> received bad input: " + data);
		}
		
	}
	
	public void startGettingTraffic() throws IOException {
		String line;
		while ((line = input.readLine()) != null) {
			parseAndUpdateMap(line);
			clients.broadcast(line + "\n");
		}
	}
}
