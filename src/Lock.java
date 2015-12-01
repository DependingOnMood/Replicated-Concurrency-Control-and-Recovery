public class Lock {
	Transaction t;
	Variable v;
	String type;
	Site site;
	boolean isActive;
	int value;

	public Lock(Transaction t, Variable v, Site site, String type, int value,
			boolean b) {
		this.t = t;
		this.v = v;
		this.site = site;
		this.value = value;
		isActive = b;
		this.type = type;
	}

	public void changeActive(boolean b) {
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
