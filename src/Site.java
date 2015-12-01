import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


   public class Site {
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
    			Action.abort(Action.getTransactionList().get(lock.transaction().getName()));
    		}
    		lockTable.clear();
    	}
    	public void recover () {
    		isFailing = false;
    		for (Variable v: variableList.values()) {
    			if (Action.isUnique(v.getName())) {
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
    