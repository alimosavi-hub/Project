/*
 * Title:        CloudSim Toolkit
 * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation of Clouds
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009-2012, The University of Melbourne, Australia
 */

package org.cloudbus.cloudsim.power;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.util.MathUtil;

/**
 * The Local Regression (LR) VM allocation policy.
 * 
 * If you are using any algorithms, policies or workload included in the power package, please cite
 * the following paper:
 * 
 * Anton Beloglazov, and Rajkumar Buyya, "Optimal Online Deterministic Algorithms and Adaptive
 * Heuristics for Energy and Performance Efficient Dynamic Consolidation of Virtual Machines in
 * Cloud Data Centers", Concurrency and Computation: Practice and Experience (CCPE), Volume 24,
 * Issue 13, Pages: 1397-1420, John Wiley & Sons, Ltd, New York, USA, 2012
 * 
 * @author Anton Beloglazov
 * @since CloudSim Toolkit 3.0
 */
public class PowerVmAllocationPolicyMigrationLocalRegression extends PowerVmAllocationPolicyMigrationAbstract {

	/** The scheduling interval. */
	private double schedulingInterval;

	/** The safety parameter. */
	private double safetyParameter;

	/** The fallback vm allocation policy. */
	private PowerVmAllocationPolicyMigrationAbstract fallbackVmAllocationPolicy;

	/**
	 * Instantiates a new power vm allocation policy migration local regression.
	 * 
	 * @param hostList the host list
	 * @param vmSelectionPolicy the vm selection policy
	 * @param schedulingInterval the scheduling interval
	 * @param fallbackVmAllocationPolicy the fallback vm allocation policy
	 * @param utilizationThreshold the utilization threshold
	 */
	public PowerVmAllocationPolicyMigrationLocalRegression(
			List<? extends Host> hostList,
			PowerVmSelectionPolicy vmSelectionPolicy,
			double safetyParameter,
			double schedulingInterval,
			PowerVmAllocationPolicyMigrationAbstract fallbackVmAllocationPolicy,
			double utilizationThreshold) {
		super(hostList, vmSelectionPolicy);
		setSafetyParameter(safetyParameter);
		setSchedulingInterval(schedulingInterval);
		setFallbackVmAllocationPolicy(fallbackVmAllocationPolicy);
	}

	/**
	 * Instantiates a new power vm allocation policy migration local regression.
	 * 
	 * @param hostList the host list
	 * @param vmSelectionPolicy the vm selection policy
	 * @param schedulingInterval the scheduling interval
	 * @param fallbackVmAllocationPolicy the fallback vm allocation policy
	 */
	public PowerVmAllocationPolicyMigrationLocalRegression(
			List<? extends Host> hostList,
			PowerVmSelectionPolicy vmSelectionPolicy,
			double safetyParameter,
			double schedulingInterval,
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
		double[] utilizationHistory = removeZeros(_host.getUtilizationHistory());
		double[] ramUtilizationHistory = removeZeros(_host.getRamUtilizationHistory());
		double[] bwUtilizationHistory = removeZeros(_host.getBWUtilizationHistory());

		
		OLSMultipleLinearRegression regression = new OLSMultipleLinearRegression();

		final List<double[]> utiLizationList = Arrays.asList(utilizationHistory, ramUtilizationHistory, bwUtilizationHistory);

		double x[][] = new double[utilizationHistory.length][3];
		if(utilizationHistory.length>3)
		{
			for(int i = 0; i< utilizationHistory.length; i++) {
			 for(int j=0; j<utiLizationList.size(); j++){
				x[i][j] = utiLizationList.get(j)[i]; 
				}
			}

		}
		

		double[] y = null;
		
		if(utilizationHistory.length>3) {
			y = new double[utilizationHistory.length];
			for(int i=0;i<utilizationHistory.length; i++) { 
				y[i] = utilizationHistory[i] * ramUtilizationHistory[i] * bwUtilizationHistory[i]; 
			}
		}
		

		int length = 10; // we use 10 to make the regression responsive enough to latest values 

		if (utilizationHistory.length < length) { 
			return getFallbackVmAllocationPolicy().isHostOverUtilized(host); 
		}

		regression.newSampleData(y, x); double[] estimates =
		regression.estimateRegressionParameters();

		double predictedUtilization = 0;
		for (int i = 0; i < utilizationHistory.length; i++) { 
			predictedUtilization = estimates[0] + (estimates[1] * utilizationHistory[i]) + (estimates[2] * ramUtilizationHistory[i]) + (estimates[3] * bwUtilizationHistory[i]);
		}

		predictedUtilization *= getSafetyParameter();

		addHistoryEntry(host, predictedUtilization);
		return predictedUtilization >= 1;
		
		
		/*
		 * PowerHostUtilizationHistory _host = (PowerHostUtilizationHistory) host;
		 * double[] utilizationHistory = removeZeros(_host.getUtilizationHistory());
		 * double[] ramUtilizationHistory =
		 * removeZeros(_host.getRamUtilizationHistory()); double[] bwUtilizationHistory
		 * = removeZeros(_host.getBWUtilizationHistory());
		 * 
		 * OLSMultipleLinearRegression regression = new OLSMultipleLinearRegression();
		 * 
		 * final List<double[]> utiLizationList = Arrays.asList(utilizationHistory,
		 * ramUtilizationHistory, bwUtilizationHistory);
		 * 
		 * double x[][] = new double[utilizationHistory.length][3]; for(int i = 0; i<
		 * utilizationHistory.length; i++) { for(int j=0; j<utiLizationList.size();
		 * j++){ x[i][j] = utiLizationList.get(j)[i]; } }
		 * 
		 * 
		 * double[] y = new double[utilizationHistory.length]; for(int
		 * i=0;i<utilizationHistory.length; i++) { y[i] = utilizationHistory[i] *
		 * ramUtilizationHistory[i] * bwUtilizationHistory[i]; }
		 * 
		 * int length = 10; // we use 10 to make the regression responsive enough to
		 * latest values
		 * 
		 * if (utilizationHistory.length < length) { return
		 * getFallbackVmAllocationPolicy().isHostOverUtilized(host); }
		 * 
		 * regression.newSampleData(y, x); double[] estimates =
		 * regression.estimateRegressionParameters();
		 * 
		 * double predictedUtilization = 0; for (int i = 0; i <
		 * utilizationHistory.length; i++) { predictedUtilization = estimates[0] +
		 * (estimates[1] * utilizationHistory[i]) + (estimates[2] *
		 * ramUtilizationHistory[i]) + (estimates[3] * bwUtilizationHistory[i]); }
		 * 
		 * predictedUtilization *= getSafetyParameter();
		 * 
		 * addHistoryEntry(host, predictedUtilization); return predictedUtilization >=
		 * 1;
		 */		
//		PowerHostUtilizationHistory _host = (PowerHostUtilizationHistory) host;		
//
//		double[] cpuUtilHistory = _host.getUtilizationHistory();
//		double[] ramUtiHistory = removeZeros(_host.getRamUtilizationHistory());
//		double[] bwUtiHistory = _host.getBWUtilizationHistory();
//		
//		int length = 10; // we use 10 to make the regression responsive enough to latest values
//		if (cpuUtilHistory.length < length) {
//			return getFallbackVmAllocationPolicy().isHostOverUtilized(host);
//		}
//
//		double[] cpuUtiHistoryReversed = new double[length];
//		for (int i = 0; i < length; i++) {
//			cpuUtiHistoryReversed[i] = cpuUtilHistory[length - i - 1];
//		}
//		
//		double[] ramUtiHistoryReversed = new double[length];
//		if(ramUtiHistory.length > 1)
//		{
//			for (int i = 0; i < length;i++) {
//				 ramUtiHistoryReversed[i] = ramUtiHistory[length - i - 1]; 
//			}
//		}
//		
//		 
//		double[] bwUtiHistoryReversed = new double[length];
//		for (int i = 0; i < length; i++) {
//			bwUtiHistoryReversed[i] = bwUtiHistory[length - i - 1];
//		}
//
//		double[] cpuEstimates = null;
//		double[] ramEstimates = null;
//		double[] bwEstimates = null;
//
//		try {
//			cpuEstimates = getParameterEstimates(cpuUtiHistoryReversed);
//			
//			ramEstimates = getParameterEstimates(ramUtiHistoryReversed);
//			
//			bwEstimates = getParameterEstimates(bwUtiHistoryReversed);
//			
//		} catch (IllegalArgumentException e) {
//			return getFallbackVmAllocationPolicy().isHostOverUtilized(host);
//		}
//
//		double migrationIntervals = Math.ceil(getMaximumVmMigrationTime(_host) / getSchedulingInterval());
//
//		double predictedCpuUti = cpuEstimates[0] + cpuEstimates[1] * (length + migrationIntervals);
//		double predictedRamUti = ramEstimates[0] + ramEstimates[1] * (length + migrationIntervals);
//		double predictedBwUti = bwEstimates[0] + bwEstimates[1] * (length + migrationIntervals);
//
//		predictedCpuUti *= getSafetyParameter();
//		predictedRamUti *= getSafetyParameter();
//		predictedBwUti *= getSafetyParameter();
//
//
//		addHistoryEntry(host, predictedCpuUti);
//		addHistoryEntry(host, predictedRamUti);
//		addHistoryEntry(host, predictedBwUti);
//
//		
//		/*
//		 * if(predictedCpuUti | predictedRamUti | predictedBwUti >=1) { return
//		 * predictedCpuUti >= 1; }
//		 */
//		boolean predictedUtilization = false;
//		
//		if(predictedCpuUti >= 1)
//		{
//			predictedUtilization =  true;
//		}
//		
//		if(predictedRamUti >= 1) {
//			predictedUtilization = true;
//		}
//		
//		if(predictedBwUti >= 1) {
//			predictedUtilization = true;
//		}
//		 
//		 
//		return predictedUtilization;

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
	public void setFallbackVmAllocationPolicy(
			PowerVmAllocationPolicyMigrationAbstract fallbackVmAllocationPolicy) {
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


}
