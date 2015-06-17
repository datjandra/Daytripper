package com.vocifery.daytripper.util;

public abstract class MetricUtils {

	public final static double R = 6371; // Radius of the earth in km
	
	public final static double metersToKilometers(double meters) {
		return (meters / 1000d);
	}
	
	public final static double kilometersToMiles(double kilometers) {
		return (kilometers * 0.621d);
	}
	
	public final static double degToRad(double deg) {
		return deg * (Math.PI / 180d);
	}
	
	public final static double haversineDistance(double startLat, double startLon, double endLat, double endLon) {
		double degLat = degToRad(endLat - startLat);
		double degLon = degToRad(endLon - startLon);
		double a = Math.sin(degLat/2d) * Math.sin(degLat/2d) 
				+ Math.cos(degToRad(startLat)) * Math.cos(degToRad(endLat)) * Math.sin(degLon/2d) * Math.sin(degLon/2d);
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
		double d = R * c;
		return d;
	}
}
