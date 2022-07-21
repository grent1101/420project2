package cmsc420_s22;

// YOU SHOULD NOT MODIFY THIS FILE

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Collections;
import java.util.Iterator;
import java.util.Scanner;

/**
 * Command handler. This inputs a single command line, processes the command (by
 * invoking the appropriate method(s) on the HBkdTree)) and returns the result
 * as a string.
 */

public class CommandHandler {

	private boolean initialized; // have we initialized the structure yet?
	private int maxHeightDifference; // max allowed height difference
	private HBkdTree<Airport> hbkdTree; // the HB kd-tree
	private HashMap<String, Airport> airports; // airport codes seen so far

	/**
	 * Initialize command handler
	 */
	public CommandHandler() {
		maxHeightDifference = Integer.MAX_VALUE;
		airports = new HashMap<String, Airport>();
		hbkdTree = null;
	}

	/**
	 * Process a single command and return the string output. Each line begins with
	 * a command (e.g., find, insert, delete) followed by a list of arguments. The
	 * arguments are separated by colons (":"). For example, the command to delete
	 * the LAX airport is "delete:LAX".
	 *
	 * The preorder command gets a listing of the airports in preorder. It prints
	 * this list and also generates a nicely indented (inorder) presentation of the
	 * tree's contents.
	 * 
	 * @param inputLine The input line with the command and parameters.
	 * @return A short summary of the command's execution/result.
	 */

	public String processCommand(String inputLine) throws Exception {
		String output = new String(); // for storing summary output
		Scanner line = new Scanner(inputLine);
		try {
			line.useDelimiter(":"); // use ":" to separate arguments
			String cmd = (line.hasNext() ? line.next() : ""); // next command
			// -----------------------------------------------------
			// INITIALIZE
			// - this command must come first in the input
			// - sets the max height difference and bounding box
			// -----------------------------------------------------
			if (cmd.compareTo("initialize") == 0) {
				maxHeightDifference = line.nextInt(); // read the difference
				if (maxHeightDifference <= 0) {
					throw new Exception("Error - max-height-difference must be at least 1");
				}
				double xMin = line.nextDouble(); // bounding box
				double xMax = line.nextDouble();
				double yMin = line.nextDouble();
				double yMax = line.nextDouble();
				if (xMin > xMax || yMin > yMax) {
					throw new Exception("Error - invalid bounding box dimensions");
				}
				if (initialized) {
					throw new Exception("Error - Second attempt to initialize");
				} else {
					Rectangle2D bbox = new Rectangle2D(new Point2D(xMin, yMin), new Point2D(xMax, yMax));
					hbkdTree = new HBkdTree<Airport>(maxHeightDifference, bbox); // create a new tree
					output += "initialize: height-difference = " + maxHeightDifference + ", bounding-box = " + bbox
							+ System.lineSeparator();
					initialized = true;
				}
			}
			// -----------------------------------------------------
			// COMMENT string
			// - comment line for the output
			// -----------------------------------------------------
			else if (cmd.compareTo("comment") == 0) {
				String comment = line.next(); // read the comment
				output += "[" + comment + "]" + System.lineSeparator();
			}
			// -----------------------------------------------------
			// INSERT code city x y
			// -----------------------------------------------------
			else if (cmd.compareTo("insert") == 0) {
				confirmInitialized(); // confirm that we are initialized
				String code = line.next(); // get parameters
				String city = line.next();
				double x = line.nextDouble();
				double y = line.nextDouble();
				Airport ap = new Airport(code, city, x, y); // create airport object
				output += "insert(" + code + "): ";
				Airport ap2 = airports.get(code);
				if (ap2 != null) { // code already exists?
					throw new Exception("Insertion of duplicate airport code");
				}
				hbkdTree.insert(ap); // insert into kd-tree
				airports.put(code, ap); // insert into dictionary
				output += "successful {" + ap.getString("attributes") + "}" + System.lineSeparator();
			}
			// -----------------------------------------------------
			// DELETE code
			// -----------------------------------------------------
			else if (cmd.compareTo("delete") == 0) {
				confirmInitialized(); // confirm that we are initialized
				String code = line.next(); // get parameters
				output += "delete(" + code + "): ";
				Airport ap = airports.get(code); // look up the airport
				if (ap == null) { // no such airport?
					throw new Exception("Deletion of nonexistent airport code");
				}
				hbkdTree.delete(ap.getPoint2D()); // delete from kd-tree
				airports.remove(code); // delete from dictionary
				output += "successful" + System.lineSeparator();
			}
			// -----------------------------------------------------
			// DELETE-POINT code
			// Warning: Just for testing. When deleting points that are
			// actually in the tree, use delete above.
			// -----------------------------------------------------
			else if (cmd.compareTo("delete-point") == 0) {
				confirmInitialized(); // confirm that we are initialized
				double x = line.nextDouble(); // get coordinates
				double y = line.nextDouble();
				Point2D pt = new Point2D(x, y);
				output += "delete-point(" + pt + "): ";
				hbkdTree.delete(pt); // delete from kd-tree
				output += "successful" + System.lineSeparator();
			}
			// -----------------------------------------------------
			// CLEAR
			// -----------------------------------------------------
			else if (cmd.compareTo("clear") == 0) {
				confirmInitialized(); // confirm that we are initialized
				hbkdTree.clear(); // clear the kd-tree
				airports.clear(); // clear the airports map
				output += "clear: successful" + System.lineSeparator();
			}
			// -----------------------------------------------------
			// SIZE
			// -----------------------------------------------------
			else if (cmd.compareTo("size") == 0) {
				confirmInitialized(); // confirm that we are initialized
				int size = hbkdTree.size(); // get the tree's current size
				output += "size: " + size + System.lineSeparator();
			}
			// -----------------------------------------------------
			// FIND code
			// -----------------------------------------------------
			else if (cmd.compareTo("find") == 0) {
				confirmInitialized(); // confirm that we are initialized
				double x = line.nextDouble();
				double y = line.nextDouble();
				Point2D pt = new Point2D(x, y);
				Airport result = hbkdTree.find(pt); // find in tree
				output += summarizeFind(cmd, pt, result); // summarize result
			}
			// -----------------------------------------------------
			// RECTANGULAR-RANGE - return the points that lie within
			// a rectangle given by its x and y intervals.
			// -----------------------------------------------------
			else if (cmd.compareTo("orthog-range-report") == 0) {
				confirmInitialized(); // confirm that we are initialized
				double xMin = line.nextDouble();
				double xMax = line.nextDouble();
				double yMin = line.nextDouble();
				double yMax = line.nextDouble();
				if (xMax < xMin || yMax < yMin) {
					throw new Exception("Error - Invalid bounds for orthog-range-report");
				}
				Rectangle2D query = new Rectangle2D(new Point2D(xMin, yMin), new Point2D(xMax, yMax));
				ArrayList<Airport> list = hbkdTree.orthogRangeReport(query);
				if (list == null) {
					throw new Exception("Error - orthogRangeReport returned a null result");
				}
				Collections.sort(list); // sort by code
				Iterator<Airport> iter = list.iterator(); // iterator for the list
				output += "orthog-range-report x=[" + xMin + " .. " + xMax + "] y=[" + yMin + " .. " + yMax + "] :"
						+ System.lineSeparator();
				if (iter.hasNext()) {
					while (iter.hasNext()) { // output the list (flat)
						output += "  " + iter.next() + System.lineSeparator();
					}
				} else {
					output += "  [Empty]" + System.lineSeparator();
				}
			}
			// -----------------------------------------------------
			// PREORDER - get a preorder list of entries and print
			// the tree with indentation
			// -----------------------------------------------------
			else if (cmd.compareTo("preorder") == 0) {
				confirmInitialized(); // confirm that we are initialized
				ArrayList<String> list = hbkdTree.getPreorderList();
				if (list == null)
					throw new Exception("Error - getPreorderList returned a null result");
				Iterator<String> iter = list.iterator(); // iterator for the list
				output += "Preorder list:" + System.lineSeparator();
				while (iter.hasNext()) { // output the preorder list (flat)
					output += "  " + iter.next() + System.lineSeparator();
				}
				output += treeStructure(list); // summarize tree contents (indented)
			}
			//
			// -----------------------------------------------------
			// Invalid command or empty
			// -----------------------------------------------------
			else {
				if (cmd.compareTo("") == 0)
					System.err.println("Error: Empty command line (Ignored)");
				else
					System.err.println("Error: Invalid command - \"" + cmd + "\" (Ignored)");
			}
		} catch (Exception e) { // exception thrown?
			output += "failure due to exception: \"" + e.getMessage() + "\"" + System.lineSeparator();
		} catch (Error e) { // error occurred?
			System.err.print("Operation failed due to error: " + e.getMessage());
			e.printStackTrace(System.err);
		} finally { // always executed
			line.close(); // close the input scanner
		}
		return output; // return summary output
	}

	/**
	 * Confirm that the data structure has been initialized, or throw an exception.
	 */
	void confirmInitialized() throws Exception {
		if (!initialized) {
			throw new Exception("Error: First command must be 'initialize'.");
		}
	}

	/**
	 * Summarize the results of the find commands.
	 * 
	 * @param cmd    The command (find)
	 * @param result The find result (null, if not found)
	 * @return String encoding the results
	 */
	static String summarizeFind(String cmd, Point2D pt, Airport result) {
		String output = new String(cmd + "(" + pt + "): ");
		if (result != null) {
			output += "found [" + result + "]" + System.lineSeparator();
		} else {
			output += "not found" + System.lineSeparator();
		}
		return output;
	}

	/**
	 * Summarize the results of the find commands.
	 * 
	 * @param cmd    The command (find-smaller, find-larger)
	 * @param result The find result (null, if not found)
	 * @return String encoding the results
	 */
	static String summarizeFindSL(String cmd, double x, Airport result) {
		String output = new String(cmd + "(" + x + "): ");
		if (result != null) {
			output += "found [" + result + "]" + System.lineSeparator();
		} else {
			output += "not found" + System.lineSeparator();
		}
		return output;
	}

	/**
	 * Summarize the results of the get command.
	 * 
	 * @param cmd    The command (get-min or get-max)
	 * @param result The find result (null, if not found)
	 * @return String encoding the results
	 */
	static String summarizeGet(String cmd, Airport result) {
		String output = new String(cmd + ": ");
		if (result != null) {
			output += "found [" + result + "]" + System.lineSeparator();
		} else {
			output += "no entries" + System.lineSeparator();
		}
		return output;
	}

	/**
	 * Summarize the results of the get command.
	 * 
	 * @param cmd    The command (remove-min or remove-max)
	 * @param result The find result (null, if not found)
	 * @return String encoding the results
	 */
	static String summarizeRemove(String cmd, Airport result) {
		String output = new String(cmd + ": ");
		if (result != null) {
			output += "removed [" + result + "]" + System.lineSeparator();
		} else {
			output += "no entries" + System.lineSeparator();
		}
		return output;
	}

	/**
	 * Print the tree contents with indentation.
	 * 
	 * @param entries List of entries in preorder
	 * @return String encoding the tree structure
	 */
	static String treeStructure(ArrayList<String> entries) {
		String output = "Tree structure:" + System.lineSeparator();
		Iterator<String> iter = entries.iterator(); // iterator for the list
		if (iter.hasNext()) { // tree is nonempty
			output += treeStructureHelper(iter, "  "); // print everything
		}
		return output;
	}

	/**
	 * Recursive helper for treeStructure. The argument iterator specifies the next
	 * node from the preorder list to be printed, and the argument indent indicates
	 * the indentation to be performed (of the form "| | | ...").
	 * 
	 * @param iter   Iterator for the entries in the list
	 * @param indent String indentation for the current line
	 */
	static String treeStructureHelper(Iterator<String> iter, String indent) {
		final String levelIndent = "| "; // the indentation for each level of the tree
		String output = "";
		if (iter.hasNext()) {
			String entry = iter.next(); // get the next entry
			Boolean isExtern = (entry.length() > 0 && entry.charAt(0) == '['); // external?
			if (isExtern) { // print external node entry
				// output += indent + entry + System.lineSeparator();
			} else {
				output += treeStructureHelper(iter, indent + levelIndent); // print left subtree
				output += indent + entry + System.lineSeparator(); // print this node
				output += treeStructureHelper(iter, indent + levelIndent); // print right subtree
			}
		} else {
			System.err.println("Unexpected trailing elements in entries list"); // shouldn't get here!
		}
		return output;
	}
}
