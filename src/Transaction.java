import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Transaction {
	String name;
	boolean isReadOnly;
	int time;
	List<Lock> lockTable;                              //the locks the transaction is currently holding
	HashMap<String, Integer[]> readOnly;               //if it's read-only type, stores all the 'caches'

	public Transaction(String name, int time, boolean b) {
		this.name = name;
		this.isReadOnly = b;
		this.time = time;
		lockTable = new ArrayList<Lock>();
		if (isReadOnly) {                              //get the values of the variables at this particular point, according to multiversion concurrency control
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

	public void realizeLocks() {                              //when a site commits, perform write operation if there's any
		for (Lock lock : lockTable) {
			if (lock.isActive() && lock.getType().equals("Write")) {
				lock.getVariable().changeValue(lock.getValue());
				lock.getVariable().changeReadReady(true);     //mark all the variables as ready_for_read
			}
		}
	}

	public void nullifyLocks() {                              //when a site ends (whether commit or abort), release all the locks by marking them as inactive
		for (Lock lock : lockTable) {
			lock.changeActive(false);
			lock.getVariable().update();
		}
	}
}