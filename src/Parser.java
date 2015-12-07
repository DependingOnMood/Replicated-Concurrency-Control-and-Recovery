import java.io.BufferedReader;
import java.io.FileReader;

/**
 * The Parser class read and parse the commands from the commands file, call the
 * needed functions in other class for processing
 * 
 * @author Dongbo Xiao
 * @author Wenzhao Zang
 *
 */
public class Parser {
	int mTime = 0;

	/**
	 * time getter
	 * 
	 * @return time
	 */
	public int getTime() {
		return mTime;
	}

	/**
	 * main parse file function, parse the file into lines
	 * 
	 * @param fileName
	 * @return boolean if parse success
	 */
	public boolean parseFile(String fileName) {

		// This will reference one line at a time
		String line = "";

		try {
			// FileReader reads text files in the default encoding.
			FileReader fileReader = new FileReader(fileName);

			// Always wrap FileReader in BufferedReader.
			BufferedReader bufferedReader = new BufferedReader(fileReader);

			while ((line = bufferedReader.readLine()) != null) {

				if (line.startsWith("//")) {
					continue;
				}

				parseLine(line, true);
			}

			// Always close files.
			bufferedReader.close();
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	/**
	 * parse a specific line in the file
	 * 
	 * @param line
	 * @param shouldAddTime
	 * @return boolean if parse success
	 */
	private boolean parseLine(String line, boolean shouldAddTime) {

		if (shouldAddTime) {
			mTime++;
		}

		line = line.replaceAll("\\s", "");

		if (line.contains(";")) {
			String[] opArray = line.split(";");

			for (String op : opArray) {
				parseLine(op, false);
			}
			
		} else if (line.startsWith("begin(")) {
			Manager.begin(findTID(line), mTime, false);
			
		} else if (line.startsWith("beginRO(")) {
			Manager.begin(findTID(line), mTime, true);
			
		} else if (line.contains("R(")) {
			callRead(line);
			
		} else if (line.contains("W(")) {
			callWrite(line);
			
		} else if (line.startsWith("end(")) {
			Manager.end(Manager.getTransactionList().get(findTID(line)), true);
			
		} else if (line.contains("fail(")) {
			Manager.fail(findSiteID(line));
			
		} else if (line.contains("recover(")) {
			Manager.recover(findSiteID(line));
			
		} else if (line.startsWith("dump()")) {
			Manager.dump();
			
		} else if (line.startsWith("dump(x")) {
			Manager.dumpVariable(findXID(line));
			
		} else if (line.startsWith("dump(")) {
			Site[] sites = Manager.getSites();
			sites[findSiteID(line) - 1].printSite();
		}
		
		return true;
	}

	/**
	 * find the Transaction ID in an input command
	 * 
	 * @param string
	 * @return TID
	 */
	private String findTID(String s) {
		int firstP = s.indexOf("(");
		int lastP = s.indexOf(")");

		return s.substring(firstP + 1, lastP);
	}

	/**
	 * find the X Position ID in an input command
	 * 
	 * @param string
	 * @return x position ID
	 */
	private String findXID(String s) {
		int firstP = s.indexOf("(");
		int lastP = s.indexOf(")");

		return s.substring(firstP + 1, lastP);
	}

	/**
	 * find the Site ID in an input command
	 * 
	 * @param string
	 * @return site ID
	 */
	private int findSiteID(String s) {
		int firstP = s.indexOf("(");
		int lastP = s.indexOf(")");

		String siteId = s.substring(firstP + 1, lastP);

		return Integer.parseInt(siteId);
	}

	/**
	 * find the variables in a read command, and call read function in Action
	 * class
	 * 
	 * @param string
	 */
	public void callRead(String s) {
		int firstP = s.indexOf("(");
		int lastP = s.indexOf(")");

		String variablesString = s.substring(firstP + 1, lastP);
		String[] variables = variablesString.split(",");

		Manager.read(variables[0], variables[1]);

	}

	/**
	 * find the variables in a write command, and call write function in Action
	 * class
	 * 
	 * @param string
	 */
	public void callWrite(String s) {
		int firstP = s.indexOf("(");
		int lastP = s.indexOf(")");

		String variablesString = s.substring(firstP + 1, lastP);
		String[] variables = variablesString.split(",");

		Manager.write(variables[0], variables[1], Integer.parseInt(variables[2]));
	}
}
