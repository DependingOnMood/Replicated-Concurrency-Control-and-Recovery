import java.util.*;

public class Action {

	static HashMap<String, Transaction> transactionList;
	static Site[] sites;

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

	public static boolean isUnique(String name) {
		int index = Integer.parseInt(name.substring(1, name.length()));
		if (index % 2 == 0) {
			return false;
		} else {
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
			if (t.containsReadOnly(vName)) {
				System.out.print("Read ");
				Integer[] array = t.getReadOnly(vName);
				print(vName, array[1], array[0]);
			} else {
				abort(t);
			}
			// int i = 0;
			// for (; i < sites.length; i++) {
			// if (!sites[i].isFailing && sites[i].containsVariable(vName)) {
			// Variable v = sites[i].getVariable(vName);
			// if (v.isReadyForRead()) {
			// System.out.print("Read ");
			// print(v.getName(), i + 1, v.getValue());
			// break;
			// }
			// }
			// }
			// if (i == sites.length) {
			// abort(t);
			// }
		} else {
			int i = 0;
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
					} else if (v.isReadyForRead()) {
						List<Lock> lockList = v.getLockList();
						if (lockList.get(0).transaction().getTime() > t
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
			if (i == sites.length) {
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
			if (!sites[i].isFailing && sites[i].containsVariable(vName)) {
				Variable v = sites[i].getVariable(vName);
				if (!v.hasLock()) {
					Lock lock = new Lock(t, v, sites[i], "Write", value, true);
					v.placeLock(lock);
					sites[i].placeLock(lock);
					t.placeLock(lock);
					shouldAbort = false;
				} else {
					List<Lock> lockList = v.getLockList();
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

	public static void abort(Transaction t) {
		// System.out.println(t.getName() + " abort");
		end(t, false);
	}

	public static void end(Transaction t, boolean isCommited) {
		if (t == null || !transactionList.containsKey(t.getName())) {
			return;
		}
		if (isCommited) {
			t.realizeLocks();
		}
		t.nullifyLocks();
		// t.realizeWaits();
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

	public static void main(String[] args) {
		initialize();
		Parser parser = new Parser();
		String fileName = "commands.txt";
		parser.parseFile(fileName);
	}

}
