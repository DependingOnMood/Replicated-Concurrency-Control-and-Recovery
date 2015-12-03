import java.util.ArrayList;
import java.util.List;

public class Variable {
	String name;
	int value;
	boolean isReadyForRead;
	List<Lock> lockList;                        //list of all the locks currently placed on this (copy of) variable
	List<Lock> waitList;                        //list of all the locks which will be placed on this (copy of) variable, but currently are waiting for other locks to release                      

	public Variable(String name, int value) {
		this.name = name;
		this.value = value;
		isReadyForRead = true;
		lockList = new ArrayList<Lock>();
		waitList = new ArrayList<Lock>();
	}

	public void changeValue(int value) {        //when a write operation is performed
		this.value = value;
	}

	public void changeReadReady(boolean b) {    //when a site recovers, mark it false for all replicated variables and mark it true for all non-replicated variables, when a write is done, mark it true 
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

	public boolean hasWriteLock() {              //when a transaction wants to read, it needs to examine if there is write lock on this variable
		cleanLock();                             //before examination, make sure all the locks in the lock list are active (remove the locks which have been released due to the end of a transaction or the failure of a site)
		if (hasLock() && lockList.get(0).type().equals("Write")) {          //if there's a write lock, it must be the first and only lock in the lock list
			return true;
		}
		else {
			return false;
		}
	}

	public boolean hasLock() {
		cleanLock();
		return lockList.size() != 0;
	}
	
	public void cleanLock() {
		for (int i = 0; i < lockList.size(); ) {          //when a transaction ends or a site fails, the lock release process will be triggered from the corresponding objects,
			                                              //so a lock which has been released will be marked as inactive but still exist in the variable lock list, such locks should be removed 
			                                              //whenever the variable object is visited
			Lock lock = lockList.get(i);
			if (!lock.isActive()) {
				lockList.remove(i);
			}
			else {
				i++;
			}
		}
	}
	public void cleanWait() {
		for (int i = 0; i < waitList.size(); ) {           //similarly, if a lock is in the wait list and is never acquired because the transaction aborts or the site fails, it should be removed from the wait list
			Lock lock = waitList.get(i);
			if (!lock.isActive()) {
				waitList.remove(i);
			}
			else {
				i++;
			}
		}
	}

	public void placeLock(Lock lock) {
		lockList.add(lock);
	}

	public List<Lock> getLockList() {
	    cleanLock();
		return lockList;
	}

	public boolean canWait(Transaction t) {               //for efficiency, there is no need to add a lock to the wait list if there exists an older transaction, so the time stamps of the transactions in the wait list should be in strict decreasing order 
		cleanWait();
		return waitList.isEmpty()
				|| waitList.get(waitList.size() - 1).transaction().getTime() > t
						.getTime();
	}

	public void wait(Lock lock) {
		waitList.add(lock);
	}

	public void update() {                                 //whenever a lock on this variable is released, update the lock list and check if locks in the wait list should be moved to the active lock list
		cleanLock();
		cleanWait();
		if (lockList.size() == 0 && waitList.size() > 0) {
			if (waitList.get(0).type().equals("Write")) {
				lockList.add(waitList.get(0));
				waitList.remove(0);
			}
			else {
				while (waitList.size() > 0 && waitList.get(0).type().equals("Read")) {
					Lock lock = waitList.get(0);
					lockList.add(lock);
					waitList.remove(0);
					System.out.print("Read by " + lock.transaction().getName() + ", ");
					Action.print(lock.getVariable().getName(), lock.getSite().siteIndex(), lock.getVariable().getValue());
				}
			}
		}
	}
}