package cmsc433.p2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
/**
 * Simulation is the main class used to run the simulation.  You may
 * add any fields (static or instance) or any methods you wish.
 */
public class Simulation {

	// the static list of events
	private static List<SimulationEvent> log = new CopyOnWriteArrayList<SimulationEvent>();

	// we suggest you implement this for use by other classes in
	// the simulation to log events
	public static void logEvent(SimulationEvent event){
		log.add(event);
	}

	/**
	 * Function responsible for performing the simulation. Returns a List of
	 * SimulationEvent objects, constructed any way you see fit. This List will
	 * be validated by a call to Validate.validateSimulation. This method is called
	 * from Simulation.main(). We should be able to test your code by only calling
	 * runSimulation.
	 *
	 * @param numEaters the number of eaters wanting to enter the restaurant
	 * @param numCooks the number of cooks in the simulation
	 * @param numTables the number of tables in the restaurant (i.e. restaurant capacity)
	 * @param machineCapacity the capacity of all machines in the restaurant 
	 */
	public static List<SimulationEvent> runSimulation(int numEaters, int numCooks, int numTables, int machineCapacity){

		log.clear();
		
		if (numEaters <= 0 || numCooks <= 0 || numTables <= 0 || machineCapacity <= 0) {
			System.err.println("Incorrect arguments.");
			System.exit(1);
		}

		// log event that the simulation has BEGUN!
		logEvent(SimulationEvent.startSimulation(numEaters, numCooks, numTables, machineCapacity));
		System.out.println(SimulationEvent.startSimulation(numEaters, numCooks, numTables, machineCapacity).toString());

		// map for communication between Cook and Machine
		ConcurrentHashMap<Eater, BlockingQueue<Food>> cookedFood = new ConcurrentHashMap<Eater, BlockingQueue<Food>>();

		// map for communication between Cook and Eater
		ConcurrentHashMap<Eater, BlockingQueue<Food>> foodForEater = new ConcurrentHashMap<Eater, BlockingQueue<Food>>();

		// BlockingQueue for communication between Eaters and Cooks
		BlockingQueue<Eater> eaters = new LinkedBlockingQueue<Eater>();

		// Map of machines (no need to be ConcurrentHashMap because no writes are occurring except in main thread)
		// This map gets passed to the Cooks so that they can check which machines are available at a given time
		HashMap<String, Machine> machines = new HashMap<String, Machine>();

		// this is the standard list of foods for each eater that gets passed to each eater thread as it is created
		List<Food> order = new ArrayList<Food>();

		// this is a list of all of the Cook threads
		List<Thread> cooks = new ArrayList<Thread>();

		// this is a list of all of the Eater threads
		List<Thread> eaterThreadsList = new ArrayList<Thread>();

		BlockingQueue<Eater> eatersDoneQueue = new LinkedBlockingQueue<Eater>(numTables);

		// Initialize the 3 Food objects, and add them to the 'order' list to pass to each Eater thread
		Food burger = new Food("burger", 500);
		order.add(burger);
		Food fries = new Food("fries", 250);
		order.add(fries);
		Food coke = new Food("coke", 100);
		order.add(coke);

		// Start up the Grill machine (and log it)
		Machine grill = new Machine("Grill", burger, machineCapacity, cookedFood);
		machines.put("burger", grill);
		Simulation.logEvent(SimulationEvent.machineStarting(grill, grill.food, machineCapacity));
		System.out.println(SimulationEvent.machineStarting(grill, grill.food, machineCapacity).toString());

		// Start up the Frier machine (and log it)
		Machine frier = new Machine("Frier", fries, machineCapacity, cookedFood);
		machines.put("fries", frier);
		Simulation.logEvent(SimulationEvent.machineStarting(frier, frier.food, machineCapacity));
		System.out.println(SimulationEvent.machineStarting(frier, frier.food, machineCapacity).toString());

		// Start up the Soda machine (and log it)
		Machine soda = new Machine("Soda Fountain", coke, machineCapacity, cookedFood);
		machines.put("coke", soda);
		Simulation.logEvent(SimulationEvent.machineStarting(soda, soda.food, machineCapacity));
		System.out.println(SimulationEvent.machineStarting(soda, soda.food, machineCapacity).toString());

		// start cook threads
		for (int i = 0; i < numCooks; i++) {

			// initialize the Cook object
			Cook cook = new Cook("Cook"+i, cookedFood, eaters, machines, foodForEater);

			// log that the cook has started
			logEvent(SimulationEvent.cookStarting(cook));
			System.out.println(SimulationEvent.cookStarting(cook).toString());

			// start the thread
			Thread t = new Thread(cook);
			t.start();
			cooks.add(t);
		}

		// start eater threads

		// if the number of eaters specified is less than or equal to the restaurant capacity (numTables),
		// then there is no need to wait, so we just start up all of the Eater threads at once
		if (numEaters <= numTables) {
			for (int i = 0; i < numEaters; i++) {
				Eater eater = new Eater("Eater"+i, order, foodForEater, eaters, eatersDoneQueue); // create the Eater object
				
				Simulation.logEvent(SimulationEvent.eaterStarting(eater));
				System.out.println(SimulationEvent.eaterStarting(eater));
				
				Simulation.logEvent(SimulationEvent.eaterEnteredRestaurant(eater)); // log event that eater is entering
				System.out.println(SimulationEvent.eaterEnteredRestaurant(eater));
				
				Thread t = new Thread(eater); // create the Thread object
				t.start(); // start the Thread
				eaterThreadsList.add(t); // add the thread to the list of eater threads
			}
		} else {

			// However, if this is not the case, then only start as many Eater threads as there are tables
			int i;
			for (i = 0; i < numTables; i++) {
				Eater eater = new Eater("Eater"+i, order, foodForEater, eaters, eatersDoneQueue);
				Thread t = new Thread(eater);
				
				Simulation.logEvent(SimulationEvent.eaterStarting(eater));
				System.out.println(SimulationEvent.eaterStarting(eater));
				
				Simulation.logEvent(SimulationEvent.eaterEnteredRestaurant(eater)); // log event that eater is entering
				System.out.println(SimulationEvent.eaterEnteredRestaurant(eater));
				
				t.start();
				eaterThreadsList.add(t);
			}

			int eaterThreads = i; // keep track of how many eater threads have been started

			// while not all eater threads have been started...
			while (eaterThreads < numEaters) {

				// if there are enough open tables, then start up another Eater thread
				// otherwise, keep iterating until this condition is met

				try {
					eatersDoneQueue.take();
					
					Eater eater = new Eater("Eater"+eaterThreads, order, foodForEater, eaters, eatersDoneQueue);
					Thread t = new Thread(eater);
					
					Simulation.logEvent(SimulationEvent.eaterStarting(eater));
					System.out.println(SimulationEvent.eaterStarting(eater));
					
					Simulation.logEvent(SimulationEvent.eaterEnteredRestaurant(eater)); // log event that eater is entering
					System.out.println(SimulationEvent.eaterEnteredRestaurant(eater));
					
					t.start();
					eaterThreadsList.add(t);
					eaterThreads++;
					
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

			}

		}

		// wait for all eater threads to finish before we stop the cooks and the machines
		for (Thread t : eaterThreadsList) {
			try {
				t.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		// the machines are not threads (the MachineRunnable threads must have terminated at this point anyway),
		// so we just log that each machine has ended
		for (Machine m : machines.values()) {
			logEvent(SimulationEvent.machineEnding(m));
			System.out.println(SimulationEvent.machineEnding(m).toString());
		}
		
		// once all of the eaters have finished, then we interrupt the cook threads to tell them to terminate
		for (Thread t : cooks) {
			t.interrupt();
		}

		for (Thread t : cooks) {
			try {
				t.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		// finally, log that the simulation has ended
		logEvent(SimulationEvent.endSimulation());
		System.out.println(SimulationEvent.endSimulation().toString());

		// Add all simulation code here, returning a List of SimulationEvent objects
		// You may construct the list in any way you see fit. We recommend using
		// the Simulation.logEvent function.
		return log;
	}

	/**
	 * Entry point for the simulation. All simulation code, however, should be
	 * in runSimulation, so that we can test your simulation by only calling
	 * runSimulation() then Validate.validateSimulation. This means that most
	 * code from your original Simulation.main should probably now be in 
	 * Simulation.runSimulation.
	 *
	 * @param args the command-line arguments for the simulation.  There
	 * should be exactly four arguments: the first is the number of eaters,
	 * the second is the number of cooks, the third is the number of tables
	 * in the restaurant, and the fourth is the number of items each cooking
	 * machine can make at the same time.  
	 */
	public static void main(String args[]) throws InterruptedException {
		// Parameters to the simulation
		if (args.length != 4) {
			System.err.println("usage: java Simulation <#eaters> <#cooks> <#tables> <capacity>");
			System.exit(1);
		}
		int numEaters = new Integer(args[0]).intValue();
		int numCooks = new Integer(args[1]).intValue();
		int numTables = new Integer(args[2]).intValue();
		int machineCapacity = new Integer(args[3]).intValue();

		List<SimulationEvent> simEvents;

		// Run the simulation
		simEvents = runSimulation(numEaters, numCooks, numTables, machineCapacity);

		// Validate the simulation
		Validate.validateSimulation(simEvents);
	}
}
