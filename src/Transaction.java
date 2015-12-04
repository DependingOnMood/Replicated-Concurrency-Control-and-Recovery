import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * The transaction class is contains all the functions and variables needed for
 * the transaction for the distributed database, including locks.
 *
 * @author Wenzhao Zhang
 * @author Dongbo Xiao
 *
 */
public class Transaction {
	String name;
	boolean isReadOnly;
	int time;
	List<Lock> lockTable; // the locks the transaction is currently holding
	HashMap<String, Integer[]> readOnly; // stores all 'caches' for read-only

	/**
	 * transaction constructor
	 * 
	 * @param name
	 * @param time
	 * @param b
	 */
	public Transaction(String name, int time, boolean b) {
		this.name = name;
		this.isReadOnly = b;
		this.time = time;
		lockTable = new ArrayList<Lock>();
		
		//get the values of the variables at this particular point,
		// according to multiversion concurrency control
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

	/**
	 * check if contains Read Only
	 * @param vName
	 * @return boolean if contains Read Only
	 */
	public boolean containsReadOnly(String vName) {
		return readOnly.containsKey(vName);
	}

	/**
	 * get readOnly
	 * 
	 * @param vName
	 * @return
	 */
	public Integer[] getReadOnly(String vName) {
		return readOnly.get(vName);
	}

	/**
	 * name getter
	 * 
	 * @return transaction name
	 */
	public String getName() {
		return name;
	}

	/**
	 * add a lock in lockTable
	 * 
	 * @param lock
	 */
	public void placeLock(Lock lock) {
		lockTable.add(lock);
	}

	/**
	 * time getter
	 * 
	 * @return time
	 */
	public int getTime() {
		return time;
	}

	/**
	 * when a site commits, perform write operation if there's any
	 */
	public void realizeLocks() {
		
		for (Lock lock : lockTable) {
			if (lock.isActive() && lock.getType().equals("Write")) {
				
				lock.getVariable().changeValue(lock.getValue());
				
				//mark all the variables as ready_for_read
				lock.getVariable().changeReadReady(true);
			}
		}
	}

	/**
	 * when a site ends (whether commit or abort),
	 * release all the locks by marking them as inactive
	 */
	public void nullifyLocks() {
		for (Lock lock : lockTable) {
			lock.changeActive(false);
			lock.getVariable().update();
		}
	}
}