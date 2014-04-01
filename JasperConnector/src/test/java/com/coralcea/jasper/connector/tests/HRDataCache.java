package com.coralcea.jasper.connector.tests;

import java.util.HashMap;

import com.coralcea.jasper.connector.tests.generated.HRData;
import com.coralcea.jasper.connector.tests.generated.HRDataImpl;

public class HRDataCache {

	private static HRDataCache instance = new HRDataCache();
	
	public static HRDataCache getInstance() {
		return instance;
	}
	
	private HashMap<String, HRData> cache = new HashMap<String, HRData>();
	
	public HRDataCache() {
		reset();
	}
	
	public void reset() {
		HRData hr;
		
		hr = new HRDataImpl();
		hr.setBpm(100);
		hr.setTimestamp("100:100:100");
		cache.put("100", hr);
		
		hr = new HRDataImpl();
		hr.setBpm(200);
		hr.setTimestamp("200:200:200");
		cache.put("200", hr);

		hr = new HRDataImpl();
		hr.setBpm(300);
		hr.setTimestamp("300:300:300");
		cache.put("300", hr);
	}
	
	public void put(String sid, HRData HRData) {
		cache.put(sid, HRData);
	}

	public HRData get(String sid) {
		return cache.get(sid);
	}
	
	public HRData[] getAll() {
		HRData[] a = new HRData[cache.values().size()];
		return cache.values().toArray(a);
	}
}
