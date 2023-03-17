
import java.util.ArrayList;
import java.util.concurrent.Semaphore;
import java.util.LinkedList;
import java.util.Queue;

// DMV simulation
public class Project2 {

	// Declare and initialize all other resources (values)
	// Number of customers to enter DMV
	private static int number_of_customers = 20;
	// Number of agents in DMV
	private static int number_of_agents = 2;
	// List to store all sequence numbers and associated customer numbers
	private static ArrayList<Integer> customer_numbers = new ArrayList<>();
	// Customer number shared by Customer to then be used by Info Desk
	private static int customer_num = 0;
	// Array to keep track of agent number serving certain customer
	private static int[] my_agent = new int[number_of_customers];
	// Queue to hold customers in agent line after being called up by Announcer
	private static Queue<Integer> customers_in_line = new LinkedList<>();
	
	// Declare all semaphores.
	// (Many of these were used by multiple classes, but I wanted to categorize them
	// based on their central purpose)
	// Used for Customer
	private static Semaphore customer_enters = new Semaphore(0, true);
	private static Semaphore print_sequence_num = new Semaphore(0, true);
	private static Semaphore using_sequence_num = new Semaphore(1, true);
	private static Semaphore customer_ready = new Semaphore(0, true);
	// Used for Info Desk
	private static Semaphore leave_info_desk = new Semaphore(0, true);
	private static Semaphore line_for_info_desk = new Semaphore(1, true);
	private static Semaphore new_customer = new Semaphore(1, true);
	private static Semaphore can_enter_waiting_area[] = new Semaphore[number_of_customers];
	// Used for Announcer
	private static Semaphore customer_in_waiting_area = new Semaphore(0, true);
	private static Semaphore agent_line = new Semaphore(4, true);
	private static Semaphore number_called[] = new Semaphore[number_of_customers];
	private static Semaphore calling_customers = new Semaphore(1, true);
	private static Semaphore customers_leaves_line[] = new Semaphore[number_of_customers];
	// Used for Agent
	private static Semaphore agent_ready = new Semaphore(2, true);
	private static Semaphore wait_agent = new Semaphore(0, true);
	private static Semaphore print_agent_num = new Semaphore(1, true);
	private static Semaphore go_to_agent = new Semaphore(2, true);
	private static Semaphore leave_agent = new Semaphore(0, true);
	private static Semaphore customer_is_served[] = new Semaphore[number_of_customers];
	private static Semaphore customer_takes_exam[] = new Semaphore[number_of_customers];
	private static Semaphore customers_finished[] = new Semaphore[number_of_customers];
	
	
	public static void main(String[] args) {
		// Creates and joins all customer threads. 
		
		// 1 thread for Information Desk created at the beginning
		Thread InfoDesk = new Thread(new InfoDesk());
		InfoDesk.start();
		
		// 1 thread for Announcer created at the beginning
		Thread Announcer = new Thread(new Announcer());
		Announcer.start();
		
		// Create a Thread array to hold Agent threads.
		Thread agents[] = new Thread[number_of_agents];
		
		// Create a Thread array to hold customer threads.
		Thread customers[] = new Thread[number_of_customers];
		
		// Initialize first index of customer_numbers Arraylist to -1.
		// Sequence numbers will begin being added to list at index 1. 
		customer_numbers.add(0, -1);
		
		// Initialize Agent thread array.
		for (int i = 0; i < number_of_agents; ++i) {
			// Create an Agent thread and store it at the current
			// index of the agent Thread array.
			// i is passed into the Agent class constructor to
			// associate a number with the agent.
			agents[i] = new Thread(new Agent(i));
			agents[i].start();
		}
		
		// Initialize customer thread array and Semaphore arrays that are of size number_of_customers.
		for (int i = 0; i < number_of_customers; ++i) {
			// Create a customer thread and store it at the current
			// index of the customer Thread array.
			// i is passed into the customer class constructor to associate
			// a number with the customer (different from the sequence number
			// given at info desk). 
			// Each index of the semaphore arrays are initialized to 0, true.
			customers[i] = new Thread(new Customer(i));
			customers_finished[i] = new Semaphore(0, true);
			can_enter_waiting_area[i] = new Semaphore(0, true);
			number_called[i] = new Semaphore(0, true);
			customers_leaves_line[i] = new Semaphore(0, true);
			customer_is_served[i] = new Semaphore(0, true);
			customer_takes_exam[i] = new Semaphore(0, true);
			customers[i].start();
		}
		
		// Join customer threads.
		for (int i = 0; i < number_of_customers; ++i) {
			try
		      {
		         customers[i].join();
		         System.out.println("Customer " + i + " was joined");
		      }
		      catch (InterruptedException e)
		      {
		    	  System.out.println("Error joining customer threads");
		      }
		}
		
		// When last thread ends, end the simulation.
		System.out.println("Done");
		System.exit(0);
		
	}
	
	static class Customer implements Runnable {
		
		int customer_number = 0;

		Customer (int number) {
			// Set customer_number.
			customer_number = number;
		}
		
		public void run() {
			
			try {
				// Print the customer thread that has started.
				System.out.println("Customer " + customer_number + " created, enters DMV");
				
				// Customer waits in line of information desk. 
				// Only 1 customer can be assigned a sequence number at a time.
				line_for_info_desk.acquire();
				
				// Set the customer_num to the current customer_number so that the Info
				// Desk can assign a sequence number to this customer.
				customer_num = customer_number;
				
				// Signal the Info Desk that a customer is in line for a sequence number.
				customer_enters.release();
				
				// Wait for the customer to get number at the information desk before going into the waiting area.
				can_enter_waiting_area[customer_number].acquire();
				
				// Wait to print the given sequence number until the Info Desk assigns it and adds it to the
				// customer_numbers ArrayList. 
				print_sequence_num.acquire();
				
				// The customer_number ArrayList is a shared resource among the classes, so
				// the using_sequence_num semaphore is used repeatedly to enforce mutual exclusion among
				// the classes and their threads.
				using_sequence_num.acquire();
				// Print the given sequence number for the customer.
				System.out.println("Customer " + customer_number + " gets sequence number " + customer_numbers.indexOf(customer_number) +
						", enters waiting room");
				using_sequence_num.release();
				
				// Signal the Info Desk that the customer left the Info Desk.
				leave_info_desk.release();
				
				// After getting a sequence number, the customer can wait in the waiting area.
				// Signal the Announcer that there is a customer in the waiting area.
				customer_in_waiting_area.release();
				
				// The customer waits for its number to be called before moving to the agent line.
				number_called[customer_number].acquire();
				
				// Once its number is called, print that the customer moved to the agent line.
				System.out.println("Customer " + customer_number + " moves to agent line");
				
				// Wait for the customer to leave the agent line before sending them to an agent.
				customers_leaves_line[customer_number].acquire();
				
				// Signal the Announcer that the customer is going to an agent and another customer can move to the agent
				// line.
				go_to_agent.release();
				
				// Signal the agents that a customer is ready.
				customer_ready.release();
				
				// Wait for the agent to begin serving the customer.
				customer_is_served[customer_number].acquire();
				
				// Since my_agent array is a shared resource, the print_agent_num semaphore is used among the Customer
				// and Agent classes to enforce mutual exclusion when accessing the array.
				print_agent_num.acquire();
				// Print that the customer is being served by the current agent.
				System.out.println("Customer " + customer_number + " is being served by Agent " + my_agent[customer_number]);
				print_agent_num.release();
				
				// Signal the agent to proceed with the exam.
				wait_agent.release();
				
				// Wait for the customer to be asked to take the exam by the agent.
				customer_takes_exam[customer_number].acquire();
				
				// Since my_agent array is a shared resource, the print_agent_num semaphore is used among the Customer
				// and Agent classes to enforce mutual exclusion when accessing the array.
				print_agent_num.acquire();
				// Print that the customer is taking the exam for the current agent.
				System.out.println("Customer " + customer_number + " completes photo and eye exam for Agent " + my_agent[customer_number]);
				print_agent_num.release();
				
				// Signal the agent that the customer completed the exam.
				wait_agent.release();
				
				// Wait for the customer to finish with the agent before taking the license and leaving.
				customers_finished[customer_number].acquire();
				
				// The customer is finished with the agent, so they may leave. 
				System.out.println("Customer " + customer_number + " gets license and departs");
				
				// Signal the agent that the customer left them.
				leave_agent.release();
				
			} catch (InterruptedException e) {
				System.out.println("InterruptException Error in customer");
			}
			
		}
		
	}
	
	static class InfoDesk implements Runnable {
		
		int temp_customer_number = 0;
		int sequence_number = 1;
		
		public void run() {

			try {
					// Print that the Info Desk has been created when the thread starts. 
					System.out.println("Information desk created");
					
					while (true) {
						// Wait until a customer comes to info desk.
						customer_enters.acquire();
						
						// Wrap the process of assigning a sequence number in a wait and signal
						// block. This is so only one customer can be assigned a number at a time.
						new_customer.acquire();
						// Store the current customer_num into a temporary variable to access it later.
						temp_customer_number = customer_num;
						
						// The customer_number ArrayList is a shared resource among the classes, so
						// the using_sequence_num semaphore is used repeatedly to enforce mutual exclusion among
						// the classes and their threads. 
						using_sequence_num.acquire();
						// Sequence number begins at 1. It is given to the current customer.
						// The array list of customer numbers is updated with the current sequence number
						// as the index, and the current customer_num is stored at that index.
						// For example, if index 1 = 14, then customer 14 has been assigned sequence number
						// 1. 
						customer_numbers.add(sequence_number, customer_num);
						sequence_number++;
						// Initialize the next index of the ArrayList to 0 to avoid an Array
						// out of Bounds Exception (the list is offset by 1).
						customer_numbers.add(sequence_number, 0);
						using_sequence_num.release();
						// The customer has been assigned a sequence number, thus the signal to exit the block.
						new_customer.release();
						
						// Signal the customer that it has been assigned a sequence number, which is 
						// accessible with the customer_numbers ArrayList. 
						print_sequence_num.release();
						
						// Signal the customer to go to the waiting area.
						// Temp_customer_number is used here in case the customer_num is changed with
						// another thread entering.
						can_enter_waiting_area[temp_customer_number].release();
						
						// Wait for the customer to leave the information desk before working
						// with another customer.
						leave_info_desk.acquire();

						// Signal that another customer can come to line for info desk.
						line_for_info_desk.release();
						
					}
			} catch (InterruptedException e){
				System.out.println("InterruptedException Error in InfoDesk");
			}
			
			
		}
	}
	
	static class Announcer implements Runnable {
		
		int current_customer = 0;
		int calling_number = 0;
		
		public void run() {
			
			try {
				// Print that the Announcer has been created when its thread starts.
				System.out.println("Announcer created");
				
				while (true) {
					// Wait for a customer to be in the waiting area. 
					customer_in_waiting_area.acquire();
					
					// 4 customers are kept in the agent line at a time.
					agent_line.acquire();
					
					// Begin calling customers 1 by 1. The queue for the agent line is
					// a shared resource, so the calling_customers semaphore
					// is used among the Announcer and Agent classes to enforce mutual exclusion
					// when enqueuing and dequeuing customers. 
					calling_customers.acquire();
					// Increment calling number.
					calling_number++;
					// The customer_number ArrayList is a shared resource among the classes, so
					// the using_sequence_num semaphore is used repeatedly to enforce mutual exclusion among
					// the classes and their threads.
					using_sequence_num.acquire();
					// Since the calling number is a sequence number that has been assigned
					// to a customer, use the calling number as an index into the customer_numbers ArrayList.
					current_customer = customer_numbers.get(calling_number);
					// Print the current calling number
					System.out.println("Announcer calls number " + calling_number);
					using_sequence_num.release();
					
					// If the customer's number is called, signal the customer to come to the agent line.
					number_called[current_customer].release();
					
					// Add the customer to the queue for the agent line.
					customers_in_line.add(current_customer);
					// The current customer has been enqueued into the agent line, so signal out of the blocks.
					calling_customers.release();
					
					agent_line.release();
					
					// Wait for an agent to be ready.
					agent_ready.acquire();
					
					// If an agent is ready, wait to the customer
					// to signal that it can go to an agent
					// before letting them leave. 
					go_to_agent.acquire();
					
					// Signal that the customer has left the line.
					customers_leaves_line[current_customer].release();
					
				}
				
			} catch (InterruptedException e) {
				System.out.println("InterruptedException Error in Announcer");
			}
		}
	}

	static class Agent implements Runnable {
		
		int agent_number = 0;
		
		Agent (int number) {
			agent_number = number;
		}
		
		public void run() {
			
			try {
				// Print that the agent was created when its thread starts.
				System.out.println("Agent " + agent_number + " created");
				
				while(true) {
				// Wait for a customer to be ready in the agent line (their
				// number must be called).
				customer_ready.acquire();
				
				// Remove customer that from the queue.
				calling_customers.acquire();
				int customer_number = customers_in_line.remove();
				calling_customers.release();
				
				// Since my_agent array is a shared resource, the print_agent_num semaphore is used among the Customer
				// and Agent classes to enforce mutual exclusion when accessing the array.
				print_agent_num.acquire();
				// Set the agent number for the current customer (will be accessed by Customer) and print that the
				// agent is serving the customer.
				System.out.println("Agent " + agent_number + " is serving Customer " + customer_number);
				my_agent[customer_number] = agent_number;
				print_agent_num.release();
				
				// Signal the current customer so that it can print that it is being served by the agent.
				customer_is_served[customer_number].release();
				
				// Wait for the customer to print that it is being served by the agent before asking
				// them to take the exam.
				wait_agent.acquire();
				
				// Print that the agent asks the current customer to take the exam.
				System.out.println("Agent " + agent_number + " asks Customer " + customer_number + " to take photo and eye exam");
				
				// Signal the current customer to complete the exam.
				customer_takes_exam[customer_number].release();
				
				// Wait for the customer to take the exam before giving them the license. 
				wait_agent.acquire();
				System.out.println("Agent " + agent_number + " gives license to Customer " + customer_number);
				
				// Customer completed the process with the agent, so signal the customer that they are finished.
				customers_finished[customer_number].release();
				
				// Wait for the customer to leave.
				leave_agent.acquire(); 
				
				// Allow another customer to come to the agent since a customer left.
				agent_ready.release();
				
				}
				
			} catch (InterruptedException e) {
				System.out.println("InterruptedException Error in Agent");
			}

		}
	}

}
