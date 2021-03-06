/**
 * The Lock class goes beyond the general concept of lock because it can either
 * represent a lock currently being held, or a lock which hasn't been acquired
 * yet, in latter case, it serves as a container storing the information which
 * will be used later
 * 
 * @author Dongbo Xiao
 * @author Wenzhao Zang
 *
 */
public class Lock {
	Transaction transaction;
	Variable variable;
	String type; // either "Read" or "Write"
	Site site;
	boolean isActive;
	int value; // the value to be written if it's a write lock

	/**
	 * Constructor
	 * 
	 * when a lock is created, its pointer will be stored in three objects: the
	 * variable, the site and the transaction. In this way, we can much more
	 * easily handle the cases when a site is failed or a transaction aborts by
	 * simply marking the lock as inactive
	 * 
	 * @param transaction
	 * @param variable
	 * @param site
	 * @param type
	 * @param value
	 * @param isActive
	 */
	public Lock(Transaction transaction, Variable variable, Site site,
			String type, int value, boolean isActive) {
		this.transaction = transaction;
		this.variable = variable;
		this.site = site;
		this.value = value;
		this.isActive = isActive;
		this.type = type;
	}

	/**
	 * changeActive
	 * 
	 * when the lock is released mark it as inactive so that all other objects
	 * storing that lock will immediately get the signal that the lock is
	 * released
	 * 
	 * @param isActive
	 */
	public void changeActive(boolean isActive) {
		this.isActive = isActive;
	}

	/**
	 * isActive getter
	 * 
	 * @return isActive
	 */
	public boolean isActive() {
		return this.isActive;
	}

	/**
	 * type getter
	 * 
	 * @return type
	 */
	public String getType() {
		return this.type;
	}

	/**
	 * transaction getter
	 * 
	 * @return transaction
	 */
	public Transaction getTransaction() {
		return this.transaction;
	}

	/**
	 * variable getter
	 * 
	 * @return variable
	 */
	public Variable getVariable() {
		return this.variable;
	}

	/**
	 * site getter
	 * 
	 * @return site
	 */
	public Site getSite() {
		return this.site;
	}

	/**
	 * value getter
	 * 
	 * @return value
	 */
	public int getValue() {
		return this.value;
	}
}
