package org.cloudbus.cloudsim.examples.power.random;

import java.io.IOException;

/**
 * A simulation of a heterogeneous power aware data center that applies the Ordinary Least Squares Multiple Regression (OLSMR) VM
 * allocation policy and Minimum Migration Time (MMT) VM selection policy.
 * 
 * The remaining configuration parameters are in the Constants and RandomConstants classes.
 * 
 * @author Sayed Ali Mosavi
 * @since May 17, 2020
 */
public class OlsMrMmt {

	/**
	 * The main method.
	 * 
	 * @param args the arguments
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public static void main(String[] args) throws IOException {
		boolean enableOutput = true;
		boolean outputToFile = false;
		String inputFolder = "";
		String outputFolder = "";
		String workload = "random"; // Random workload
		String vmAllocationPolicy = "OlsMr"; //Ordinary Least Squares Multiple Regression (OLSMR) VM allocation policy
		String vmSelectionPolicy = "mmt"; // Minimum Migration Time (MMT) VM selection policy
		String parameter = "0.5"; // the safety parameter of the OlsMr policy

		new RandomRunner(
				enableOutput,
				outputToFile,
				inputFolder,
				outputFolder,
				workload,
				vmAllocationPolicy,
				vmSelectionPolicy,
				parameter);
	}

}
