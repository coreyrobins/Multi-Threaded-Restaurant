package cmsc433.p2;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

public class MachineRunnable implements Runnable {

	public final Food food; // the food that this machine cooks
	private final ConcurrentHashMap<Eater, BlockingQueue<Food>> cookedFood; // Cook <--> Machine
	private final Eater eater; // the current eater
	private final Semaphore spaceAvailable; // same semaphore that was initialized in the Machine class
	private final Machine machine; // pass the machine object, because we are no longer in the Machine class

	public MachineRunnable(Machine machine, Food food, ConcurrentHashMap<Eater, BlockingQueue<Food>> cookedFood, 
			Eater eater, Semaphore spaceAvailable) {
		this.machine = machine;
		this.food = food;
		this.cookedFood = cookedFood;
		this.eater = eater;
		this.spaceAvailable = spaceAvailable;
	}

	public void run() {
		try {
			spaceAvailable.acquire(); // acquire a spot on the machine (the semaphore)
		} catch (InterruptedException e) {
			return;
		}
		
		// log the event that the machine has started cooking the food
		Simulation.logEvent(SimulationEvent.machineCookingFood(machine, food));
		System.out.println(SimulationEvent.machineCookingFood(machine, food).toString());
		
		try {
			// "cook" the food for the specific amount of time
			Thread.sleep(food.cookTimeMS);

			// at this point we know that the machine has finished cooking the food, so we log that event
			Simulation.logEvent(SimulationEvent.machineDoneFood(machine, food));
			System.out.println(SimulationEvent.machineDoneFood(machine, food).toString());
			
			// add the food to the list of foods that have been cooked (in the map for the eater) so that the Cook
			// knows that this food has been cooked, and can then communicate that to the eater
			cookedFood.get(eater).add(food);
			
		} catch (InterruptedException e) {
			return;
		}
		
		// finally, we release the permit that we had on the semaphore, meaning that the number of available "spots"
		// on the machine has now been incremented, meaning that another cook can cook another (of this food)
		// on this machine
		spaceAvailable.release();

	}

}
