package cmsc433.p2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Cooks are simulation actors that have at least one field, a name.
 * When running, a cook attempts to retrieve outstanding orders placed
 * by Eaters and process them.
 */
public class Cook implements Runnable {
	private final String name;
	private final ConcurrentHashMap<Eater, BlockingQueue<Food>> cookedFood; // Cook <--> Machine
	private final ConcurrentHashMap<Eater, BlockingQueue<Food>> foodForEater; // Cook <--> Eater
	private final BlockingQueue<Eater> eaters; // Eater --> Cook
	private HashMap<String, Machine> machines; // keep a map of the 3 machine objects
	private ArrayList<Food> standardOrder; // list to hold the standard order of the 3 Foods

	/**
	 * You can feel free modify this constructor.  It must
	 * take at least the name, but may take other parameters
	 * if you would find adding them useful. 
	 *
	 * @param name - the name of the cook
	 */
	public Cook(String name) {
		this.name = name;
		foodForEater = null;
		eaters = null;
		cookedFood = null;
	}

	public Cook(String name, ConcurrentHashMap<Eater, BlockingQueue<Food>> cookedFood,
			BlockingQueue<Eater> eaters, HashMap<String, Machine> machines,
			ConcurrentHashMap<Eater, BlockingQueue<Food>> foodForEater) {
		this.name = name;
		this.cookedFood = cookedFood;
		this.eaters = eaters;
		this.machines = machines;
		standardOrder = new ArrayList<Food>();
		this.repopulateOrder(); // populates the standardOrder list
		this.foodForEater = foodForEater;
	}

	public String toString() {
		return name;
	}

	public void repopulateOrder() {
		// just repopulates the standardOrder list with the 3 Foods
		
		// instead of calling eater.getOrder(), since the order is going to be the same for every eater,
		// I keep a local copy of the standard order (the List with all 3 foods), and remove from the local list
		// every time a cook starts cooking the food
		
		// once the cook has finished an order, he repopulates the List
		
		Food burger = new Food("burger", 500);
		standardOrder.add(burger);
		Food fries = new Food("fries", 250);
		standardOrder.add(fries);
		Food coke = new Food("coke", 100);
		standardOrder.add(coke);
	}

	/**
	 * This method executes as follows.  The cook tries to retrieve
	 * orders placed by Eaters.  For each order, a List<Food>, the
	 * cook submits each Food item in the List to an appropriate
	 * Machine, by calling makeFood().  Once all machines have
	 * produced the desired Food, the order is complete, and the Eater
	 * is notified.  The cook can then go to process the next order.
	 * If during its execution the cook is interrupted (i.e., some
	 * other thread calls the interrupt() method on it, which could
	 * raise InterruptedException if the cook is blocking), then it
	 * terminates.
	 */
	public void run() {

		Eater eater = null; // current Eater that the cook is processing
		Food toCook = null; // current (Eater's) Food that the cook is processing

		while (true) {
			try {
				// pop the next eater from the list to be processed --> block if no eaters are waiting
				eater = eaters.take();
				
				// log that the cook is processing that Eater and has received their order
				Simulation.logEvent(SimulationEvent.cookReceivedOrder(this, eater.getOrder(), eater.getOrderNum()));
				System.out.println(SimulationEvent.cookReceivedOrder(this, eater.getOrder(), eater.getOrderNum()).toString());
				
			} catch (InterruptedException e) {
				// If cook was interrupted, then it should log that it is ending, and terminate
				Simulation.logEvent(SimulationEvent.cookEnding(this));
				System.out.println(SimulationEvent.cookEnding(this).toString());
				return;
			}

			int count = 0; // index of where we are in the standardOrder list
			
			// process all 3 foods for the eater
			while (!standardOrder.isEmpty()) {

				// examine element (at index 'count') in standardOrder
				toCook = standardOrder.get(count);

				// find machine to cook that food from the machines map
				Machine machine = machines.get(toCook.toString());
				
				// if the machine is available, then tell the machine to start cooking that food
				
				// if the machine is NOT available, then we keep looking for a food that we can cook
				// on an available machine
				if (machine.available()) {
					
					// pop off that food from the local standardOrder list
					standardOrder.remove(toCook);
					
					try {
						// log that the cook has started cooking that food for the eater
						// (this is where we need the getOrderNum() method)
						Simulation.logEvent(SimulationEvent.cookStartedFood(this, toCook, eater.getOrderNum()));
						System.out.println(SimulationEvent.cookStartedFood(this, toCook, eater.getOrderNum()).toString());
						
						// before we even tell the machine to start cooking the Food, we initialize (if absent) the
						// map at key 'eater'
						cookedFood.putIfAbsent(eater, new LinkedBlockingQueue<Food>());
						
						// send food to machine to cook
						machine.makeFood(eater);
					} catch (InterruptedException e) {
						// log event that cook is terminating if interrupted
						Simulation.logEvent(SimulationEvent.cookEnding(this));
						System.out.println(SimulationEvent.cookEnding(this).toString());
						return;
					}
				}

				// just a way to keep wrapping around the standardOrder list
				if (count >= standardOrder.size() - 1) {
					count = 0;
				} else {
					count++;
				}

				// if machine for that given food is not available, continue iterating through the food list until
				// we find a machine that is available to cook one of the foods that has not been cooked yet

			}

			// Cook thread waits for all the foods to be cooked
			// did this in a for loop from 0-3 because we know there will never be more than 3 foods in the order
			for (int i = 0; i < 3; i++) {
				
				try {
					
					// wait for an item to be cooked, and once one is, pop it off the list in the cookedFood map
					Food itemRemoved = cookedFood.get(eater).take();
					
					// log event that the Cook has finished the food that it just popped
					Simulation.logEvent(SimulationEvent.cookFinishedFood(this, itemRemoved, eater.getOrderNum()));
					System.out.println(SimulationEvent.cookFinishedFood(this, itemRemoved, eater.getOrderNum()).toString());
					
					if (i == 2) {
						// once we complete the for loop, the cook has completed the order for the given eater and is ready to
						// process another one, so we log that the cook has finished cooking that order for the specific eater
						Simulation.logEvent(SimulationEvent.cookCompletedOrder(this, eater.getOrderNum()));
						System.out.println(SimulationEvent.cookCompletedOrder(this, eater.getOrderNum()).toString());
					}
					
					// add this food to the foodForEater list so that the eater knows that one of the foods is ready
					foodForEater.get(eater).add(itemRemoved);
					
				} catch (InterruptedException e) {
					// again, log it if the cook is interrupted, because it will terminate
					Simulation.logEvent(SimulationEvent.cookEnding(this));
					System.out.println(SimulationEvent.cookEnding(this));
					return;
				}
			}

			// now we must call this to repopulate the standardOrder list (as explained in the method declaration)
			this.repopulateOrder();

		}

	}
}