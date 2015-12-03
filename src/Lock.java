public class Lock {             //the Lock class goes beyond the general concept of lock because it can either represent a lock currently being held, or a lock which hasn't been acquired yet, 
	                            //in latter case, it serves as a container storing the information which will be used later
	Transaction t;
	Variable v;
	String type;                //either "Read" or "Write"
	Site site;
	boolean isActive;
	int value;                  //the value to be written if it's a write lock

	public Lock(Transaction t, Variable v, Site site, String type, int value,
			boolean b) {        //when a lock is created, its pointer will be stored in three objects: the variable, the site and the transaction. In this way, we can much more easily handle the cases when 
		                        //a site is failed or a transaction aborts by simply marking the lock as inactive
		this.t = t;
		this.v = v;
		this.site = site;
		this.value = value;
		isActive = b;
		this.type = type;
	}

	public void changeActive(boolean b) {        //when the lock is released, mark it as inactive, so that all other objects storing that lock will immediately get the signal that the lock is released
		isActive = b;
	}

	public boolean isActive() {
		return isActive;
	}

	public String type() {
		return type;
	}

	public Transaction transaction() {
		return t;
	}

	public Variable getVariable() {
		return v;
	}

	public Site getSite() {
		return site;
	}

	public int getValue() {
		return value;
	}
}
