package com.example.sensingapp;

public class WifiData {
	private String m_SSID;
	private String m_BSSID;
	private int m_nSignalLevel;
	
	public WifiData(String SSID, String BSSID, int nSignalLevel) {
		// TODO Auto-generated constructor stub
		m_SSID = SSID;
		m_BSSID = BSSID;
		m_nSignalLevel = nSignalLevel;
	}
	
	public String getSSID() {
		return m_SSID;
	}
	
	public String getBSSID() {
		return m_BSSID;
	}
	
	public int getSignalLevel() {
		return m_nSignalLevel;
	}

}
