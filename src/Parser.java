import java.io.BufferedReader;
import java.io.FileReader;


public class Parser {
	
	int mTime = 0;
	
	public boolean parseFile(String fileName) {

	    // This will reference one line at a time
	    String line = "";

	    try {
	        // FileReader reads text files in the default encoding.
	        FileReader fileReader = 
	            new FileReader(fileName);

	        // Always wrap FileReader in BufferedReader.
	        BufferedReader bufferedReader = 
	            new BufferedReader(fileReader);

	        while((line = bufferedReader.readLine()) != null) {
	        	
	        	if (line.startsWith("//")){
	        		continue;
	        	}
	        	
	            parseLine(line, true);
	        }   

	        // Always close files.
	        bufferedReader.close();         
	    }
	    catch(Exception e) {
	        e.printStackTrace();
	        return false;
	    }
	    
	    return true;
	}
	
	private boolean parseLine(String line, boolean shouldAddTime){
		
		if (shouldAddTime){
			mTime++;
		}
		
		line = line.replaceAll("\\s", "");
		
		if (line.contains(";")) {
			String[] opArray = line.split(";");
			
			for (String op : opArray) {
				parseLine(op, false);
			}
		} else if (line.startsWith("begin(")) {
			begin(findTID(line));
		} else if (line.startsWith("beginRO(")) {
			beginRO(findTID(line));
		} else if (line.contains("R(")) {
			callRead(line);
		} else if (line.contains("W(")) {
			callWrite(line);
		} else if (line.contains("commit(")) {
			commit(findTID(line));
		} else if (line.startsWith("abort(")) {
			abort(findTID(line));
		} else if (line.startsWith("end(")) {
			end(findTID(line));
		} else if (line.contains("fail(")) {
			fail(findSiteID(line));
		} else if (line.contains("recover(")) {
			recover(findSiteID(line));
		} else if (line.startsWith("dump()")) {
			dump();
		} else if (line.startsWith("dump(x")) {
			dumpVariable(findXID(line));
		} else if (line.startsWith("dump(")) {
			dumpSite(findSiteID(line));
		}
		return true;
	}
	
	private String findTID(String s){
		int firstP = s.indexOf("(");
		int lastP = s.indexOf(")");

		return s.substring(firstP + 1, lastP);
	}
	
	private String findXID(String s){
		int firstP = s.indexOf("(");
		int lastP = s.indexOf(")");

		return s.substring(firstP + 1, lastP);
	}
	
	private int findSiteID(String s){
		int firstP = s.indexOf("(");
		int lastP = s.indexOf(")");

		String siteId =  s.substring(firstP + 1, lastP);
		
		return Integer.parseInt(siteId);
	}
	
	public int getTime(){
		return mTime;
	}
	
	public void callRead(String s){
		int firstP = s.indexOf("(");
		int lastP = s.indexOf(")");

		String variablesString =  s.substring(firstP + 1, lastP);
		String[] variables = variablesString.split(";");
		
		read(variables[0], variables[1]);
		
	}
	
	public void callWrite(String s){
		int firstP = s.indexOf("(");
		int lastP = s.indexOf(")");

		String variablesString =  s.substring(firstP + 1, lastP);
		String[] variables = variablesString.split(";");
		
		write(variables[0], variables[1], variables[2]);
	}
	
	public void main(String[] args) {
		
		String fileName = "commands.txt";
		parseFile(fileName);
	}
}
	