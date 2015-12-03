import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Site {
	int index;
	boolean isFailing;
	HashMap<String, Variable> variableList;            //stores the variables on the site
	List<Lock> lockTable;                              //stores all the locks on the variables of this site

	public Site(int index) {
		this.index = index;
		isFailing = false;
		variableList = new HashMap<String, Variable>();
		lockTable = new ArrayList<Lock>();
	}

	public int siteIndex() {
		return index;
	}

	public void add(Variable v) {
		variableList.put(v.getName(), v);
	}

	public void fail() {
		isFailing = true;
		System.out.println("Site " + index + " failed");
		for (Lock lock : lockTable) {                          //when a site fails, mark all the locks on this site as inactive before erasing the lock table, 
			                                                   //so that other objects will know the lock has been released
			lock.changeActive(false);
			Action.abort(Action.getTransactionList().get(      //abort all the transaction which hold locks on this site
					lock.getTransaction().getName()));
		}
		lockTable.clear();
	}

	public void recover() {
		isFailing = false;
		System.out.println("Site " + index + " recovered");
		for (Variable v : variableList.values()) {             //if the variable is not replicated, mark ready_for_read as true, otherwise, mark it false
			if (Action.isUnique(v.getName())) {
				v.changeReadReady(true);
			} else {
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
					Action.print(v.getName(), index, v.getValue());
				}
			}
		}
	}

	public void printVariable(String vName) {
		if (variableList.containsKey(vName)) {
			Variable v = variableList.get(vName);
			Action.print(v.getName(), index, v.getValue());
		}
	}
}
