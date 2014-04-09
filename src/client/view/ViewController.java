package client.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;

import main.Utils;
import client.ClientApp;
import client.eventhandlers.AutocorrectHandler;
import client.eventhandlers.RouteHandler;
import data.ClientMapWay;
import data.LatLongPoint;

/**
 * Class that deals with drawing information to screen
 * 
 * @author dgattey
 */
public class ViewController {
	
	private final ClientApp						app;
	
	// Constants
	static final String							FONT				= "Arial";
	public static final Color					COLOR_WINDOW		= new Color(50, 55, 70);
	private static final Color					COLOR_BG			= Color.black;
	private static final Color					COLOR_FG			= Color.white;
	private static final Color					COLOR_CONNECTED		= new Color(0, 150, 0);
	private static final Color					COLOR_DISCONNECTED	= new Color(150, 0, 0);
	private static final String					MSG_CONNECTED		= "Connected to server";
	private static final String					MSG_DISCONNECTED	= "Disconnected from server";
	public static final String					DEFAULT_STATUS		= "Click any two points on the map or enter two intersections and press the button to find a route";
	
	// View stuff
	private JFrame								window;
	private JLabel								statusLabel;
	private JButton								routeButton;
	private final List<JComboBox<String>>		fields				= new ArrayList<>();
	private MapView								mapView;
	private JLabel								connectionLabel;
	private JPanel								connectionPanel;
	
	// Data
	private final ExecutorService				chunkerPool			= Executors.newFixedThreadPool(10);
	private final Map<LatLongPoint, MapChunk>	chunks				= new ConcurrentHashMap<>();
	private List<ClientMapWay>					route				= new ArrayList<>();
	private final LatLongPoint[]				userPoints			= new LatLongPoint[2];
	
	// Handlers
	private RouteHandler						routeHandler;
	private ScaleHandler						scaleHandler;
	private PanHandler							panHandler;
	
	/**
	 * Constructor with a hub for use later
	 * 
	 * @param app the parent app
	 */
	public ViewController(final ClientApp app) {
		this.app = app;
	}

	/**
	 * Cancels all current route requests
	 */
	public void cancelRequests() {
		if (routeHandler != null) {
			routeHandler.cancelRequests();
		}
	}
	
	/**
	 * For a given JFrame, tries to use Apple APIs to enable native fullscreen mode
	 * 
	 * @param window a JFrame window
	 */
	public static void tryEnableFullScreenMode(final Window window) {
		final String className = "com.apple.eawt.FullScreenUtilities";
		final String methodName = "setWindowCanFullScreen";
		
		try {
			final Class<?> clazz = Class.forName(className);
			final Method method = clazz.getMethod(methodName, new Class<?>[] { Window.class, boolean.class });
			method.invoke(null, window, true);
		} catch (final Throwable t) {} // We don't care if it didn't work (just means non-Apple)
	}
	
	/**
	 * Sets background and foreground color of this panel
	 * 
	 * @param panel the panel to apply it to
	 */
	private static void theme(final JComponent panel) {
		panel.setForeground(COLOR_FG);
		panel.setBackground(COLOR_BG);
	}
	
	/**
	 * Sets the padding of this panel to be x on either side and y on top and bottom
	 * 
	 * @param panel the panel
	 * @param x the horizontal padding
	 * @param y the vertical padding
	 */
	private static void addPadding(final JComponent panel, final int x, final int y) {
		panel.setBorder(new EmptyBorder(y, x, y, x));
	}
	
	/**
	 * Constructor makes a frame, canvas to go on top of it, sets up mouse listeners, and adds components
	 */
	public void create() {
		window = new JFrame(Utils.APP_NAME);
		window.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		window.setMinimumSize(new Dimension(780, 600));
		window.setResizable(true);
		tryEnableFullScreenMode(window);
		
		// Add content
		window.getContentPane().add(createTopPanel(), BorderLayout.NORTH);
		window.getContentPane().add(createConnectionBar(), BorderLayout.SOUTH);
		window.getContentPane().add(createMap(), BorderLayout.CENTER);
		
		// Show it
		window.pack();
		window.setVisible(true);
	}
	
	/**
	 * Creates the title area at top
	 * 
	 * @return a panel for the top section
	 */
	private static JPanel createTitlePanel() {
		final JPanel titleArea = new JPanel();
		final JLabel title = new JLabel(Utils.APP_NAME);
		final JLabel about = new JLabel(Utils.APP_ABOUT);
		title.setFont(new Font(FONT, Font.BOLD, 32));
		about.setFont(new Font(FONT, Font.BOLD, 16));
		titleArea.setLayout(new BoxLayout(titleArea, BoxLayout.X_AXIS));
		titleArea.add(title);
		titleArea.add(Box.createHorizontalGlue());
		titleArea.add(about);
		theme(title);
		theme(about);
		return titleArea;
	}
	
	/**
	 * Creates the bottom bar for the connection
	 * 
	 * @return a new JPanel for the connection
	 */
	private JPanel createConnectionBar() {
		connectionPanel = new JPanel();
		connectionLabel = new JLabel("Connecting...");
		connectionLabel.setFont(new Font(FONT, Font.BOLD, 10));
		theme(connectionLabel);
		theme(connectionPanel);
		addPadding(connectionPanel, 0, 2);
		connectionPanel.setAlignmentY(SwingConstants.CENTER);
		connectionPanel.setAlignmentX(SwingConstants.CENTER);
		connectionPanel.add(connectionLabel);
		return connectionPanel;
	}
	
	/**
	 * Returns panel with labels and four fields for the streets
	 * 
	 * @return the panel representing the fields
	 */
	private JPanel createFieldPanel() {
		final JPanel fieldsPanel = new JPanel();
		fieldsPanel.setLayout(new BoxLayout(fieldsPanel, BoxLayout.Y_AXIS));
		
		// One for each row
		for (int i = 0; i < 2; i++) {
			final JPanel temp = new JPanel();
			temp.setLayout(new BoxLayout(temp, BoxLayout.X_AXIS));
			theme(temp);
			
			// Label
			final JLabel l = new JLabel(String.format("Intersection %c ", ('A' + i)));
			l.setFont(new Font(FONT, Font.BOLD, 14));
			theme(l);
			temp.add(l);
			temp.add(Box.createHorizontalStrut(50));
			
			// Two new text fields with listeners
			final JComboBox<String> f1 = getComboBox();
			final JComboBox<String> f2 = getComboBox();
			final JLabel and = new JLabel("and");
			and.setFont(new Font(FONT, Font.BOLD, 14));
			theme(and);
			temp.add(f1);
			temp.add(Box.createHorizontalStrut(10));
			temp.add(and);
			temp.add(Box.createHorizontalStrut(10));
			temp.add(f2);
			fields.add(f1);
			fields.add(f2);
			
			fieldsPanel.add(temp);
			if (i == 0) {
				fieldsPanel.add(Box.createVerticalStrut(10));
			}
		}
		return fieldsPanel;
	}
	
	/**
	 * Makes a new combo box with the right listeners
	 * 
	 * @return a new ComboBox for a field
	 */
	private JComboBox<String> getComboBox() {
		final AutocorrectHandler ac = new AutocorrectHandler(app);
		final JComboBox<String> field = new JComboBox<>();
		field.setPrototypeDisplayValue("ReallyLongName Street");
		ac.setField(field);
		field.setEditable(true);
		field.getEditor().addActionListener(ac);
		field.addFocusListener(ac);
		field.getEditor().getEditorComponent().addKeyListener(ac);
		field.setEnabled(false);
		return field;
	}
	
	/**
	 * Returns the status panel
	 * 
	 * @return a panel containing the status label
	 */
	private JPanel createStatusPanel() {
		final JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
		statusLabel = new JLabel(DEFAULT_STATUS);
		panel.add(Box.createHorizontalGlue());
		panel.add(statusLabel);
		statusLabel.setFont(new Font(FONT, Font.BOLD, 12));
		theme(statusLabel);
		theme(panel);
		return panel;
	}
	
	/**
	 * Makes a panel for the buttons
	 * 
	 * @return a panel containing the buttons
	 */
	private JPanel createButtonPanel() {
		final JPanel parent = new JPanel();
		routeButton = new JButton("Find Route");
		routeButton.setEnabled(false);
		routeButton.setAlignmentX(Component.CENTER_ALIGNMENT);

		JButton clearButton = new JButton("Clear");
		clearButton.setEnabled(true);
		clearButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		clearButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				clearFields();
			}
		});
		
		parent.setLayout(new BoxLayout(parent, BoxLayout.Y_AXIS));
		parent.add(clearButton);
		parent.add(Box.createVerticalStrut(10));
		parent.add(routeButton);
		return parent;
	}
	
	/**
	 * Creates the layout panel for the top
	 * 
	 * @return a panel to put on the GUI representing AC
	 */
	private JPanel createTopPanel() {
		final JPanel top = new JPanel();
		final JPanel titleArea = createTitlePanel();
		final JPanel fieldsPanel = createFieldPanel();
		final JPanel statusPanel = createStatusPanel();
		final JPanel buttonPanel = createButtonPanel();
		final JPanel mid = new JPanel();
		mid.setLayout(new BoxLayout(mid, BoxLayout.X_AXIS));
		mid.add(fieldsPanel);
		mid.add(Box.createHorizontalGlue());
		mid.add(buttonPanel);
		
		// Theme and space the components
		theme(titleArea);
		theme(top);
		theme(mid);
		theme(fieldsPanel);
		theme(buttonPanel);
		addPadding(top, 30, 12);
		addPadding(titleArea, 10, 8);
		addPadding(statusPanel, 10, 8);
		addPadding(buttonPanel, 12, 10);
		addPadding(fieldsPanel, 12, 10);
		
		// Add items
		final MatteBorder bottomLine = BorderFactory.createMatteBorder(0, 0, 2, 0, COLOR_FG);
		final MatteBorder leftLine = BorderFactory.createMatteBorder(0, 1, 0, 0, COLOR_FG);
		buttonPanel.setBorder(BorderFactory.createCompoundBorder(leftLine, buttonPanel.getBorder()));
		top.setBorder(BorderFactory.createCompoundBorder(bottomLine, top.getBorder()));
		top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
		top.add(titleArea);
		top.add(mid);
		top.add(statusPanel);
		
		return top;
	}
	
	/**
	 * Loads map
	 * 
	 * @return a new JPanel with the map on it
	 */
	private JComponent createMap() {
		mapView = new MapView(app);
		panHandler = new PanHandler(mapView);
		scaleHandler = new ScaleHandler(mapView);
		routeHandler = new RouteHandler(app);
		mapView.addMouseListener(panHandler);
		mapView.addMouseListener(routeHandler);
		mapView.addMouseMotionListener(panHandler);
		mapView.addMouseWheelListener(scaleHandler);
		
		if (routeButton == null) {
			routeButton = new JButton("Find Route");
		}
		routeButton.addActionListener(routeHandler);
		routeButton.setEnabled(true);
		for (final JComboBox<String> box : fields) {
			box.setEnabled(true);
		}
		
		return mapView;
	}
	
	/**
	 * Looks at the viewport and pages in all chunks that haven't already been created (+/- 2 in each direction)
	 * 
	 * @param nw the NW point of the view to page in
	 * @param se the SW point of the view to page in
	 * @param sw the SW point of the view to page in
	 * @param ne the NE point of the view to page in
	 */
	void chunkInVisible(final LatLongPoint nw, final LatLongPoint se, final LatLongPoint sw, final LatLongPoint ne) {
		
		// Gets around constant addition/removal issue
		if (!app.getHub().isConnected()) {
			return;
		}
		
		// Chunk!
		final double widthToChunk = (Math.abs(nw.distance(ne)) * 100.0) + 2;
		final double heightToChunk = (Math.abs(nw.distance(sw)) * 100.0) + 2;
		// Longitude
		for (int i = -2; i <= widthToChunk; i++) {
			// Latitude
			for (int j = -2; j <= heightToChunk; j++) {
				final LatLongPoint min = nw.plus((j) / 100.0, (i) / 100.0).floor();
				
				// Only add it if not previously added
				if (!chunks.containsKey(min)) {
					final LatLongPoint max = min.plus(MapChunk.CHUNKSIZE, MapChunk.CHUNKSIZE).floor();
					final MapChunk chunk = new MapChunk(app, mapView, min, max);
					chunkerPool.execute(chunk);
					chunks.put(min, chunk);
				}
			}
		}
	}
	
	/**
	 * A bit strange, but calls the map chunk method which calls chunkInVisible with the right parameters
	 */
	public void chunk() {
		if (mapView != null) {
			mapView.chunk();
		}
	}
	
	/**
	 * Getter for the fields - first two correspond to one intersection, second two to other
	 * 
	 * @return a list of all fields
	 */
	public List<String> getFields() {
		assert (fields.size() == 4);
		final List<String> ret = new ArrayList<>();
		for (final JComboBox<String> field : fields) {
			ret.add(field.getEditor().getItem().toString());
		}
		return ret;
	}
	
	/**
	 * Clears all field text
	 */
	public void clearFields() {
		for (final JComboBox<String> field : fields) {
			field.getEditor().setItem("");
		}
	}
	
	/**
	 * Getter for the points in the canvas the user has created by clicking
	 * 
	 * @return the points to route with
	 */
	public List<LatLongPoint> getUserPoints() {
		return Arrays.asList(userPoints);
	}
	
	/**
	 * Sets a new route on the canvas to draw
	 * 
	 * @param route the new route
	 */
	public void setRoute(final List<ClientMapWay> route) {
		this.route = route;
		if (route != null && !route.isEmpty() && mapView != null) {
			mapView.centerViewOn(route.get(0).getStart().getPoint());
		}
	}
	
	/**
	 * Updates canvas with new click point
	 * 
	 * @param point the point to update
	 * @return true if there are two points and thus ready to route, false if not
	 */
	public synchronized boolean updateUserPoints(final Point2D.Double point) {
		final LatLongPoint p = mapView.screenToLatLong(point);
		
		// Either nothing or both points exist
		if (userPoints[0] == null) {
			if (route != null) {
				return false;
			}
			// Set first point
			userPoints[0] = p;
			userPoints[1] = null;
		} else if (userPoints[1] == null) {
			// Set the second point
			userPoints[1] = p;
		} else {
			clearPoints();
		}
		return userPoints[1] != null;
	}
	
	/**
	 * Sets a message for the user unless disconnected
	 * 
	 * @param message the new message
	 */
	public void setLabel(final String message) {
		if (connectionLabel.getText().toLowerCase().contains("dis")) {
			statusLabel.setText(DEFAULT_STATUS);
		} else {
			statusLabel.setText(message);
		}
	}
	
	/**
	 * Public getter for the route
	 * 
	 * @return a list of MapWays representing the route
	 */
	public List<ClientMapWay> getRoute() {
		return route;
	}
	
	/**
	 * Gives a map of all map chunks for the view to use
	 * 
	 * @return a map of min points to mapChunks to use to draw
	 */
	public Map<LatLongPoint, MapChunk> getChunks() {
		return chunks;
	}
	
	/**
	 * Erases the user clicked points
	 */
	public void clearPoints() {
		userPoints[0] = null;
		userPoints[1] = null;
	}
	
	/**
	 * Erases the current route
	 */
	public void clearRoute() {
		route = null;
	}
	
	/**
	 * Repaints the map
	 */
	public void repaintMap() {
		if (mapView != null) {
			mapView.repaint();
		}
	}
	
	/**
	 * Sets the connectionLabel
	 * 
	 * @param connected if the label should be connected
	 */
	public void setConnectionLabel(final boolean connected) {
		if (connectionLabel != null && connectionPanel != null) {
			connectionLabel.setText(connected ? MSG_CONNECTED : MSG_DISCONNECTED);
			connectionPanel.setBackground(connected ? COLOR_CONNECTED : COLOR_DISCONNECTED);
		}
	}
	
	/**
	 * If the chunk returned null, this is called to remove the chunk from the table so we can check it again later
	 * 
	 * @param min the minimum point of the chunk
	 */
	public void removeChunk(final LatLongPoint min) {
		chunks.remove(min);
	}
	
	/**
	 * For when the view needs to clear out all its chunks
	 */
	public void clearChunks() {
		chunks.clear();
	}
}
