import java.util.ArrayList;
import java.util.List;

	
    public class Variable {
    	String name;
    	int value;
    	boolean isReadyForRead;
    	List<Lock> lockList;
    	List<Lock> waitList;
    	public Variable (String name, int value) {
    		this.name = name;
    		this.value = value;
    		isReadyForRead = true;
    		lockList = new ArrayList<Lock>();
    		waitList = new ArrayList<Lock>();
    	}
    	public void changeValue(int value) {
    		this.value = value;
    	}
    	public void changeReadReady(boolean b) {
    		isReadyForRead = b;
    	}
    	public String getName() {
    		return name;
    	}
    	public int getValue() {
    		return value;
    	}
    	public boolean isReadyForRead() {
    		return isReadyForRead;
    	}
    	public boolean hasWriteLock() {
    		for (int i = 0; i < lockList.size(); ) {
    			Lock lock = lockList.get(i);
    			if (!lock.isActive()) {
    				lockList.remove(i);
    			}
    			else if (lock.type.equals("write")) {
    				return true;
    			}
    			else {
    				i++;
    			}
    		}
    		return false;
    	}
    	public boolean hasLock() {
    		for (Lock lock: lockList) {
    			if (lock.isActive()) {
    				return true;
    			}
    		}
    		return false;
    	}
    	public void placeLock(Lock lock) {
    		lockList.add(lock);
    	}
    	public List<Lock> getLockList() {
    		return lockList;
    	}
    	public boolean canWait(Transaction t) {
    		int i = waitList.size() - 1;
    		while (waitList.size() > 0 && !waitList.get(i).isActive()) {
    			waitList.remove(i);
    			i--;
    		}
    		return waitList.isEmpty() || waitList.get(waitList.size() - 1).transaction().getTime() > t.getTime();
    	}
    	public void wait(Lock lock) {
    		waitList.add(lock);
    	}
    	public void update() {
    		for (int i = 0; i < lockList.size(); ) {
    			Lock lock = lockList.get(i);
    			if (!lock.isActive()) {
    				lockList.remove(i);
    			}
    			else {
    				i++;
    			} 
    		}
    		if (lockList.size() > 0) {
    			return;
    		}
    		while (waitList.size() > 0) {
    			if (!waitList.get(0).isActive()) {
    				waitList.remove(0);
    			}
    			else {
    				lockList.add(waitList.get(0));
    				break;
    			}
    		}
    	}
    }