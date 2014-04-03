package data;

interface Convertible<T> {
	
	/**
	 * Returns a string representation of the object for the default protocol
	 * 
	 * @return String
	 */
	public String encodeObject();
	
	/**
	 * Returns the actual object represented by the String rep
	 * 
	 * @param rep String rep from encodeObject()
	 * @return T
	 */
	public T decodeObject(String rep);
	
}
