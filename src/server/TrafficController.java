package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import main.Utils;

public class TrafficController {
	
	Map<String, Integer>	trafficMap;
	Socket					sock;
	BufferedReader			input;
	
	public TrafficController(final String hostname, final int port) throws UnknownHostException, IOException {
		if (port < 1025) {
			throw new IllegalArgumentException("<TrafficController> Ports under 1025 are reserved");
		}
		if (Utils.isNullOrEmpty(hostname)) {
			throw new IllegalArgumentException("<TrafficController> Non-null, non-empty hostname required");
		}
		trafficMap = new HashMap<>();
		sock = new Socket(hostname, port);
		input = new BufferedReader(new InputStreamReader(sock.getInputStream()));
	}
	
	public void parseAndUpdateMap(final String data) {
		if (data == null) {
			throw new IllegalArgumentException("<TrafficController> (internal) argument should not be null");
		}
		final String[] arr = data.split("\\t");
		if (arr.length != 2) {
			return; // Ignore bad data quietly
		}
		trafficMap.put(arr[0], Integer.parseInt(arr[1]));
		
	}
	
	public void startGettingTraffic() throws IOException {
		String line;
		while ((line = input.readLine()) != null) {
			
		}
		
	}
}
