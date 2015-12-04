import java.util.HashMap;
import java.util.List;

/**
 * The Action class is use as a Transaction Manager, it has the main methods
 * needed to control transactions in the database.
 * 
 * @author Dongbo Xiao
 * @author Wenzhao Zang
 *
 */
public class Action {

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

						List<Lock> lockList = v.getLockList();

						// check if the current transaction should wait for the
						// transaction holding the write lock
						// also check if all the transactions in the wait list
						// of that variable are younger than the current
						// transaction,
						// because otherwise, there is no need to wait
						// if decide to wait, put the lock (which represents an
						// operation to be performed later) to the wait list
						if (lockList.get(0).getTransaction().getTime() > t
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
				abort(t);
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
			// and make sure it does not have any lock on it
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

					// if it already has a lock, decide if it should wait
					List<Lock> lockList = v.getLockList();

					for (Lock lock : lockList) {
						if (lock.isActive()
								&& lock.getTransaction().getTime() < t
										.getTime()) {
							abort(t);
							break;
						}
					}

					if (!v.canWait(t)) {
						abort(t);
						break;
					}

					// if decide to wait,
					// get a new lock and put it in the wait list
					Lock lock = new Lock(t, v, sites[i], "Write", value, true);
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
	 * main execution function
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		// for testing purpose
		// for (int i = 1; i <= 12; i++) {
		// initialize();
		//
		// Parser parser = new Parser();
		// StringBuilder temp = new StringBuilder();
		//
		// temp.append("input");
		// temp.append(i);
		// temp.append(".txt");
		//
		// String fileName = temp.toString();
		//
		// System.out.println("******Output " + i + "******");
		// System.out.println();
		// parser.parseFile(fileName);
		// System.out.println();
		// }

		initialize();
		Parser parser = new Parser();
		String fileName = "commands.txt";
		parser.parseFile(fileName);
	}

}
