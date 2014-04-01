package backend.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Contains functionality to binary search large data-sets (TSV files)
 * 
 * @author aiguha
 */
public class BinaryFileSearcher {
	
	public BinaryFileSearcher() throws IOException {
		
	}
	
	/**
	 * Reads the next valid record from a given pointer location, middle in the file r Side effect: places r's file
	 * pointer at the start of the found record (if the record is found)
	 * 
	 * @param r
	 * @param middle
	 * @return the string representation of the next valid record, or null if there isn't one
	 * @throws IOException
	 */
	public static String readNextValidRecord(final RandomAccessFile r, final long middle) throws IOException {
		r.seek(middle);
		// Skips middle of line (first line never has to be read)
		r.readLine();
		final long startOfRecord = r.getFilePointer();
		final int bufSize = 256;
		final byte[] buffer = new byte[bufSize];
		final ByteArrayOutputStream bAOS = new ByteArrayOutputStream(bufSize);
		
		while (true) {
			final int ret = r.read(buffer);
			// On EOF, read returns -1
			if (ret == -1) {
				return null;
			}
			for (int i = 0; i < ret; i++) {
				// Found the end of a record
				if ((char) buffer[i] == '\n') {
					r.seek(startOfRecord);
					return new String(bAOS.toByteArray(), StandardCharsets.UTF_8);
				}
				// Writes record byte by byte to bAOS
				bAOS.write(buffer[i]);
			}
		}
	}
	
	/**
	 * Searches forward from the current spot in r for all
	 * 
	 * @param r
	 * @param search
	 * @param searchIndex
	 * @param delimiter
	 * @return
	 * @throws IOException
	 * @throws DataSetException
	 */
	private static List<String> searchForward(final RandomAccessFile r, final String search, final int searchIndex,
			final String delimiter, final boolean onlyEqual, final boolean matchPartial) throws IOException,
			DataSetException {
		// System.out.println("Entered search Forward. R was at: " + r.getFilePointer());
		// Skip to a new line
		r.readLine();
		final int bufSize = 512;
		final byte[] buffer = new byte[bufSize];
		final ByteArrayOutputStream bAOS = new ByteArrayOutputStream(bufSize);
		final List<String> records = new ArrayList<>();
		while (true) {
			final int ret = r.read(buffer);
			// On EOF, read returns -1
			if (ret == -1) {
				break;
			}
			for (int i = 0; i < ret; i++) {
				// Found the end of a record
				if ((char) buffer[i] == '\n') {
					final String rec = new String(bAOS.toByteArray(), StandardCharsets.UTF_8);
					final int compare = (matchPartial) ? ParserTools.searchPartialInRecord(rec, search, searchIndex,
							delimiter) : ParserTools.searchFullyInRecord(rec, search, searchIndex, delimiter);
					if (compare > 0) {
						return records;
					} else if (compare == 0 || !onlyEqual) {
						records.add(rec);
						bAOS.reset();
					}
				} else {
					// Writes record byte by byte from buffer to bAOS
					bAOS.write(buffer[i]);
				}
			}
		}
		
		return records;
		
	}
	
	private static List<String> jumpBackwardAndSearchForward(final RandomAccessFile r, final String start,
			final String end, final int searchIndex, final String delimiter, final boolean matchPartial)
			throws IOException, DataSetException {
		// System.out.println("Entered jumpBackwardAndSearchForward");
		final List<String> found = new ArrayList<>();
		final int bufSize = 256;
		long cur1 = r.getFilePointer();
		// Jumping Backwards
		boolean stop = false;
		while (!stop) {
			
			cur1 = Math.max(0, cur1 - bufSize);
			final String record = readNextValidRecord(r, cur1);
			final int compare = (matchPartial) ? ParserTools.searchPartialInRecord(record, start, searchIndex,
					delimiter) : ParserTools.searchFullyInRecord(record, start, searchIndex, delimiter);
			if (compare != 0 || cur1 == 0) {
				if (compare == 0) {
					found.add(record);
				}
				stop = true;
			}
			
		}
		stop = false;
		// Reading forwards to first valid record
		while (!stop) {
			long cur = r.getFilePointer();
			final String record = readNextValidRecord(r, cur);
			if (record == null) {
				return found;
			}
			final int compare = (matchPartial) ? ParserTools.searchPartialInRecord(record, start, searchIndex,
					delimiter) : ParserTools.searchFullyInRecord(record, start, searchIndex, delimiter);
			if (compare > 0) {
				break;
			} else if (compare < 0) {
				cur = cur + record.length();
			} else {
				found.add(record);
				break;
			}
			
		}
		final boolean onlyEqual = !(start.compareTo(end) < 0);
		found.addAll(searchForward(r, end, searchIndex, delimiter, onlyEqual, matchPartial));
		return found;
	}
	
	/**
	 * Binary Search Helper Performs binary search inside this random access file. <br>
	 * Wrap all calls to this method with the opening and closing of the random access file (along with other effects)
	 * 
	 * @param r
	 * @param search
	 * @param searchIndex
	 * @param delimiter
	 * @param matchPartial true if the searcher should try to make partial matches between record and search
	 * @return
	 * @throws DataSetException
	 * @throws IOException
	 */
	public static String binarySearchHelper(final RandomAccessFile r, final String search, final int searchIndex,
			final String delimiter, final boolean matchPartial) throws DataSetException, IOException {
		final long length = r.length();
		long top = length;
		long bottom = 0;
		long middle = 0;
		while (bottom <= top) {
			middle = (bottom + top) / 2;
			final String record = readNextValidRecord(r, middle);
			// No valid record after middle, so search value must be before
			if (record == null) {
				top = middle - 1;
				continue;
			}
			final int compare = (matchPartial) ? ParserTools.searchPartialInRecord(record, search, searchIndex,
					delimiter) : ParserTools.searchFullyInRecord(record, search, searchIndex, delimiter);
			if (compare == 0) {
				return record;
			} else if (compare < 0) {
				bottom = middle + 1;
			} else {
				top = middle - 1;
			}
		}
		return null;
	}
	
	/**
	 * Binary Search Performs a binary search on fNum file Searches for records containing search in the relevant index
	 * hIndex
	 * 
	 * @param filename the filename
	 * @param search the value to be searched for
	 * @param hIndex the index within the record containing the value of interest
	 * @param delimiter the splitter to use
	 * @return the entire record as a string or null
	 * @throws IOException
	 * @throws DataSetException
	 */
	public static String simpleBinarySearch(final String filename, final String search, final int searchIndex,
			final String delimiter) throws IOException, DataSetException {
		if (filename == null || search == null || searchIndex < 0 || delimiter == null) {
			throw new DataSetException("Internal: Invalid arguments to binarySearch");
		}
		final RandomAccessFile r = new RandomAccessFile(filename, "r");
		final String record = binarySearchHelper(r, search, searchIndex, delimiter, false);
		r.close();
		return record;
	}
	
	/**
	 * Find all matching records using binarySearch rather than just one record
	 * 
	 * @param filename
	 * @param search
	 * @param searchIndex
	 * @param delimiter
	 * @return
	 * @throws DataSetException
	 * @throws IOException
	 */
	public static List<String> findMatchingRecords(final String filename, final String search, final int searchIndex,
			final String delimiter) throws DataSetException, IOException {
		if (filename == null || search == null || searchIndex < 0 || delimiter == null) {
			throw new DataSetException("Internal: Invalid arguments to findMatchingRecords");
		}
		final RandomAccessFile r = new RandomAccessFile(filename, "r");
		binarySearchHelper(r, search, searchIndex, delimiter, false);
		final List<String> records = new ArrayList<>();
		records.addAll(jumpBackwardAndSearchForward(r, search, search, searchIndex, delimiter, false));
		r.close();
		return records;
	}
	
	public static List<String> getPage(final String filename, String start, String end, final int searchIndex,
			final String delimiter) throws DataSetException, IOException {
		if (filename == null || start == null || searchIndex < 0 || end == null || delimiter == null) {
			throw new DataSetException("Internal: Invalid arguments to findMatchingRecords");
		}
		final String temp = end;
		if (start.compareTo(end) > 0) {
			end = start;
			start = temp;
		}
		final RandomAccessFile r = new RandomAccessFile(filename, "r");
		binarySearchHelper(r, start, searchIndex, delimiter, true);
		final List<String> records = new ArrayList<>();
		records.addAll(jumpBackwardAndSearchForward(r, start, end, searchIndex, delimiter, true));
		r.close();
		return records;
	}
	
}
