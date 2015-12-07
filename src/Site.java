import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * The Site class contains all the variables and functions needed for a site in
 * a distributed database, including the locks. Each database have 10 sites.
 * 
 * @author Wenzhao Zang
 * @author Dongbo Xiao
 *
 */
public class Site {
	int index;
	boolean isFailing;
	HashMap<String, Variable> variableList; // stores the variables on the site
	List<Lock> lockTable; // stores all the locks on the variables of this site
	//List<Lock> waitForReadyReadTable; // if necessary, store all the read transactions waiting for the variable to become ready

	/**
	 * site constructor
	 * 
	 * @param index
	 */
	public Site(int index) {
		this.index = index;
		isFailing = false;
		variableList = new HashMap<String, Variable>();
		lockTable = new ArrayList<Lock>();
	}

	/**
	 * get the site index
	 * 
	 * @return site index
	 */
	public int siteIndex() {
		return index;
	}

	/**
	 * add a variable into variableList
	 * 
	 * @param variable
	 */
	public void add(Variable v) {
		variableList.put(v.getName(), v);
	}

	/**
	 * fail a site
	 * 
	 * when a site fails,
	 * mark all the locks on this site as inactive before erasing the lock table, 
	 * so that other objects will know the lock has been released
	 * also abort all the transaction which hold locks on this site
	 */
	public void fail() {
		isFailing = true;
		System.out.println("Site " + index + " failed");

		for (Lock lock : lockTable) {
			lock.changeActive(false);
			Manager.abort(Manager.getTransactionList().get(
					lock.getTransaction().getName()));
		}
		lockTable.clear();
	}

	/**
	 * recover a site
	 */
	public void recover() {
		isFailing = false;
		System.out.println("Site " + index + " recovered");
		// if the variable is not replicated, mark ready_for_read as true,
		// otherwise, mark it false
		for (Variable v : variableList.values()) {
			if (Manager.isUnique(v.getName())) {
				v.changeReadReady(true);
			} else {
				v.changeReadReady(false);
			}
		}
		//if (!lockTable.isEmpty())
	}

	/**
	 * check if the site is failing
	 * 
	 * @return boolean if the site is failing
	 */
	public boolean isFailing() {
		return isFailing;
	}

	/**
	 * check if the variable list contains a variable
	 * 
	 * @param vName
	 * @return boolean if it contains the variable
	 */
	public boolean containsVariable(String vName) {
		return variableList.containsKey(vName);
	}

	/**
	 * get the variable from the variableList
	 * 
	 * @param vName
	 * @return variable
	 */
	public Variable getVariable(String vName) {
		return variableList.get(vName);
	}

	/**
	 * add a lock into lockTable
	 * 
	 * @param lock
	 */
	public void placeLock(Lock lock) {
		lockTable.add(lock);
	}

	/**
	 * print the all variables and values in the site
	 */
	public void printSite() {
		for (int i = 1; i <= 20; i++) {
			StringBuilder temp = new StringBuilder();
			temp.append("x");
			temp.append(i);
			if (variableList.containsKey(temp.toString())) {
				Variable v = variableList.get(temp.toString());
				if (v.isReadyForRead()) {
					Manager.print(v.getName(), index, v.getValue());
				}
			}
		}
	}

	/**
	 * print a specific variable in the site
	 * 
	 * @param vName
	 */
	public void printVariable(String vName) {
		if (variableList.containsKey(vName)) {
			Variable v = variableList.get(vName);
			Manager.print(v.getName(), index, v.getValue());
		}
	}
}
