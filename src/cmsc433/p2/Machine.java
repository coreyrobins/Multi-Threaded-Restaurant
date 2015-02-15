package cmsc433.p2;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

/**
 * A Machine is used to make a particular Food.  Each Machine makes
 * just one kind of Food.  Each machine has a capacity: it can make
 * that many food items in parallel; if the machine is asked to
 * produce a food item beyond its capacity, the requester blocks.
 * Each food item takes at least item.cookTimeMS milliseconds to
 * produce.
 */
public class Machine {
	public final String name;
	public final Food food;
	private final int capacity;
	private final ConcurrentHashMap<Eater, BlockingQueue<Food>> cookedFood; // Cook <--> Machine
	private final Semaphore spaceAvailable; // semaphore to limit capacity

	/**
	 * The constructor takes at least the name of the machine,
	 * the Food item it makes, and its capacity.  You may extend
	 * it with other arguments, if you wish.  Notice that the
	 * constructor currently does nothing with the capacity; you
	 * must add code to make use of this field (and do whatever
	 * initialization etc. you need).
	 */
	public Machine(String name, Food food, int capacity) {
		this.name = name;
		this.food = food;
		this.capacity = capacity;
		this.spaceAvailable = new Semaphore(this.capacity);
		cookedFood = null;
	}

	public Machine(String name, Food food, int capacity, ConcurrentHashMap<Eater, BlockingQueue<Food>> cookedFood) {
		this.name = name;
		this.food = food;
		this.capacity = capacity;
		this.spaceAvailable = new Semaphore(this.capacity);
		this.cookedFood = cookedFood;
	}

	public boolean available() {
		// This method is called by the Cook thread to see if the current machine is available 
		// (meaning that there is an available permit)
		if (spaceAvailable.availablePermits() > 0) {
			return true;
		}
		return false;
	}

	/**
	 * This method is called by a Cook in order to make the Machine's
	 * food item.  You can extend this method however you like, e.g.,
	 * you can have it take extra parameters or return something other
	 * than void.  It should block if the machine is currently at full
	 * capacity.  If not, the method should return, so the Cook making
	 * the call can proceed.  You will need to implement some means to
	 * notify the calling Cook when the food item is finished.
	 */
	public void makeFood(Eater e) throws InterruptedException {
		// I created another class (MachineRunnable), which effectively does all the work of the machine
		// This method just starts a new MachineRunnable thread
		Thread t = new Thread(new MachineRunnable(this, food, cookedFood, e, spaceAvailable));
		t.start();
	}

	public String toString() {
		return name;
	}
}