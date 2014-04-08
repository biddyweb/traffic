package server.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import main.Utils;
import data.ProtocolManager;

/**
 * Wraps the client socket and encapsulates all IO
 * 
 * @author aiguha
 */
public class ClientHandler extends Thread {
	
	private final Socket			_client;
	private final BufferedReader	_input;
	private final PrintWriter		_output;
	
	private final Server			_server;
	
	/**
	 * Constructs a ClientHandler on the given client
	 * 
	 * @param client the client to handle
	 * @throws IOException if the client socket is invalid
	 * @throws IllegalArgumentException if client is null
	 */
	public ClientHandler(final Socket client, final Server server) throws IOException {
		if (client == null) {
			throw new IllegalArgumentException("Cannot accept null arguments.");
		}
		
		_client = client;
		_input = new BufferedReader(new InputStreamReader(_client.getInputStream()));
		_output = new PrintWriter(_client.getOutputStream(), true);
		_server = server;
	}
	
	/**
	 * @return tells whether the response handler was ready
	 */
	boolean dispatch() {
		String req_start = "";
		try {
			
			// Stops the hanging issue
			if (!_server.getRC().isReady()) {
				return false;
			}
			
			// Do stuff!
			req_start = _input.readLine();
			System.out.println("GOT: " + req_start);
			if (req_start == null) {
				_server.getRC().errorResponse(_output, null);
			} else if (req_start.startsWith(ProtocolManager.TR_Q)) {
				System.out.println("Adding this client");
				_server.addClientToTrafficPool(this);
			} else if (req_start.startsWith(ProtocolManager.AC_Q)) {
				_server.getRC().autocorrectResponse(_input, _output);
			} else if (req_start.startsWith(ProtocolManager.RS_Q)) {
				_server.getRC().routeFromNamesResponse(_input, _output);
			} else if (req_start.startsWith(ProtocolManager.RP_Q)) {
				_server.getRC().routeFromClicksResponse(_input, _output);
			} else if (req_start.startsWith(ProtocolManager.MC_Q)) {
				_server.getRC().mapDataResponse(_input, _output);
			} else {
				_server.getRC().errorResponse(_output, null);
			}
		} catch (final IOException e) {
			// It's possible that the IOException was caused by writing to a closed socket, in which case trying
			// to write again doesn't make a whole lot of sense. I suppose we just try responsding and then "kill" the
			// client
			try {
				_server.getRC().errorResponse(_output, e);
			} catch (final IOException e1) {
				kill();
			}
		}
		return true;
		
	}
	
	/**
	 * 
	 */
	@Override
	public void run() {
		// The worker thread is created so the main client thread may listen to heartbeats from
		// the client end, to know when a client hangs up unexpectedly
		final Thread worker = new Thread() {
			
			@Override
			public void run() {
				if (!dispatch()) {
					kill();
				}
			}
		};
		
		worker.start();
		try {
			Thread.sleep(2000);
			
			while (worker.isAlive()) {
				// Check if peer connection still exists
			}
		} catch (final InterruptedException e) {}
		kill();
		
	}
	
	public BufferedReader getReader() {
		return _input;
	}
	
	/**
	 * Send a string to the client via the socket
	 * 
	 * @param message response to send
	 * @throws IOException
	 */
	public void send(final String message) throws IOException {
		_output.write(message);
		_output.flush();
		_client.shutdownOutput();
	}
	
	/**
	 * Sends without closing
	 * 
	 * @param message
	 * @throws IOException
	 */
	public void sendWithoutClosing(final String message) throws IOException {
		_output.write(message);
		_output.flush();
	}
	
	/**
	 * Close this socket and its related streams.
	 * 
	 * @throws IOException Passed up from socket
	 */
	public void kill() {
		// Close all the streams after the client disconnects.
		try {
			_client.close();
			_input.close();
			_output.close();
		} catch (final IOException e) {
			Utils.printError("<ClientHandler> Unrecoverable IO error in client");
		}
		
	}
	
}
