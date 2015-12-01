import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Transaction {
	String name;
	boolean isReadOnly;
	int time;
	// the locks the transaction is currently holding
	List<Lock> lockTable;
	// the locks which other transaction wants to get
	// List<Lock> waitList;
	HashMap<String, Integer[]> readOnly;

	public Transaction(String name, int time, boolean b) {
		this.name = name;
		this.isReadOnly = b;
		this.time = time;
		lockTable = new ArrayList<Lock>();
		// waitList = new ArrayList<Lock>();
		if (isReadOnly) {
			readOnly = new HashMap<String, Integer[]>();
			for (int i = 1; i <= 20; i++) {
				StringBuilder temp = new StringBuilder();
				temp.append('x');
				temp.append(i);
				String vName = temp.toString();
				Site[] sites = Action.getSites();
				for (int j = 0; j < sites.length; j++) {
					if (!sites[j].isFailing()
							&& sites[j].containsVariable(vName)) {
						readOnly.put(vName, new Integer[] {
								sites[j].getVariable(vName).getValue(), j + 1 });
						break;
					}
				}
			}
		}
	}

	public boolean containsReadOnly(String vName) {
		return readOnly.containsKey(vName);
	}

	public Integer[] getReadOnly(String vName) {
		return readOnly.get(vName);
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

	// public void addWait(Lock lock) {
	// waitList.add(lock);
	// }
	public void realizeLocks() {
		for (Lock lock : lockTable) {
			if (lock.isActive() && lock.type().equals("Write")) {
				lock.getVariable().changeValue(lock.getValue());
				lock.getVariable().changeReadReady(true);
				// System.out.println(lock.getVariable().getName() + " " +
				// lock.getValue());
			}
		}
	}

	public void nullifyLocks() {
		for (Lock lock : lockTable) {
			lock.changeActive(false);
			lock.getVariable().update();
		}
	}
	// public void realizeWaits() {
	// for (Lock lock: waitList) {
	// if (transactionList.containsKey(lock.transaction().getName())) {
	// lock.changeActive(true);
	// }
	// }
	// }
}