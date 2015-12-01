import java.util.*;
import java.io.BufferedReader;
import java.io.FileReader;

public class Database {
	//global variables
	static HashMap<String, Transaction> transactionList;
	static Site[] sites;
	
    public static class Variable {
    	String name;
    	int value;
    	boolean isReadyForRead;
    	List<Lock> lockList;
    	public Variable (String name, int value) {
    		this.name = name;
    		this.value = value;
    		isReadyForRead = true;
    		lockList = new ArrayList<Lock>();
    	}
    	public void changeValue(int value) {
    		this.value = value;
    	}
    	public void changeReadReady(boolean b) {
    		isReadyForRead = b;
    	}
    	public String getName() {
    		return name;
    	}
    	public int getValue() {
    		return value;
    	}
    	public boolean isReadyForRead() {
    		return isReadyForRead;
    	}
    	public boolean hasWriteLock() {
    		for (Lock lock: lockList) {
    			if (lock.isActive() && lock.type.equals("write")) {
    				return true;
    			}
    		}
    		return false;
    	}
    	public boolean hasLock() {
    		for (Lock lock: lockList) {
    			if (lock.isActive()) {
    				return true;
    			}
    		}
    		return false;
    	}
    	public void placeLock(Lock lock) {
    		lockList.add(lock);
    	}
    	public Lock getLock() {
    		return lockList.get(0);
    	}
    	public boolean canPlaceWriteLock(Transaction writeT, int index) {
    		for (Lock lock: lockList) {
    			if (lock.isActive() && lock.type.equals("Read") && lock.transaction().getTime() > writeT.getTime()) {
    				continue;
    			}
    			else {
    				return false;
    			}
    		}
    		Lock lock = new Lock(writeT, this, sites[index], "Write", 0, true);
			this.placeLock(lock);
			sites[index].placeLock(lock);
			writeT.placeLock(lock);
    		lockList.add(lock);
    		return true;
    	}
    }
    
    public static class Site {
    	int index;
    	boolean isFailing;
    	HashMap<String, Variable> variableList;
    	List<Lock> lockTable;
    	public Site (int index) {
    		this.index = index;
    		isFailing = false;
    		variableList = new HashMap<String, Variable>();
    		lockTable = new ArrayList<Lock>();
    	}
    	public int siteIndex() {
    		return index;
    	}
    	public void add (Variable v) {
    		variableList.put(v.getName(), v);
    	}
    	public void fail () {
    		isFailing = true;
    		for (Lock lock: lockTable) {
    			lock.changeActive(false);
    			if(transactionList.containsKey(lock.transaction().getName())) {
    				abort(transactionList.get(lock.transaction().getName()));
    			}
    		}
    		lockTable.clear();
    	}
    	public void recover () {
    		isFailing = false;
    		for (Variable v: variableList.values()) {
    			if (isUnique(v.getName())) {
    				v.changeReadReady(true);
    			}
    			else {
    				v.changeReadReady(false);
    			}	
    		}
    	}
    	public boolean isFailing() {
    		return isFailing;
    	}
    	public boolean containsVariable(String vName) {
    		return variableList.containsKey(vName);
    	}
    	public Variable getVariable(String vName) {
    		return variableList.get(vName);
    	}
    	public void placeLock(Lock lock) {
    		lockTable.add(lock); 
    	}
    	public void printSite() {
    		for (int i = 1; i <= 20; i++) {
    			StringBuilder temp = new StringBuilder();
    			temp.append("x");
    			temp.append(i);
    			if (variableList.containsKey(temp.toString())) {
    				Variable v = variableList.get(temp.toString());
    				if (v.isReadyForRead()) {
                   	 print(v.getName(), index, v.getValue());
                    }
    			}      
    		}
    	}
    	public void printVariable(String vName) {
    		if (variableList.containsKey(vName)) {
    			Variable v = variableList.get(vName);
    			print(v.getName(), index, v.getValue());
    		}
    	}
    }
    
    public static class Transaction {
    	String name;
    	boolean isReadOnly;
    	int time;
    	//the locks the transaction is currently holding
    	List<Lock> lockTable;
    	//the locks which other transaction wants to get
    	List<Lock> waitList;
    	public Transaction(String name, int time, boolean b) {
    		this.name = name;
    		this.isReadOnly = b;
    		this.time = time;
    		lockTable = new ArrayList<Lock>();
    		waitList = new ArrayList<Lock>();
    	}
    	public String getName() {
    		return name;
    	}
    	public void placeLock(Lock lock) {
    		lockTable.add(lock); 
    	}
    	public int getTime() {
    		return time;
    	}
    	public void addWait(Lock lock) {
    		waitList.add(lock);
    	}
    	public void realizeLocks() {
    		for (Lock lock: lockTable) {
    			if (lock.isActive() && lock.type().equals("Write")) {
    				lock.getVariable().changeValue(lock.value);
    				lock.getVariable().changeReadReady(true);
    			}
    		}
    	}
    	public void nullifyLocks() {
    		for (Lock lock: lockTable) {
    			lock.changeActive(false);
    		}
    	}
    	public void realizeWaits() {
    		for (Lock lock: waitList) {
    			if (transactionList.containsKey(lock.transaction().getName())) {
    				lock.changeActive(true);
    			}
    		}
    	}
    }
    
    public static class Lock {
    	Transaction t;
    	Variable v;
    	String type;
    	Site site;
    	boolean isActive;
    	int value;
    	public Lock(Transaction t, Variable v, Site site, String type, int value, boolean b) {
    		this.t = t;
    		this.v = v;
    		this.site = site;
    		this.value = value;
    		isActive = b;
    		this.type = type;
    	}
    	public void changeActive(boolean b) {
    		isActive = b;
    	}
    	public boolean isActive() {
    		return isActive;
    	}
    	public String type() {
    		return type;
    	}
    	public Transaction transaction() {
    		return t;
    	}
    	public Variable getVariable() {
    		return v;
    	}
    	public Site getSite() {
    		return site;
    	}
    	public int getValue() {
    		return value;
    	}
    }
    
    public static void print(String vName, int siteNumber, int value) {
    	System.out.println(vName + ": " + value + " at site " + siteNumber);
    }
    
    public static void initialize() {
		transactionList = new HashMap<String, Transaction>();
		sites = new Site[10];
    	for (int i = 0; i < 10; i++) {
    		sites[i] = new Site(i + 1);
    		for (int j = 1; j <= 20; j++) {
    			StringBuilder temp = new StringBuilder();
    			temp.append("x");
    			temp.append(j);
    			if (j % 2 == 0) {
    				sites[i].add(new Variable(temp.toString(), j * 10));
    			}
    			else if (i == 9 && (j == 9 || j == 19)) {
    				sites[i].add(new Variable(temp.toString(), j * 10));
    			}
    			else if (((j + 1) % 10) == i + 1){
    				sites[i].add(new Variable(temp.toString(), j * 10));
    			}
    		}
    	}
	}
    
    public static boolean isUnique(String name) {
    	int index = Integer.parseInt(name.substring(1, name.length()));
    	if (index % 2 == 0) {
    		return false;
    	}
    	else {
    		return true;
    	}
    }
    
    public static void begin(String name, int time, boolean isReadOnly) {
    	transactionList.put(name, new Transaction(name, time, isReadOnly));
    }
    
    public static void read(String tName, String vName) {
    	Transaction t = transactionList.get(tName);
    	if (t == null) {
    		return;
    	}
    	if (t.isReadOnly) {
    		int i = 0;
    		for (; i < sites.length; i++) {
    			if (!sites[i].isFailing && sites[i].containsVariable(vName)) {
    				Variable v = sites[i].getVariable(vName);
    				if (v.isReadyForRead()) {
    					System.out.print("Read ");
    					print(v.getName(), i + 1, v.getValue());
    					break;
    				}
    			}
    		}
    		if (i == sites.length) {
    			abort(t);
    		}
    	}
    	else {
    		int i = 0;
    		Lock canWait = null;
    		for (; i < sites.length; i++) {
    			if (!sites[i].isFailing && sites[i].containsVariable(vName)) {
    				Variable v = sites[i].getVariable(vName);
    				if (v.isReadyForRead() && !v.hasWriteLock()) {
    					Lock lock = new Lock(t, v, sites[i], "Read", 0, true);
    					v.placeLock(lock);
    					sites[i].placeLock(lock);
    					t.placeLock(lock);
    					System.out.print("Read ");
    					print(v.getName(), i + 1, v.getValue());
    					break;
    				}
    				else if (v.isReadyForRead()) {
    					Lock currentLock = v.getLock();
    					if (currentLock.transaction().getTime() > t.getTime()) {
    						canWait = currentLock;
    					}
    				} 
    			}
    		}
    		if (i == sites.length) {
    			if (canWait == null) {
    				abort(t);
    			}
    			else {
    				canWait.transaction().addWait(new Lock(t, canWait.getVariable(), canWait.getSite(), "Read", 0, false));
    			}
    		}
    	}
    }
    
    public static void write(String tName, String vName, int value) {
    	Transaction t = transactionList.get(tName);
    	if (t == null) {
    		return;
    	}
    		int i = 0;
    		boolean shouldAbort = true;
    		for (; i < sites.length; i++) {
    			if (!sites[i].isFailing && sites[i].containsVariable(vName)) {
    				Variable v = sites[i].getVariable(vName);
    				if (!v.hasLock()) {
    					Lock lock = new Lock(t, v, sites[i], "Write", value, true);
    					v.placeLock(lock);
    					sites[i].placeLock(lock);
    					t.placeLock(lock);
    					shouldAbort = false;
    				}
    				else {
    					if (!v.canPlaceWriteLock(t, i)){
    						abort(t);
    						break;
    					}
    				} 
    			}
    		}
    		if (shouldAbort) {
    			abort(t);
    		}
    }
    
    public static void abort(Transaction t) {
    	end(t, false);
    }
    
    public static void end(Transaction t, boolean isCommited) {
    	if (isCommited) {
    		t.realizeLocks();
    	}
    	t.nullifyLocks();
    	t.realizeWaits();
    	transactionList.remove(t.getName());
    }
    
    public static void fail(int siteNumber) {
    	Site site = sites[siteNumber - 1];
    	site.fail();
    }
    
    public static void recover(int siteNumber) {
    	Site site = sites[siteNumber - 1];
    	site.recover();
    }
    
    public static void dump() {
    	for (int i = 0; i < sites.length; i++) {
    		sites[i].printSite();
    	}
    }
    
    public static void dumpVariable(String vName) {
    	for (int i = 0; i < sites.length; i++) {
    		sites[i].printVariable(vName);
    	}
    }
    
    public static class Parser {	
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
    			begin(findTID(line), mTime, false);
    		} else if (line.startsWith("beginRO(")) {
    			begin(findTID(line), mTime, true);
    		} else if (line.contains("R(")) {
    			callRead(line);
    		} else if (line.contains("W(")) {
    			callWrite(line);
    		} else if (line.startsWith("end(")) {
    			if (transactionList.containsKey(findTID(line))) {
    				end(transactionList.get(findTID(line)), true);
    			}	
    		} else if (line.contains("fail(")) {
    			fail(findSiteID(line));
    		} else if (line.contains("recover(")) {
    			recover(findSiteID(line));
    		} else if (line.startsWith("dump()")) {
    			dump();
    		} else if (line.startsWith("dump(x")) {
    			dumpVariable(findXID(line));
    		} else if (line.startsWith("dump(")) {
    			sites[findSiteID(line) - 1].printSite();
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
    		String[] variables = variablesString.split(",");
    		
    		read(variables[0], variables[1]);
    		
    	}
    	
    	public void callWrite(String s){
    		int firstP = s.indexOf("(");
    		int lastP = s.indexOf(")");

    		String variablesString =  s.substring(firstP + 1, lastP);
    		String[] variables = variablesString.split(",");
    		
    		write(variables[0], variables[1], Integer.parseInt(variables[2]));
    	}
    }
    	

    	
    public static void main(String[] args) {
    	initialize();
    	Parser parser = new Parser();
    	String fileName = "commands.txt";
		parser.parseFile(fileName);
    }
    
 }

