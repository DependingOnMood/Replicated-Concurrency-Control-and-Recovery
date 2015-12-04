import java.util.ArrayList;
import java.util.List;

/**
 * The Variable class is contains all the functions and variables needed for the
 * variables for the distributed database, including the locks. The lockList in
 * it lists of all the locks currently placed on this (copy of); the waitList in
 * it lists of all the locks which will be placed on this (copy of) variable,
 * currently are waiting for other locks to release.
 * 
 * @author Wenzhao Zang
 * @author Dongbo Xiao
 *
 */
public class Variable {
	String name;
	int value;
	boolean isReadyForRead;
	List<Lock> lockList;
	List<Lock> waitList;

	public Variable(String name, int value) {
		this.name = name;
		this.value = value;
		isReadyForRead = true;
		lockList = new ArrayList<Lock>();
		waitList = new ArrayList<Lock>();
	}

	/**
	 * change the value when a write operation is performed
	 * 
	 * @param value
	 */
	public void changeValue(int value) {
		this.value = value;
	}

	/**
	 * when a site recovers, mark it false for all replicated variables and mark
	 * it true for all non-replicated variables, when a write is done, mark it
	 * true
	 * 
	 * @param isReadyForRead
	 */
	public void changeReadReady(boolean isReadyForRead) {
		this.isReadyForRead = isReadyForRead;
	}

	/**
	 * variable name getter
	 * 
	 * @return name
	 */
	public String getName() {
		return name;
	}

	/**
	 * variable value getter
	 * 
	 * @return value
	 */
	public int getValue() {
		return value;
	}

	/**
	 * is ready for read
	 * 
	 * @return boolean if the value is ready for read
	 */
	public boolean isReadyForRead() {
		return isReadyForRead;
	}

	/**
	 * if the variable has write lock
	 * 
	 * when a transaction wants to read, it needs to examine if there is write
	 * lock on this variable before examination, make sure all the locks in the
	 * lock list are active (remove the locks which have been released due to
	 * the end of a transaction or the failure of a site)
	 * 
	 * @return boolean if the variable has write lock
	 */
	public boolean hasWriteLock() {

		cleanLock();

		// if there's a write lock, it must be the first and only lock in the
		// lock list
		if (hasLock() && lockList.get(0).getType().equals("Write")) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * hasLock
	 * 
	 * @return boolean if has lock
	 */
	public boolean hasLock() {
		cleanLock();
		return lockList.size() != 0;
	}

	/**
	 * clean lock
	 * 
	 * when a transaction ends or a site fails, the lock release process will be
	 * triggered from the corresponding objects, so a lock which has been
	 * released will be marked as inactive but still exist in the variable lock
	 * list, such locks should be removed whenever the variable object is
	 * visited
	 */
	public void cleanLock() {
		for (int i = 0; i < lockList.size();) {
			Lock lock = lockList.get(i);
			if (!lock.isActive()) {
				lockList.remove(i);
			} else {
				i++;
			}
		}
	}

	/**
	 * clean wait
	 * 
	 * if a lock is in the wait list and is never acquired because the
	 * transaction aborts or the site fails, it should be removed from the wait
	 * list
	 */
	public void cleanWait() {

		for (int i = 0; i < waitList.size();) {
			Lock lock = waitList.get(i);
			if (!lock.isActive()) {
				waitList.remove(i);
			} else {
				i++;
			}
		}
	}

	/**
	 * place a lock on the variable
	 * 
	 * @param lock
	 */
	public void placeLock(Lock lock) {
		lockList.add(lock);
	}

	/**
	 * lockList getter
	 * 
	 * @return LockList
	 */
	public List<Lock> getLockList() {
		cleanLock();
		return lockList;
	}

	/**
	 * check if a transaction can wait
	 * 
	 * for efficiency, there is no need to add a lock to the wait list if there
	 * exists an older transaction, so the time stamps of the transactions in
	 * the wait list should be in strict decreasing order
	 * 
	 * @param transaction
	 * @return boolean if a transaction can wait
	 */
	public boolean canWait(Transaction t) {
		cleanWait();
		return waitList.isEmpty()
				|| waitList.get(waitList.size() - 1).getTransaction().getTime() > t
						.getTime();
	}

	/**
	 * add a lock in the waitList
	 * 
	 * @param lock
	 */
	public void wait(Lock lock) {
		waitList.add(lock);
	}

	/**
	 * update
	 * 
	 * whenever a lock on this variable is released, update the lock list and
	 * check if locks in the wait list also should be moved to the active lock
	 * list
	 */
	public void update() {
		cleanLock();
		cleanWait();

		if (lockList.size() == 0 && waitList.size() > 0) {

			if (waitList.get(0).getType().equals("Write")) {
				lockList.add(waitList.get(0));
				waitList.remove(0);

			} else {

				while (waitList.size() > 0
						&& waitList.get(0).getType().equals("Read")) {
					Lock lock = waitList.get(0);
					lockList.add(lock);
					waitList.remove(0);

					System.out.print("Read by "
							+ lock.getTransaction().getName() + ", ");
					Action.print(lock.getVariable().getName(), lock.getSite()
							.siteIndex(), lock.getVariable().getValue());
				}
			}
		}
	}
}