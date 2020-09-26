/**
 * @author Sayed Ali Mosavi
 * @since May 21, 2020
 */

package org.cloudbus.cloudsim.power;

import java.util.List;

import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.util.MathUtil;

import com.sun.jndi.toolkit.ctx.Continuation;

/**
 * The Local Regression (LR) VM allocation policy.
 * 
 * If you are using any algorithms, policies or workload included in the power
 * package, please cite the following paper:
 * 
 * Anton Beloglazov, and Rajkumar Buyya, "Optimal Online Deterministic
 * Algorithms and Adaptive Heuristics for Energy and Performance Efficient
 * Dynamic Consolidation of Virtual Machines in Cloud Data Centers", Concurrency
 * and Computation: Practice and Experience (CCPE), Volume 24, Issue 13, Pages:
 * 1397-1420, John Wiley & Sons, Ltd, New York, USA, 2012
 * 
 * @author Anton Beloglazov
 * @since CloudSim Toolkit 3.0
 */
public class PowerVmAllocationPolicyMigrationHybridLocalRegression extends PowerVmAllocationPolicyMigrationAbstract {

	/** The scheduling interval. */
	private double schedulingInterval;

	/** The safety parameter. */
	private double safetyParameter;

	/** The fallback vm allocation policy. */
	private PowerVmAllocationPolicyMigrationAbstract fallbackVmAllocationPolicy;

	/**
	 * Instantiates a new power vm allocation policy migration local regression.
	 * 
	 * @param hostList                   the host list
	 * @param vmSelectionPolicy          the vm selection policy
	 * @param schedulingInterval         the scheduling interval
	 * @param fallbackVmAllocationPolicy the fallback vm allocation policy
	 * @param utilizationThreshold       the utilization threshold
	 */
	public PowerVmAllocationPolicyMigrationHybridLocalRegression(List<? extends Host> hostList,
			PowerVmSelectionPolicy vmSelectionPolicy, double safetyParameter, double schedulingInterval,
			PowerVmAllocationPolicyMigrationAbstract fallbackVmAllocationPolicy, double utilizationThreshold) {
		super(hostList, vmSelectionPolicy);
		setSafetyParameter(safetyParameter);
		setSchedulingInterval(schedulingInterval);
		setFallbackVmAllocationPolicy(fallbackVmAllocationPolicy);
	}

	/**
	 * Instantiates a new power vm allocation policy migration local regression.
	 * 
	 * @param hostList                   the host list
	 * @param vmSelectionPolicy          the vm selection policy
	 * @param schedulingInterval         the scheduling interval
	 * @param fallbackVmAllocationPolicy the fallback vm allocation policy
	 */
	public PowerVmAllocationPolicyMigrationHybridLocalRegression(List<? extends Host> hostList,
			PowerVmSelectionPolicy vmSelectionPolicy, double safetyParameter, double schedulingInterval,
			PowerVmAllocationPolicyMigrationAbstract fallbackVmAllocationPolicy) {
		super(hostList, vmSelectionPolicy);
		setSafetyParameter(safetyParameter);
		setSchedulingInterval(schedulingInterval);
		setFallbackVmAllocationPolicy(fallbackVmAllocationPolicy);
	}

	/**
	 * Checks if is host over utilized.
	 * 
	 * @param host the host
	 * @return true, if is host over utilized
	 */
	@Override
	protected boolean isHostOverUtilized(PowerHost host) {
        PowerHostUtilizationHistory _host = (PowerHostUtilizationHistory) host;
		
		double[] cpuUtilizationHistory = removeZeros(_host.getUtilizationHistory());
		double[] ramUtilizationHistory = removeZeros(_host.getRamUtilizationHistory());
		double[] bwUtilizationHistory = removeZeros(_host.getBWUtilizationHistory());
		
		double[] utilizationHistory = new double[cpuUtilizationHistory.length];
		for(int i=0;i<cpuUtilizationHistory.length; i++) {
			
			double w1 = getMaxValueOfArray(cpuUtilizationHistory);
			double w2 = getMaxValueOfArray(ramUtilizationHistory);
			double w3 = getMaxValueOfArray(bwUtilizationHistory);
			
			double volCpu = w1 / 1-cpuUtilizationHistory[i] ;
			double volRam = w2 / 1-ramUtilizationHistory[i];
			double volBw  = w3 / 1-bwUtilizationHistory[i];
			
			utilizationHistory[i] = volCpu * volRam * volBw;
		}
		
		int length = 5; // we use 10 to make the regression responsive enough to latest values
		if (utilizationHistory.length < length) {
			return getFallbackVmAllocationPolicy().isHostOverUtilized(host);
		}
		double[] utilizationHistoryReversed = new double[length];
		for (int i = 0; i < length; i++) {
			utilizationHistoryReversed[i] = utilizationHistory[length - i - 1];
		}
		double[] estimates = null;
		try {
			estimates = getParameterEstimates(utilizationHistoryReversed);
		} catch (IllegalArgumentException e) {
			return getFallbackVmAllocationPolicy().isHostOverUtilized(host);
		}
		double migrationIntervals = Math.ceil(getMaximumVmMigrationTime(_host) / getSchedulingInterval());
		double predictedUtilization = estimates[0] + estimates[1] * (length + migrationIntervals);
		predictedUtilization *= getSafetyParameter();

		addHistoryEntry(host, predictedUtilization);

		return predictedUtilization >= 1;
		
	}

	/**
	 * Gets the parameter estimates.
	 * 
	 * @param utilizationHistoryReversed the utilization history reversed
	 * @return the parameter estimates
	 */
	protected double[] getParameterEstimates(double[] utilizationHistoryReversed) {
		return MathUtil.getLoessParameterEstimates(utilizationHistoryReversed);
	}

	/**
	 * Gets the maximum vm migration time.
	 * 
	 * @param host the host
	 * @return the maximum vm migration time
	 */
	protected double getMaximumVmMigrationTime(PowerHost host) {
		int maxRam = Integer.MIN_VALUE;
		for (Vm vm : host.getVmList()) {
			int ram = vm.getRam();
			if (ram > maxRam) {
				maxRam = ram;
			}
		}
		return maxRam / ((double) host.getBw() / (2 * 8000));
	}

	/**
	 * Sets the scheduling interval.
	 * 
	 * @param schedulingInterval the new scheduling interval
	 */
	protected void setSchedulingInterval(double schedulingInterval) {
		this.schedulingInterval = schedulingInterval;
	}

	/**
	 * Gets the scheduling interval.
	 * 
	 * @return the scheduling interval
	 */
	protected double getSchedulingInterval() {
		return schedulingInterval;
	}

	/**
	 * Sets the fallback vm allocation policy.
	 * 
	 * @param fallbackVmAllocationPolicy the new fallback vm allocation policy
	 */
	public void setFallbackVmAllocationPolicy(PowerVmAllocationPolicyMigrationAbstract fallbackVmAllocationPolicy) {
		this.fallbackVmAllocationPolicy = fallbackVmAllocationPolicy;
	}

	/**
	 * Gets the fallback vm allocation policy.
	 * 
	 * @return the fallback vm allocation policy
	 */
	public PowerVmAllocationPolicyMigrationAbstract getFallbackVmAllocationPolicy() {
		return fallbackVmAllocationPolicy;
	}

	public double getSafetyParameter() {
		return safetyParameter;
	}

	public void setSafetyParameter(double safetyParameter) {
		this.safetyParameter = safetyParameter;
	}
	// Function to print the array by
	// removing leading zeros
	static double[] removeZeros(double[] array) {
		int targetIndex = 0;
		for (int sourceIndex = 0; sourceIndex < array.length; sourceIndex++) {
			if (array[sourceIndex] != 0.0)
				array[targetIndex++] = array[sourceIndex];
		}
		double[] newArray = new double[targetIndex];
		System.arraycopy(array, 0, newArray, 0, targetIndex);

		return newArray;
	}
	
	static double getMaxValueOfArray(double[] array) {
		double max = 0;
	    for(int i=0; i<array.length; i++ ) {
	       if(array[i]>max) {
	           max = array[i];
	       }
	    }
	    return max;
	}
	
	static double getMinValueOfArray(double[] array)
	{
		 double min = array[0];
	     
	      for(int i=0; i<array.length; i++ ) {
	         if(array[i]<min) {
	            min = array[i];
	         }
	      }
	      return min;
	}

}
