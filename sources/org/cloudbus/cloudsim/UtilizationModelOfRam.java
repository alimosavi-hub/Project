/*
 * Title:        CloudSim Toolkit
 * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation of Clouds
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009-2012, The University of Melbourne, Australia
 */

package org.cloudbus.cloudsim;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * The UtilizationModelNull class is a simple model, according to which a Cloudlet always require
 * zero capacity.
 * 
 * @author Anton Beloglazov
 * @since CloudSim Toolkit 2.0
 */
public class UtilizationModelOfRam implements UtilizationModel {

	
	/** The scheduling interval. */
	private double schedulingInterval;

	/** The data (5 min * 288 = 24 hours). */
	private double[] data;
	  
	
	public UtilizationModelOfRam(String inputPath, double schedulingInterval) throws NumberFormatException, IOException {
		BufferedReader input = new BufferedReader(new InputStreamReader(new
		FileInputStream(inputPath)));
	    String line = input.readLine();
	    line = input.readLine(); setSchedulingInterval(schedulingInterval);
	  
	    String[] fields = line.split(";",-1);
	    
	    data = new double[2000];
	    int n = data.length;
	    for (int i = 0; i < n - 1; i++) {
	    	double ram  = Double.parseDouble(fields[6].trim()) / 1024;
		    data[i] = ((ram / 16384) * 100) / 100.0;
		}
		    
	    data[n - 1] = data[n - 2];
	    input.close();
	}

	
	@Override
	public double getUtilization(double time) {
		if (time % getSchedulingInterval() == 0) {
			return data[(int) time / (int) getSchedulingInterval()];
		}
		int time1 = (int) Math.floor(time / getSchedulingInterval());
		int time2 = (int) Math.ceil(time / getSchedulingInterval());
		double utilization1 = data[time1];
		double utilization2 = data[time2];
		double delta = (utilization2 - utilization1) / ((time2 - time1) * getSchedulingInterval());
		double utilization = utilization1 + delta * (time - time1 * getSchedulingInterval());

		return utilization;
		
	}
	
	/**
	 * Sets the scheduling interval.
	 * 
	 * @param schedulingInterval the new scheduling interval
	 */
	public void setSchedulingInterval(double schedulingInterval) {
		this.schedulingInterval = schedulingInterval;
	}
	
	/**
	 * Gets the scheduling interval.
	 * 
	 * @return the scheduling interval
	 */
	public double getSchedulingInterval() {
		return schedulingInterval;
	}
	
}
