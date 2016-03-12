package com.example.sensingapp;

import java.util.List;

import android.hardware.SensorEvent;
import android.location.*;

public class SensorData {
	private int m_nSensorDataType;	//Sensor date type
	
	private double m_fSoundLevelDb;   //Sound level
	private int m_nCellId;			//Cell ID
	private Location m_locGps;		//GPS location information
	private SensorEvent m_senEvt; 	//Sensor Event
	private List<WifiData> m_lstWifiData;  //WifiData
	
	public SensorData(int nSensorDataType, SensorEvent senEvt) {
		// TODO Auto-generated constructor stub
		m_nSensorDataType = nSensorDataType;
		m_senEvt = senEvt;
	}
	
	public SensorData(int nSensorDataType, Location locGps) {
		m_nSensorDataType = nSensorDataType;
		m_locGps = locGps;
	}
	
	public SensorData(int nSensorDataType, double fSoundLevelDb) {
		m_nSensorDataType = nSensorDataType;
		m_fSoundLevelDb = fSoundLevelDb;
	}
	
	public SensorData(int nSensorDataType, int nCellId) {
		m_nSensorDataType = nSensorDataType;
		m_nCellId = nCellId;
	}
	
	public SensorData(int nSensorDataType, List<WifiData> lstWifiData) {
		m_nSensorDataType = nSensorDataType;
		m_lstWifiData =  lstWifiData;		
	}
	
	
	public int getSensorDataType() {
		return m_nSensorDataType;
	}
	
	public SensorEvent getSensorEvent() {
		return m_senEvt;
	}
	
	public Location getGpsLocation() {
		return m_locGps;
	}
	
	public double getSoundLevelDb() {
		return m_fSoundLevelDb;
	}
	
	public int getCellId() {
		return m_nCellId;
	}
	
	public List<WifiData> getListWifiData() {
		return m_lstWifiData;
	}

}
