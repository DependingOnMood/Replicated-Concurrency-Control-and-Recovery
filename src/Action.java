import java.util.*;

public class Action {      //transaction manager

	static HashMap<String, Transaction> transactionList;     //active transactions
	static Site[] sites;                                     //ten sites

	public static HashMap<String, Transaction> getTransactionList() {
		return transactionList;
	}

	public static Site[] getSites() {
		return sites;
	}

	public static void print(String vName, int siteNumber, int value) {
		System.out.println(vName + ": " + value + " at site " + siteNumber);
	}

	public static void initialize() {
		transactionList = new HashMap<String, Transaction>();
		sites = new Site[10];
		for (int i = 0; i < 10; i++) {
			sites[i] = new Site(i + 1);                         //stores available copies to each site
			for (int j = 1; j <= 20; j++) {
				StringBuilder temp = new StringBuilder();
				temp.append("x");
				temp.append(j);
				if (j % 2 == 0) {
					sites[i].add(new Variable(temp.toString(), j * 10));
				} else if (i == 9 && (j == 9 || j == 19)) {
					sites[i].add(new Variable(temp.toString(), j * 10));
				} else if (((j + 1) % 10) == i + 1) {
					sites[i].add(new Variable(temp.toString(), j * 10));
				}
			}
		}
	}

	public static boolean isUnique(String name) {                 //determine whether a variable is replicated
		int index = Integer.parseInt(name.substring(1, name.length()));
		if (index % 2 == 0) {
			return false;
		} else {
			return true;
		}
	}

	public static void begin(String name, int time, boolean isReadOnly) {             //to instantiate a new transaction
		transactionList.put(name, new Transaction(name, time, isReadOnly));
	}

	public static void read(String tName, String vName) {                             //perform read operation
		Transaction t = transactionList.get(tName);
		if (t == null) {
			return;
		}
		if (t.isReadOnly) {                              //if the transaction is read-only type, simply retrieve and return the 'cache' it stores
			if (t.containsReadOnly(vName)) {
				System.out.print("Read by " + t.getName() + ", ");
				Integer[] array = t.getReadOnly(vName);
				print(vName, array[1], array[0]);
			} else {
				abort(t);             
			}
		} else {
			int i = 0;
			for (; i < sites.length; i++) {
				if (!sites[i].isFailing && sites[i].containsVariable(vName)) {      //if not read-only type, get the first working site which contains that variable
					Variable v = sites[i].getVariable(vName);                    
					if (v.isReadyForRead() && !v.hasWriteLock()) {                  //need to check if the variable is ready for read, and make sure it does not have a write lock on it
						Lock lock = new Lock(t, v, sites[i], "Read", 0, true);      //get a new lock and put it inside the lock list of that variable
						v.placeLock(lock);
						sites[i].placeLock(lock);
						t.placeLock(lock);
						System.out.print("Read by " + t.getName() + ", ");          //print out the read value
						print(v.getName(), i + 1, v.getValue());
						break;
					} else if (v.isReadyForRead()) {                                //if the variable has a write lock 
						List<Lock> lockList = v.getLockList();
						if (lockList.get(0).transaction().getTime() > t             //check if the current transaction should wait for the transaction holding the write lock
								.getTime() && v.canWait(t)) {                       //also check if all the transactions in the wait list of that variable are younger than the current transaction,
							              											//because otherwise, there is no need to wait
							Lock lock = new Lock(t, v, sites[i], "Read", 0,         //if decide to wait, put the lock (which represents an operation to be performed later) to the wait list
									true);
							v.wait(lock);
							sites[i].placeLock(lock);
							t.placeLock(lock);
						} else {
							abort(t);
						}
						break;
					}
				}
			}
			if (i == sites.length) {                                                 //does not find a working site containing the variable, abort 
				abort(t);
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
			if (!sites[i].isFailing && sites[i].containsVariable(vName)) {           //need to check if the variable is ready for read, and make sure it does not have any lock on it          
				Variable v = sites[i].getVariable(vName);
				if (!v.hasLock()) {
					Lock lock = new Lock(t, v, sites[i], "Write", value, true);      //if it does not have lock, get a new lock
					v.placeLock(lock);
					sites[i].placeLock(lock);
					t.placeLock(lock);
					shouldAbort = false;
				} else {
					List<Lock> lockList = v.getLockList();                            //if it already has a lock, decide if it should wait
					for (Lock lock : lockList) {
						if (lock.isActive()
								&& lock.transaction().getTime() < t.getTime()) {
							abort(t);
							break;
						}
					}
					if (!v.canWait(t)) {
						abort(t);
						break;
					}
					Lock lock = new Lock(t, v, sites[i], "Write", value, true);        //if decide to wait, get a new lock and put it in the wait list 
					v.wait(lock);
					sites[i].placeLock(lock);
					t.placeLock(lock);
					shouldAbort = false;
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

	public static void end(Transaction t, boolean isCommited) {                         //value of isCommited represent if it's commit or abort
		if (t == null || !transactionList.containsKey(t.getName())) {
			return;
		}
		if (isCommited) {
			System.out.println(t.getName() + " committed");
		}
		else {
			System.out.println(t.getName() + " aborted");
		}
		if (isCommited) {                                                                //difference between commit and abort is that if all the locks should be 'realized', or simply discarded
			t.realizeLocks();                    
		}
		t.nullifyLocks();
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
			System.out.println();
		}
	}

	public static void dumpVariable(String vName) {
		for (int i = 0; i < sites.length; i++) {
			sites[i].printVariable(vName);
		}
	}

	public static void main(String[] args) {
//		for (int i = 1; i <= 12; i++) {                                                     //for testing purpose
//			initialize();
//			Parser parser = new Parser();
//			StringBuilder temp = new StringBuilder();
//			temp.append("input");
//			temp.append(i);
//			temp.append(".txt");
//			String fileName = temp.toString();
//			System.out.println("******Output " + i + "******");
//			System.out.println();
//			parser.parseFile(fileName);
//		    System.out.println();
//		}
		initialize();
    	Parser parser = new Parser();
    	String fileName = "commands.txt";
		parser.parseFile(fileName);
	}

}
