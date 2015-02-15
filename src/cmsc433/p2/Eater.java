package cmsc433.p2;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Eaters are simulation actors that have two fields: a name, and a list
 * of Food items that constitute the Eater's order.  When running, an
 * eater attempts to enter the restaurant (only successful if the
 * restaurant has a free table), place its order, and then leave the 
 * restaurant when the order is complete.
 */
public class Eater implements Runnable {
	private final String name;
	private final List<Food> order;
	private final int orderNum;    
	private static int cnt = 0;
	// Blocking queues
	private final ConcurrentHashMap<Eater, BlockingQueue<Food>> foodForEater; // Cook <--> Eater
	private final BlockingQueue<Eater> eaters; // current eaters (Eater --> Cook)
	private final BlockingQueue<Eater> eatersDoneQueue;

	/**
	 * You can feel free modify this constructor.  It must take at
	 * least the name and order but may take other parameters if you
	 * would find adding them useful.
	 */
	public Eater(String name, List<Food> order) {
		this.name = name;
		this.order = order;
		this.orderNum = ++cnt;
		foodForEater = null;
		eaters = null;
		eatersDoneQueue = null;
	}

	public Eater(String name, List<Food> order, ConcurrentHashMap<Eater, BlockingQueue<Food>> foodForEater, 
			BlockingQueue<Eater> eaters, BlockingQueue<Eater> eatersDoneQueue) {
		this.name = name;
		this.order = order;
		this.orderNum = ++cnt;
		this.foodForEater = foodForEater;
		this.eaters = eaters;
		this.eatersDoneQueue = eatersDoneQueue;
	}

	public String toString() {
		return name;
	}

	public int getOrderNum() {
		// I need this method primarily for the Validate() method
		return Integer.parseInt(name.substring(5));
	}

	public List<Food> getOrder() {
		// returns the eater's order
		return order;
	}

	/** 
	 * This method defines what an Eater does: The eater attempts to
	 * enter the restaurant (only successful if the restaurant has a
	 * free table), place its order, and then leave the restaurant
	 * when the order is complete.
	 */
	public void run() {

		// must initialize the list at the given key in the foodForEater map (which will be populated by the cook)
		// this map is a way for the cook to communicate with the eater that the eater's food is done
		foodForEater.put(this, new LinkedBlockingQueue<Food>());

		// log event that eater has now placed order
		Simulation.logEvent(SimulationEvent.eaterPlacedOrder(this, order, this.getOrderNum()));
		System.out.println(SimulationEvent.eaterPlacedOrder(this, order, this.getOrderNum()).toString());
		
		// tell the cook threads that there is a new eater ready to be processed
		// add eater to the queue of eaters waiting to be processed
		eaters.add(this); 

		// wait for all three of the foods to finish cooking
		for (int i = 0; i < 3; i++) {
			try {
				// block until one of the foods is ready (foodForEater will be populated by the cook thread)
				// once a food is ready, just pop it off and iterate again to this point
				foodForEater.get(this).take();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		// after this point, the eater will have received all of their food (order completed)
		Simulation.logEvent(SimulationEvent.eaterReceivedOrder(this, order, this.getOrderNum()));
		System.out.println(SimulationEvent.eaterReceivedOrder(this, order, this.getOrderNum()).toString());

		// immediately after that, the eater will leave the restaurant
		Simulation.logEvent(SimulationEvent.eaterLeavingRestaurant(this));
		System.out.println(SimulationEvent.eaterLeavingRestaurant(this).toString());

		// remove the eater from the foodForEater map
		foodForEater.remove(this);
		
		try {
			eatersDoneQueue.put(this);
		} catch (InterruptedException e) {
			return;
		}

	}
}