package data;

/**
 * Exception for generic maps error
 * 
 * @author dgattey
 */
public class MapException extends Exception {
	
	private static final long	serialVersionUID	= -8569643197882240921L;
	
	/**
	 * Constructor just calls the super with the given message
	 * 
	 * @param msg the string message to print
	 */
	public MapException(final String msg) {
		super(msg);
	}
	
}
