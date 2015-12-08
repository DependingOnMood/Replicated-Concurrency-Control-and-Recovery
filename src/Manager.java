import java.util.HashMap;
import java.util.List;

/**
 * The Manager class is use as a Transaction Manager, it has the main methods
 * needed to control transactions in the database.
 * 
 * @author Dongbo Xiao
 * @author Wenzhao Zang
 *
 */
public class Manager {

	static HashMap<String, Transaction> transactionList; // active transactions
	static Site[] sites; // ten sites

	/**
	 * transactionList getter
	 * 
	 * @return transactionList
	 */
	public static HashMap<String, Transaction> getTransactionList() {
		return transactionList;
	}

	/**
	 * sites getter
	 * 
	 * @return sites
	 */
	public static Site[] getSites() {
		return sites;
	}

	/**
	 * print values in sites
	 * 
	 * @param vName
	 * @param siteNumber
	 * @param value
	 */
	public static void print(String vName, int siteNumber, int value) {
		System.out.println(vName + ": " + value + " at site " + siteNumber);
	}

	/**
	 * initialize the transaction list and sites
	 */
	public static void initialize() {

		transactionList = new HashMap<String, Transaction>();
		sites = new Site[10];

		for (int i = 0; i < 10; i++) {

			// stores available copies to each site
			sites[i] = new Site(i + 1);

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

	/**
	 * determine whether a variable is replicated
	 * 
	 * @param name
	 * @return boolean if it's unique
	 */
	public static boolean isUnique(String name) {

		int index = Integer.parseInt(name.substring(1, name.length()));
		if (index % 2 == 0) {
			return false;
		} else {
			return true;
		}
	}

	/**
	 * to instantiate a new transaction
	 * 
	 * @param name
	 * @param time
	 * @param isReadOnly
	 */
	public static void begin(String name, int time, boolean isReadOnly) {

		transactionList.put(name, new Transaction(name, time, isReadOnly));
	}

	/**
	 * perform read operation
	 * 
	 * @param tName
	 * @param vName
	 */
	public static void read(String tName, String vName) {
		Transaction t = transactionList.get(tName);
		if (t == null) {
			return;
		}

		// if the transaction is read-only type,
		// simply retrieve and return the 'cache' it stores
		if (t.isReadOnly) {

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

				// if not read-only type,
				// get the first working site which contains that variable
				if (!sites[i].isFailing && sites[i].containsVariable(vName)) {

					Variable v = sites[i].getVariable(vName);

					// need to check if the variable is ready for read,
					// and make sure it does not have a write lock on it
					if (v.isReadyForRead() && !v.hasWriteLock()) {

						// get a new lock and put it inside the lock list of
						// that variable
						Lock lock = new Lock(t, v, sites[i], "Read", 0, true);

						v.placeLock(lock);
						sites[i].placeLock(lock);
						t.placeLock(lock);

						// print out the read value
						System.out.print("Read by " + t.getName() + ", ");
						print(v.getName(), i + 1, v.getValue());
						break;

						// if the variable has a write lock
					} else if (v.isReadyForRead()) {
                        // first check if the transaction holding the lock is the same with the current transaction,
						// if so, print out the value of that write lock even if the transaction has not committed
						// if not, then check if the current transaction should wait for the
						// transaction holding the write lock
						// also check if all the transactions in the wait list
						// of that variable are younger than the current
						// transaction,
						// because otherwise, there is no need to wait
						// if decide to wait, put the lock (which represents an
						// operation to be performed later) to the wait list
						if (v.getWriteLock().getTransaction().getName().equals(t.getName())) {
							Lock lock = new Lock(t, v, sites[i], "Read", 0,
									true);

							v.placeLock(lock);
							sites[i].placeLock(lock);
							t.placeLock(lock);
							System.out.print("Read by " + t.getName() + ", ");
							print(v.getName(), v.getWriteLock().getSite().siteIndex(), v.getWriteLock().getValue());
						}
					    else if (v.getWriteLock().getTransaction().getTime() > t
								.getTime() && v.canWait(t)) {

							Lock lock = new Lock(t, v, sites[i], "Read", 0,
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

			// does not find a working site containing the variable, abort
			if (i == sites.length) {
				if (isUnique(vName)) {
					for (int pos = 0; pos < sites.length; pos++) {
						if (sites[pos].containsVariable(vName) && sites[pos].isFailing()) {
							Lock lock = new Lock(t, sites[pos].getVariable(vName), sites[pos], "Read", 0,
									true); 
							sites[pos].placeLock(lock);
							t.placeLock(lock);
							sites[pos].getVariable(vName).placeLock(lock);
							break;
						} 
					}
				}
				else {
					abort(t);
				}			
			}
		}
	}

	/**
	 * perform write operation
	 * 
	 * @param tName
	 * @param vName
	 * @param value
	 */
	public static void write(String tName, String vName, int value) {

		Transaction t = transactionList.get(tName);

		if (t == null) {
			return;
		}

		int i = 0;
		boolean shouldAbort = true;

		for (; i < sites.length; i++) {

			// need to check if the variable is ready for read,
			// and make sure it does not have any lock on it (except for the lock held by itself)
			if (!sites[i].isFailing && sites[i].containsVariable(vName)) {

				Variable v = sites[i].getVariable(vName);
				if (!v.hasLock()) {

					// if it does not have lock, get a new lock
					Lock lock = new Lock(t, v, sites[i], "Write", value, true);

					v.placeLock(lock);
					sites[i].placeLock(lock);
					t.placeLock(lock);
					shouldAbort = false;
				} else {
					List<Lock> lockList = v.getLockList();
					// if it already has a lock, first check if it is itself holding the lock
					int pos = 0;
					for (; pos < lockList.size(); pos++) {
						if (!lockList.get(pos).getTransaction().getName().equals(t.getName())) {
							break;
						}
					}
					// if it's not itself, decide if it should wait
					if (pos != lockList.size()) {
						for (Lock lock : lockList) {
							if (lock.getTransaction().getTime() < t
											.getTime()) {
								abort(t);
								break;
							}
						}
						if (!v.canWait(t)) {
							abort(t);
							break;
						}
					}
					
					// if it's only itself or it decides to wait,
					// get a new lock and put it in the lock list or wait list
					Lock lock = new Lock(t, v, sites[i], "Write", value, true);
					if (pos == lockList.size()) {
					    v.placeLock(lock);
					}
					else {
						v.wait(lock);	
					}
					sites[i].placeLock(lock);
					t.placeLock(lock);
					shouldAbort = false;
				}
			}
		}

		if (shouldAbort) {
			if (isUnique(vName)) {
				for (int pos = 0; pos < sites.length; pos++) {
					if (sites[pos].containsVariable(vName) && sites[pos].isFailing()) {
						Lock lock = new Lock(t, sites[pos].getVariable(vName), sites[pos], "Write", value,
								true); 
						sites[pos].placeLock(lock);
						t.placeLock(lock);
						sites[pos].getVariable(vName).placeLock(lock);
						break;
					} 
				}
			}
			else {
				abort(t);
			}
		}
	}

	/**
	 * perform abort operation, abort a transaction
	 * 
	 * @param t
	 */
	public static void abort(Transaction t) {
		end(t, false);
	}

	/**
	 * perform end operation, end a transaction
	 * 
	 * @param t
	 *            transaction
	 * @param isCommited
	 *            represent if it's commit or abort
	 */
	public static void end(Transaction t, boolean isCommited) {

		if (t == null || !transactionList.containsKey(t.getName())) {
			return;
		}

		if (isCommited) {
			System.out.println(t.getName() + " committed");
		} else {
			System.out.println(t.getName() + " aborted");
		}

		// difference between commit and abort is that if all the locks should
		// be 'realized', or simply discarded
		if (isCommited) {
			t.realizeLocks();
		}

		t.nullifyLocks();
		transactionList.remove(t.getName());
	}

	/**
	 * perform fail operation, fail a site
	 * 
	 * @param siteNumber
	 */
	public static void fail(int siteNumber) {
		Site site = sites[siteNumber - 1];
		site.fail();
	}

	/**
	 * perform recover operation, recover a site
	 * 
	 * @param siteNumber
	 */
	public static void recover(int siteNumber) {
		Site site = sites[siteNumber - 1];
		site.recover();
	}

	/**
	 * perform dump operation, dump all variables in each site
	 */
	public static void dump() {
		for (int i = 0; i < sites.length; i++) {
			sites[i].printSite();
			System.out.println();
		}
	}

	/**
	 * perform dump(xj) operation, dump a specific variable in each site
	 * 
	 * @param vName
	 */
	public static void dumpVariable(String vName) {
		for (int i = 0; i < sites.length; i++) {
			sites[i].printVariable(vName);
		}
	}
	
	/**
	 * testing 12 files together, for testing purpose
	 */
	public static void test12Files(){
		for (int i = 1; i <= 12; i++) {
			 initialize();
			
			 Parser parser = new Parser();
			 StringBuilder temp = new StringBuilder();
			
			 temp.append("input");
			 temp.append(i);
			 temp.append(".txt");
			
			 String fileName = temp.toString();
			
			 System.out.println("******Output " + i + "******");
			 System.out.println();
			 parser.parseFile(fileName);
			 System.out.println();
			 }
		
	}

	/**
	 * main execution function, default run input1.txt in res folder, run the
	 * args[0] file in res folder if have input
	 * 
	 * @param args
	 */
	public static void main(String[] args) {

		// test12Files();

		initialize();
		Parser parser = new Parser();
		String fileName = "res/input1.txt";

		if (0 < args.length) {
			fileName = "res/" + args[0];
			System.out.println("running file: " + fileName);
		} else {
			System.out.println("running default file: " + fileName);
		}

		parser.parseFile(fileName);
	}

}
