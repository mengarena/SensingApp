package com.example.sensingapp;

/*
 * ****************************************************************************
 * Copyright 2014 Rufeng Meng <mengrufeng@gmail.com>
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301, USA.
 * ****************************************************************************
 */

/*  This program is used to collect sensor data from smartphone.
 *  
 *  Currently, the following sensor data could be recorded:
 *  1) Accelerometer
 *  2) Linear Acceleration
 *  3) Gyroscope
 *  4) Orientation
 *  5) Magnetic Field
 *  6) Light
 *  7) Barometer
 *  8) Audio (Sould level and audio file)
 *  9) Cellular ID
 *  10) GPS (Latitude, Longitude, Altitude, GeoDeclination, GeoInclination, Speed, Bearing)
 *  11) WiFi (SSID, BSSID, RSS)
 *  
 *  Data are stored locally as CSV files under /sdcard/Sensing/SensorData/ and could be uploaded to MySQL Server
 *   
 */

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.*;
import android.content.pm.*;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Environment;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.provider.Settings;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.RadioButton;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.Toast;
import android.view.KeyEvent;
import android.view.View;
import android.telephony.CellLocation;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.text.*;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.GeomagneticField;
import android.util.*;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.wifi.*;
import android.content.BroadcastReceiver;
import android.location.*;
import android.app.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Calendar;
import java.util.Locale;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.FileWriter;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.http.client.ClientProtocolException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import android.os.StrictMode;

@SuppressLint("NewApi")
public class SensingApp extends Activity implements SensorEventListener {
	private static final String TAG = "SensingApp";
	private static final int WIFI_COUNT = 20; // How many Wifi SSID to record from the scanned list (originally 10)
	
	private String[] m_arrsWiFiScanSpeed = {
			"Fast",
			"Normal",
			"Slow"};

	private int[] m_arrlWiFiScanSpeed = {
		500,
		3000,
		15*1000};
	
	
	private static boolean m_blnRecordStatus = false; // true: Recording; false: Stopped 
	
	
	//Sensor data type
	private static final int DATA_TYPE_SENSOR = 1;
	private static final int DATA_TYPE_MIC = 2;
	private static final int DATA_TYPE_CELLULAR = 3;
	private static final int DATA_TYPE_GPS = 4;
	private static final int DATA_TYPE_WIFI = 5;
	
	//Sensor event type
	private static int SENSOR_EVENT_NULL = 0;
	private static int SENSOR_EVENT_ACCL = 1;
	private static int SENSOR_EVENT_LINEAR_ACCL = 2;
	private static int SENSOR_EVENT_GYRO = 3;
	private static int SENSOR_EVENT_ORIENT = 4;
	private static int SENSOR_EVENT_MAGNET = 5;
	private static int SENSOR_EVENT_LIGHT = 6;
	private static int SENSOR_EVENT_BAROMETER = 7;
	private static int SENSOR_EVENT_MIC = 8;
	private static int SENSOR_EVENT_CELLULAR = 9;
	private static int SENSOR_EVENT_GPS = 10;
	private static int SENSOR_EVENT_WIFI = 11;
	private static int SENSOR_EVENT_GRAVITY = 12;
		
	private SensorManager m_smSurScan = null;
			
	/* Sensor is available or not */
	private static boolean m_blnOrientPresent = false;
	private static boolean m_blnGyroPresent = false;
	private static boolean m_blnAcclPresent = false;
	private static boolean m_blnLinearAcclPresent = false;
	private static boolean m_blnGravityPresent = false;
	private static boolean m_blnLightPresent = false;
	private static boolean m_blnBarometerPresent = false;

	private static boolean m_blnMagnetPresent = false;
	
	/* Sensor is selected or not */
	private static boolean m_blnOrientEnabled = false; /* false: Sensor data will not be recorded; 
														true: will be recorded */
	private static boolean m_blnGyroEnabled = false;
	private static boolean m_blnAcclEnabled = false;
	private static boolean m_blnLinearAcclEnabled = false;
	private static boolean m_blnGravityEnabled = false;
	private static boolean m_blnLightEnabled = false;
	private static boolean m_blnBarometerEnabled = false;

	private static boolean m_blnMagnetEnabled = false;
	
	private static boolean m_blnMicEnabled = false;
	private static boolean m_blnCellularEnabled = false;
	
	/* Wifi is selected or not */
	private static boolean m_blnWifiEnabled = false;
	
	/* GPS is selected or not */
	private static boolean m_blnGPSEnabled = false;

		
	/* Default delay mode for sensors */
	private static int m_nAcclMode = SensorManager.SENSOR_DELAY_FASTEST;
	private static int m_nLinearAcclMode = SensorManager.SENSOR_DELAY_FASTEST;
	private static int m_nGravityMode = SensorManager.SENSOR_DELAY_FASTEST;
	private static int m_nGyroMode = SensorManager.SENSOR_DELAY_FASTEST;
	private static int m_nOrientMode = SensorManager.SENSOR_DELAY_FASTEST;
	private static int m_nMagnetMode = SensorManager.SENSOR_DELAY_FASTEST;
	private static int m_nLightMode = SensorManager.SENSOR_DELAY_FASTEST;
	private static int m_nBarometerMode = SensorManager.SENSOR_DELAY_FASTEST;
	
		
	private Location m_location;
		
	private String m_sRecordFile; //Sensor record file
	private String m_sFullPathFile; //Sensor record full pathfile
	
	private FileWriter m_fwSensorRecord = null;
	private SensingApp m_actHome = this;
	private Date m_dtFileStart;	//The time sensor data file is created (for each data file intervally)
	private ResolveInfo m_riHome;
	
	private boolean m_blnWifiSignalEnabled = false; // true: Wifi signal is enabled
	private boolean m_blnGPSSignalEnabled = false; // true: GPS signal is enabled
	
	private float[] m_arrfAcclValues = null;	 // Store accelerometer values
	private float[] m_arrfMagnetValues = null; // Store magnetic values
	private float[] m_arrfOrientValues = new float[3]; // Store orientation values

	private WifiManager m_mainWifi = null;
	private WifiReceiver m_receiverWifi = null;
	List<ScanResult> m_lstWifiList; // Wifi List

	private Thread m_wifiScanThread = null;
	//private int m_nWiFiScanInterval = 500;   //500ms
	private int m_nWifiScanSpeedIndex = 0;
	
	private LocationManager m_locManager = null;
	private String m_sGPSProvider = LocationManager.GPS_PROVIDER; //GPS provider

	//Sensor data fields in CSV file
	private static String m_sAccl = ",,,";
	private static String m_sLinearAccl = ",,,";
	private static String m_sGravity = ",,,";	
	private static String m_sGyro = ",,,";	
	private static String m_sOrient = ",,,";
	private static String m_sMagnet = ",,,";

	private static String m_sLight = ",";
	private static String m_sBarometer = ",";
	
	private static String m_sGPS = ",,,,,,,";
	
	private static String m_sSouldLevel = ",";
	private static String m_sCellId = ",";
	private static String m_sWifi = ",,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,";   // 20 WiFi <BSSID, RSS> tupes
	
		
	private static String m_sSensorAccuracy=",,";
		
	///////////////////////////////////////////////////
	private String m_sSettingFoler = "";       //Setting folder, which will be /sdcard/Sensing/Setting
	private String m_sExistingProjectNameFileName = "ExistingProjectName.txt";   //Existing project names are saved in this file under Setting folder
	private String m_sExistingUsernameFileName = "ExistingUsername.txt";  //Existing user names are saved in this file under Setting folder
	private String m_sExistingLabelFileName = "ExistingLabel.txt";  //Existing label names are saved in this file under Setting folder
	private String m_sUnUploadedFileName = "UnUploadedFileName.txt";  //The list of sensor data files which have not been uploaded

	private String m_sSensorDataFolder = "";   //Sensor data folder, which will be /sdcard/Sensing/SensorData

	private static int m_nProjectNameType = -1;    //0--New,   1--Existing
	private static int m_nUsernameType = -1;	   //0--New,   1--Existing
	private static String m_sProjectName = "";
	private static String m_sUsername = "";
	private static String m_sCurrentLabel = "";
	private static String m_sSensingStartTime = "";
	private static String m_sSensingEndTime = "";
	
	private boolean m_blnRecordSoundFile = true;
	private boolean m_blnRecordSoundLevel = true;
	private int m_nBufferSize = 0;
	private AudioRecord m_audioRecorder = null;
	private Thread m_soundLevelThread = null;
	private Thread m_processSoundThread = null;
	private String m_sRecordFullPathFile = "";
	private double m_fSoundLevelDb = 1000;  //db, the normal value should be < 0 db, 1000 stands for "no reading".
	
	private int m_nAudioSampleRate = 44100;  //Audio sample rate
    private byte m_btAudioBuffer[];
    private int m_nAudioReadCount = AudioRecord.ERROR_INVALID_OPERATION;;

	private TelephonyManager m_tmCellular = null;
    private Thread m_cellularThreadGsm = null;
    private Thread m_cellularThreadCdma = null;	
	private GsmCellLocation m_GsmCellLocation = null;
	private CdmaCellLocation m_CdmaCellLocation = null;
	
	private static int SCREEN_1 = 1;
	private static int SCREEN_6 = 6;
	
	private static int m_nPreviousScreen = -1;  //For "Back" operation
	
	private List<String> m_lstExistingProjectName = new ArrayList<String>();
	private ArrayAdapter<String> m_adpExistingProjectName;

	private List<String> m_lstExistingUsername = new ArrayList<String>();
	private ArrayAdapter<String> m_adpExistingUsername;
	
	private List<String> m_lstSensorDataLabel = new ArrayList<String>();
	private int m_nCurrentLabelIndex = -1;
	
	private List<String> m_lstUsedSensorDateLabel = new ArrayList<String>();
	
	private List<String> m_lstUnUploadedFileName = new ArrayList<String>();
	private List<Integer> m_lstUploadedFileNameIndex = new ArrayList<Integer>();   //This is not the list of all the uploaded file name,
																		    //It is the file name which is in the m_lstUnUploadedFileName, 
																			//but during the "Upload" operation, its upload is completed.
 	
	private static boolean m_blnNoLabel = false;
	
	private static boolean m_blnGpsUp = false;
	
	//Screen 1: Begin
	private RadioGroup m_rdgpProjectNameType,m_rdgpUsernameType;
	private RadioButton m_rdProjectNameType_New,m_rdProjectNameType_Existing;
	private RadioButton m_rdUsernameType_New,m_rdUsernameType_Existing;
	
	private EditText m_etProjectName_New, m_etUsername_New;
	private Spinner m_spnScreen1_ExistingProjectName;
	private Spinner m_spnScreen1_ExistingUsername;
	private Button m_btnScreen1_Next;
	private Button m_btnScreen1_Upload;
	//Screen 1: End
	
	//Screen 2: Begin
	private CheckBox m_chkAccl,m_chkLinearAccl, m_chkGravity,m_chkGyro,m_chkOrient,m_chkLight,m_chkBarometer;
	private CheckBox m_chkMagnet;
	private CheckBox m_chkWifi,m_chkGPS;
	private CheckBox m_chkMic, m_chkCellular;

	private RadioGroup m_rdgpSensor,m_rdgpSensorMode;
	private RadioButton m_rdSensorOrient,m_rdSensorGyro,m_rdSensorAccl,m_rdSensorLinearAccl, m_rdSensorGravity, m_rdSensorMagnet,m_rdSensorLight;
	private RadioButton m_rdSensorBarometer;
	private RadioButton m_rdSensorModeFastest,m_rdSensorModeGame,m_rdSensorModeNormal,m_rdSensorModeUI;
	
	private Spinner m_spnScreen2_WiFiScanSpeed;
	private ArrayAdapter<String> m_adpWiFiScanSpeed;
	private Button m_btnScreen2_Back;
	private Button m_btnScreen2_Next;
	//Screen 2: End
	
	//Screen 3: Begin
	private EditText m_etSensorDataLabel;
	private Button m_btnScreen3_AddLabel;
//	private TextView m_tvScreen3LabelList;
	private EditText m_etScreen3LabelList;

	private Button m_btnScreen3_Back;
	private Button m_btnScreen3_Next;
	//Screen 3: End
	
	//Screen 4: Begin
	private Spinner m_spnScreen4_SelectSensorDataLabel;
	private ArrayAdapter<String> m_adpSensorDataLabel;
	private List<String> m_lstSensorDataLabel_Screen4 = new ArrayList<String>();
	private CheckBox m_chkNoLabel;
	private Button m_btnScreen4_Back;
	private Button m_btnScreen4_Start;

	//Screen 4: End
	
	//Screen 5: Begin
	private TextView m_tvGpsUp;
	private TextView m_tvSensingInfo;
	private Button m_btnScreen5_Stop;
	private RadioButton m_arrLabelButton[];
	private EditText m_etScreen5_TempLabel;
	private CheckBox m_chkScreen5_UseTempLabel;
	private ScrollView m_svScreen5_Label;
	
	private RadioGroup m_rdgpLabelRadioButtonGroup;
	private LayoutParams m_lpLabelButtonParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);

	//Screen 5: End

	//Screen 6: Begin
	private TextView m_tvScreen6_Username;
	private TextView m_tvScreen6_ProjectName;
	private TextView m_tvScreen6_StartTime;
	private TextView m_tvScreen6_EndTime;
	private TextView m_tvScreen6_LabelNumber;

	private Button m_btnScreen6_Back;
	private Button m_btnScreen6_Upload;
	//Screen 6: End
	
	//Screen Upload Data:  Begin
	private Button m_btnScreenUploadData_Upload;
	private Button m_btnScreenUploadData_Back;
	private LinearLayout m_llUploadData_ChkLayoutFileName;
	private CheckBox m_arrChkFilename[];
	private LayoutParams m_lpFilenameChkParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
	private ProgressBar m_pgbActivityIndicator;
	private boolean m_blnUpload = false;
	
	//Screen Upload Data:  End
	
	//For Sensor working after screen off
	private WakeLock m_wakeLock;
	
	private static boolean m_blnUploadFunctionEnabled = false;    //Change this to "true" to enable the Upload function
	
	private static final String m_sURL = "http://xxx.xxx.xxx.xxx/sensordata_upload.php";   //Set the server address for sensor data upload
	////////////////////////////////////////////////
	
	
	public static void PrintCurrentTime(String sTag) {
	    Date dtFileStart = new Date();
 	    final String DATE_FORMAT = "yyyy/MM/dd-HH:mm:ss";
 	    SimpleDateFormat spdCurrentTime = new SimpleDateFormat(DATE_FORMAT);
 	   
 	    System.out.println("[" + sTag + "] Time:  " + spdCurrentTime.format(dtFileStart));	
	}

	
	/* Class for upload recorded sensor data file to server */
	private class SensorDataUpload extends AsyncTask<Void, Void, Void> {
		
		private List<String> m_lstSelectedUploadFile = new ArrayList<String>();
		private List<Integer> m_lstSelectedUploadFileID = new ArrayList<Integer>();
		private HttpClient m_httpClt = null;
		private HttpPost m_httpPost = null;
					
		public SensorDataUpload(List<String> lstSelectedUploadFile, List<Integer> lstSelectedUploadFileID) {
			m_lstSelectedUploadFile = lstSelectedUploadFile;
			m_lstSelectedUploadFileID = lstSelectedUploadFileID;
			
			m_httpClt = new DefaultHttpClient();
			m_httpPost = new HttpPost(m_sURL);
		}
			
		private List<String> getUserProjName(String sFilePathName) {
			List<String> lstUserProjName = new ArrayList<String>();
			
	    	String sFileName;
	    	int nPos = -1;
	    	String delims = "-";
	    	
	    	nPos = sFilePathName.lastIndexOf('/');
	    	sFileName = sFilePathName.substring(nPos+1);
	    	String[] names = sFileName.split(delims);
	    	lstUserProjName.add(names[0]);
	    	lstUserProjName.add(names[1]);
	    	
			return lstUserProjName;
		}
		 
		
		private void operateDb() {
	    	FileReader fr;
	    	BufferedReader br;
	    	String sLine = "";
	    	String[] fieldsData;
	    	String[] fields;
	    	String sUsername = "";
	    	String sProjectname = "";
	    	int nFieldCnt = 59;  // Number of fields
	    	List<String> lstUserProjName = new ArrayList<String>();
	    	
			for (int i=0; i<m_lstSelectedUploadFile.size(); i++) {
				if (m_blnUpload == false) break;
				
				lstUserProjName = getUserProjName(m_lstSelectedUploadFile.get(i));
				sUsername = lstUserProjName.get(0);
				sProjectname = lstUserProjName.get(1);
				
				try {
					fr = new FileReader(m_lstSelectedUploadFile.get(i));
					br = new BufferedReader(fr);
					
					while( ((sLine = br.readLine()) != null) && (m_blnUpload == true)) {
						fields = new String[nFieldCnt];
						
						for (int j=0; j<nFieldCnt; j++) {
							fields[j]="";
						}
						
						fieldsData = sLine.split(",");
						for (int k=0; k<fieldsData.length; k++) {
							fields[k] = fieldsData[k];
						}
						/*
						 *  0) Timestamp
						 *  1) Label
						 *  2) SensingEventType
						 *  3-5) Accl
						 *  6-8) Linear Accl
						 *  9-11) Gravity
						 *  12-14) Gyro
						 *  15-17) Orientation
						 *  18-20) Magnet
						 *  21) Light
						 *  22) Barometer
						 *  23) Sould Level (Decibel)
						 *  24) Cell ID
						 *  25-31) GPS (Lat, Long, Alt, Declination, Inclination, Speed, Bearing)
						 *  32-61) WiFi (<SSID, BSSID, Level>) 
						 */
						
						try {
							List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
							nameValuePairs.add(new BasicNameValuePair("projectname", sProjectname));
							nameValuePairs.add(new BasicNameValuePair("username", sUsername));
							nameValuePairs.add(new BasicNameValuePair("timestamp", fields[0]));
							nameValuePairs.add(new BasicNameValuePair("label", fields[1]));
							nameValuePairs.add(new BasicNameValuePair("datatype", fields[2]));
							
							nameValuePairs.add(new BasicNameValuePair("acclX", fields[3]));
							nameValuePairs.add(new BasicNameValuePair("acclY", fields[4]));
							nameValuePairs.add(new BasicNameValuePair("acclZ", fields[5]));

							nameValuePairs.add(new BasicNameValuePair("linearAcclX", fields[6]));
							nameValuePairs.add(new BasicNameValuePair("linearAcclY", fields[7]));
							nameValuePairs.add(new BasicNameValuePair("linearAcclZ", fields[8]));
							
							nameValuePairs.add(new BasicNameValuePair("gravityX", fields[9]));
							nameValuePairs.add(new BasicNameValuePair("gravityY", fields[10]));
							nameValuePairs.add(new BasicNameValuePair("gravityZ", fields[11]));

							nameValuePairs.add(new BasicNameValuePair("gyroX", fields[12]));
							nameValuePairs.add(new BasicNameValuePair("gyroY", fields[13]));
							nameValuePairs.add(new BasicNameValuePair("gyroZ", fields[14]));
							
							nameValuePairs.add(new BasicNameValuePair("orientX", fields[15]));
							nameValuePairs.add(new BasicNameValuePair("orientY", fields[16]));
							nameValuePairs.add(new BasicNameValuePair("orientZ", fields[17]));
							
							nameValuePairs.add(new BasicNameValuePair("magnetX", fields[18]));
							nameValuePairs.add(new BasicNameValuePair("magnetY", fields[19]));
							nameValuePairs.add(new BasicNameValuePair("magnetZ", fields[20]));							
							
							nameValuePairs.add(new BasicNameValuePair("light", fields[21]));
							
							nameValuePairs.add(new BasicNameValuePair("barometer", fields[22]));
							
							nameValuePairs.add(new BasicNameValuePair("soundlevel", fields[23]));
							
							nameValuePairs.add(new BasicNameValuePair("cellid", fields[24]));
							
							nameValuePairs.add(new BasicNameValuePair("gpsLat", fields[25]));
							nameValuePairs.add(new BasicNameValuePair("gpsLong", fields[26]));
							nameValuePairs.add(new BasicNameValuePair("gpsAlt", fields[27]));
							nameValuePairs.add(new BasicNameValuePair("geoDeclination", fields[28]));
							nameValuePairs.add(new BasicNameValuePair("geoInclination", fields[29]));							
							nameValuePairs.add(new BasicNameValuePair("gpsSpeed", fields[30]));
							nameValuePairs.add(new BasicNameValuePair("gpsBearing", fields[31]));
							
							nameValuePairs.add(new BasicNameValuePair("wifiSSID1", fields[32]));
							nameValuePairs.add(new BasicNameValuePair("wifiBSSID1", fields[33]));
							nameValuePairs.add(new BasicNameValuePair("wifiRSS1", fields[34]));
							
							nameValuePairs.add(new BasicNameValuePair("wifiSSID2", fields[35]));
							nameValuePairs.add(new BasicNameValuePair("wifiBSSID2", fields[36]));
							nameValuePairs.add(new BasicNameValuePair("wifiRSS2", fields[37]));
							
							nameValuePairs.add(new BasicNameValuePair("wifiSSID3", fields[38]));
							nameValuePairs.add(new BasicNameValuePair("wifiBSSID3", fields[39]));
							nameValuePairs.add(new BasicNameValuePair("wifiRSS3", fields[40]));
							
							nameValuePairs.add(new BasicNameValuePair("wifiSSID4", fields[41]));
							nameValuePairs.add(new BasicNameValuePair("wifiBSSID4", fields[42]));
							nameValuePairs.add(new BasicNameValuePair("wifiRSS4", fields[43]));
							
							nameValuePairs.add(new BasicNameValuePair("wifiSSID5", fields[44]));
							nameValuePairs.add(new BasicNameValuePair("wifiBSSID5", fields[45]));
							nameValuePairs.add(new BasicNameValuePair("wifiRSS5", fields[46]));
							
							nameValuePairs.add(new BasicNameValuePair("wifiSSID6", fields[47]));
							nameValuePairs.add(new BasicNameValuePair("wifiBSSID6", fields[48]));
							nameValuePairs.add(new BasicNameValuePair("wifiRSS6", fields[49]));
							
							nameValuePairs.add(new BasicNameValuePair("wifiSSID7", fields[50]));
							nameValuePairs.add(new BasicNameValuePair("wifiBSSID7", fields[51]));
							nameValuePairs.add(new BasicNameValuePair("wifiRSS7", fields[52]));
							
							nameValuePairs.add(new BasicNameValuePair("wifiSSID8", fields[53]));
							nameValuePairs.add(new BasicNameValuePair("wifiBSSID8", fields[54]));
							nameValuePairs.add(new BasicNameValuePair("wifiRSS8", fields[55]));
							
							nameValuePairs.add(new BasicNameValuePair("wifiSSID9", fields[56]));
							nameValuePairs.add(new BasicNameValuePair("wifiBSSID9", fields[57]));
							nameValuePairs.add(new BasicNameValuePair("wifiRSS9", fields[58]));
							
							nameValuePairs.add(new BasicNameValuePair("wifiSSID10", fields[59]));
							nameValuePairs.add(new BasicNameValuePair("wifiBSSID10", fields[60]));
							nameValuePairs.add(new BasicNameValuePair("wifiRSS10", fields[61]));
							
							m_httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
							m_httpClt.execute(m_httpPost);
							
							//HttpResponse response = httpClt.execute(httpPost);
							//HttpEntity resEntity = response.getEntity();
							//if(resEntity != null) {
							//	String responseMsg = EntityUtils.toString(resEntity).trim();
							//	Log.i("......Response: " + responseMsg); 
							//}
						} catch (Exception e) {

						}
					
					}  //While
					
					fr.close();
				} catch (Exception e) {
					
				}					
				
				m_lstUploadedFileNameIndex.add(m_lstSelectedUploadFileID.get(i));

			}  //For
			
			
		}
		
		
		@Override
		protected Void doInBackground(Void...params) {
			// TODO Auto-generated method stub
			operateDb();
			return null;
		}
		
		protected void onProgressUpdate(Void... params) {

		}	
		
		protected void onPostExecute(Void result) {
			int i;
			
			m_pgbActivityIndicator.setVisibility(View.GONE);
			m_btnScreenUploadData_Back.setEnabled(true);
			m_btnScreenUploadData_Upload.setText(R.string.upload);
			
			for (i=0; i<m_arrChkFilename.length; i++) {
				m_arrChkFilename[i].setEnabled(true);
			}
			
			//Update the UnUploadedFileName.txt
			for (i=m_lstUploadedFileNameIndex.size()-1; i>=0; i--) {
				m_lstUnUploadedFileName.remove(m_lstUploadedFileNameIndex.get(i).intValue());
			}
			
			updateUnUploadedFileName(m_lstUnUploadedFileName);

			refreshUploadFileNameBox();
			
			m_blnUpload = false;
		}

	}
	
	
	//When the power button is pushed, screen goes off, to still keep the service running, some need to be re-enabled
    public BroadcastReceiver m_ScreenOffReceiver = new BroadcastReceiver() {
    	
    	public void onReceive(Context context, Intent intent) {
    		
    		if (!intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
    			return;
    		}
    		    		
    		if (m_blnRecordStatus == true) {
    			    			
    			if ((m_blnWifiSignalEnabled == true) && (m_blnWifiEnabled == true)) {
    	    		if (m_receiverWifi != null) {
    	    			unregisterReceiver(m_receiverWifi);
    	    			registerReceiver(m_receiverWifi, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
    	 				//m_mainWifi.startScan();
    	 				startWiFiScan();
    	    		}
    			}
    			
     			if (m_blnGPSSignalEnabled == true && m_blnGPSEnabled == true) {
     				if (m_locManager != null) {
     					m_locManager.removeUpdates(m_locListener);
     					m_locManager.requestLocationUpdates(m_sGPSProvider, 0L, 0.0f, m_locListener);
     				}
     			}
	
    		}
    			
    	}
    	
    };
    
    
    private boolean containComma(String sContent) {
    	boolean bRet = false;
    	
    	bRet = sContent.contains(",");
   
    	return bRet;
    }
    
    //Refresh the sensor data file list when the selected files are uploaded
    private void refreshUploadFileNameBox() {
    	int i;
    	int nCnt = m_arrChkFilename.length;
		for (i=nCnt-1; i>=0; i--) {
			m_llUploadData_ChkLayoutFileName.removeViewAt(i);
			m_arrChkFilename[i] = null;
		}

    	int nCount = m_lstUnUploadedFileName.size();
    	m_arrChkFilename = new CheckBox[nCount];
    	
    	for (i=0; i<nCount; i++) {
    		m_arrChkFilename[i] = new CheckBox(this);   		
    		m_arrChkFilename[i].setId(i);
    		m_arrChkFilename[i].setText(getFileName(m_lstUnUploadedFileName.get(i)));
    		m_arrChkFilename[i].setOnCheckedChangeListener(null);
    		m_arrChkFilename[i].setLayoutParams(m_lpFilenameChkParams);
    		m_llUploadData_ChkLayoutFileName.addView(m_arrChkFilename[i]);
    		m_arrChkFilename[i].setEnabled(true);
    	}    	
		
    }
    
		
	private void resetValues() {
		m_sOrient = ",,,";
		m_sGyro = ",,,";
		m_sAccl = ",,,";
		m_sLinearAccl = ",,,";
		m_sGravity = ",,,";
		m_sLight = ",";
		m_sBarometer = ",";

		m_sMagnet = ",,,";
				
		m_sSouldLevel = ",";
		m_sCellId = ",";
		
		m_sGPS = ",,,,,,,";
		m_sWifi = ",,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,";
		
		m_sSensorAccuracy=",,";
		
		m_blnGpsUp = false;
	}
		
	
    private class WifiReceiver extends BroadcastReceiver {
    	/* Record scanned Wifi information */
    	public void onReceive(Context c, Intent intent) {
    		int i,j, iCount,iRecordCount = 0;
    		WifiData wifiData = null;
    		WifiData tmpWifiData = null;
    		int nPos;

    		//PrintCurrentTime("WiFi Receiver");

    		m_lstWifiList = m_mainWifi.getScanResults();
    		    		
    		iCount = m_lstWifiList.size();
    		iRecordCount = iCount;

    		if (iRecordCount <= 0) return;
    		
    		List<WifiData> lstWifiData = new ArrayList<WifiData>();
    		
    		wifiData = new WifiData(m_lstWifiList.get(0).SSID, m_lstWifiList.get(0).BSSID, m_lstWifiList.get(0).level);
    		lstWifiData.add(wifiData); 
    		
    		for (i = 1; i < iRecordCount; i++) {
    			wifiData = new WifiData(m_lstWifiList.get(i).SSID, m_lstWifiList.get(i).BSSID, m_lstWifiList.get(i).level);
    			nPos = -1;
    			for (j=0; j < lstWifiData.size(); j++) {
    				tmpWifiData = lstWifiData.get(j);
    				if (m_lstWifiList.get(i).level > tmpWifiData.getSignalLevel()) {
    					nPos = j;
    					break;
    				}
    			}
    			
    			if (nPos == -1) {
    				lstWifiData.add(wifiData); 
    			} else {
    				lstWifiData.add(nPos, wifiData);
    			}
    		}
    		
        	SensorData senData = new SensorData(DATA_TYPE_WIFI, lstWifiData);
        	recordSensingInfo(senData);
        	
    	}
    }
	
	
	/* Set file identification and type 
	 * Add "_" + "O", "G", "A", "M", "P", "L" to represent each enabled sensor recorded in the data file
	 * Sensor data is recorded as "CSV" file 
	 */
	private String getFileType() {
		String sFileType = "";
		boolean blnHasUnderscore = false; //Indicate the first "_" before the letters representing sensors
		
				
//		if (m_blnAcclEnabled) {
//			sFileType = "_A";
//			blnHasUnderscore = true;
//		}

		if (m_blnAcclEnabled) {
			sFileType = "_A";
			blnHasUnderscore = true;
		}

		if (m_blnLinearAcclEnabled) {
			if (blnHasUnderscore == true) {
				sFileType = sFileType + "R";
			} else {
				sFileType = "_R";
				blnHasUnderscore = true;
			}
		}
		
		if (m_blnGravityEnabled) {
			if (blnHasUnderscore == true) {
				sFileType = sFileType + "V";
			} else {
				sFileType = "_V";
				blnHasUnderscore = true;
			}
		}
		
		if (m_blnGyroEnabled) {
			if (blnHasUnderscore == true) {
				sFileType = sFileType + "G";
			} else {
				sFileType = "_G";
				blnHasUnderscore = true;
			}
		}

		if (m_blnOrientEnabled) {
			if (blnHasUnderscore == true) {
				sFileType = sFileType + "O";
			} else {
				sFileType = "_O";
				blnHasUnderscore = true;
			}
		} 

		if (m_blnMagnetEnabled) {
			if (blnHasUnderscore == true) {
				sFileType = sFileType + "N";
			} else {
				sFileType = "_N";
				blnHasUnderscore = true;
			}
		} 
		
		if (m_blnLightEnabled) {
			if (blnHasUnderscore == true) {
				sFileType = sFileType + "L";
			} else {
				sFileType = "_L";
				blnHasUnderscore = true;
			}
		}

		if (m_blnBarometerEnabled) {
			if (blnHasUnderscore == true) {
				sFileType = sFileType + "B";
			} else {
				sFileType = "_B";
				blnHasUnderscore = true;
			}
		}
		
		if (m_blnMicEnabled) {
			if (blnHasUnderscore == true) {
				sFileType = sFileType + "M";
			} else {
				sFileType = "_M";
				blnHasUnderscore = true;
			}
		}

		if (m_blnCellularEnabled) {
			if (blnHasUnderscore == true) {
				sFileType = sFileType + "C";
			} else {
				sFileType = "_C";
				blnHasUnderscore = true;
			}
		}
		
		if (m_blnGPSEnabled) {
			if (blnHasUnderscore == true) {
				sFileType = sFileType + "S";
			} else {
				sFileType = "_S";
				blnHasUnderscore = true;
			}			
		}
		
		if (m_blnWifiEnabled) {
			if (blnHasUnderscore == true) {
				sFileType = sFileType + "W";
			} else {
				sFileType = "_W";
				blnHasUnderscore = true;
			}						
		}
		
		sFileType = sFileType + ".csv";
		
		return sFileType;
		
	}
	
	/* Enable/Disable sensor setting 
	 * When recording is going on, sensor setting should be disabled 
	 */
	private void enableSensorSetting(boolean blnSettingEnabled) {
		if (m_blnOrientPresent) {
			m_chkOrient.setEnabled(blnSettingEnabled);
			m_rdSensorOrient.setEnabled(blnSettingEnabled);
		} else {
			m_chkOrient.setEnabled(false);
			m_rdSensorOrient.setEnabled(false);			
		}
		
		if (m_blnGyroPresent) {
			m_chkGyro.setEnabled(blnSettingEnabled);
			m_rdSensorGyro.setEnabled(blnSettingEnabled);
		} else {
			m_chkGyro.setEnabled(false);
			m_rdSensorGyro.setEnabled(false);
		}
		
		if (m_blnAcclPresent) {
			m_chkAccl.setEnabled(blnSettingEnabled);
			m_rdSensorAccl.setEnabled(blnSettingEnabled);
		} else {
			m_chkAccl.setEnabled(false);
			m_rdSensorAccl.setEnabled(false);
		}

		if (m_blnLinearAcclPresent) {
			m_chkLinearAccl.setEnabled(blnSettingEnabled);
			m_rdSensorLinearAccl.setEnabled(blnSettingEnabled);
		} else {
			m_chkLinearAccl.setEnabled(false);
			m_rdSensorLinearAccl.setEnabled(false);
		}
	
		if (m_blnGravityPresent) {
			m_chkGravity.setEnabled(blnSettingEnabled);
			m_rdSensorGravity.setEnabled(blnSettingEnabled);
		} else {
			m_chkGravity.setEnabled(false);
			m_rdSensorGravity.setEnabled(false);
		}
		
		if (m_blnMagnetPresent) {
			m_chkMagnet.setEnabled(blnSettingEnabled);
			m_rdSensorMagnet.setEnabled(blnSettingEnabled);
		} else {
			m_chkMagnet.setEnabled(false);
			m_rdSensorMagnet.setEnabled(false);
		}
		
		if (m_blnLightPresent){
			m_chkLight.setEnabled(blnSettingEnabled);
			m_rdSensorLight.setEnabled(blnSettingEnabled);
		} else {
			m_chkLight.setEnabled(false);
			m_rdSensorLight.setEnabled(false);			
		}

		if (m_blnBarometerPresent){
			m_chkBarometer.setEnabled(blnSettingEnabled);
			m_rdSensorBarometer.setEnabled(blnSettingEnabled);
		} else {
			m_chkBarometer.setEnabled(false);
			m_rdSensorBarometer.setEnabled(false);			
		}
		
		if (m_blnMicEnabled) {
			m_chkMic.setEnabled(blnSettingEnabled);
		} else {
			m_chkMic.setEnabled(false);
		}
		
		if (m_blnCellularEnabled) {
			m_chkCellular.setEnabled(blnSettingEnabled);
		} else {
			m_chkCellular.setEnabled(false);
		}
		
		if (m_blnWifiSignalEnabled){
			m_chkWifi.setEnabled(blnSettingEnabled);
		} else {
			m_chkWifi.setEnabled(false);
		}
		
		if (m_blnGPSSignalEnabled) {
			m_chkGPS.setEnabled(blnSettingEnabled);
		} else {
			m_chkGPS.setEnabled(false);
		}
		
		if (m_blnOrientPresent || 
			m_blnGyroPresent ||
			m_blnAcclPresent ||
			m_blnLinearAcclPresent ||
			m_blnGravityPresent ||
			m_blnMagnetPresent ||
			m_blnLightPresent ||
			m_blnBarometerPresent) {
			
			m_rdSensorModeFastest.setEnabled(blnSettingEnabled);
			m_rdSensorModeGame.setEnabled(blnSettingEnabled);
			m_rdSensorModeNormal.setEnabled(blnSettingEnabled);
			m_rdSensorModeUI.setEnabled(blnSettingEnabled);
		} else {
			//No sensor is installed, disable mode selection
			m_rdSensorModeFastest.setEnabled(false);
			m_rdSensorModeGame.setEnabled(false);
			m_rdSensorModeNormal.setEnabled(false);
			m_rdSensorModeUI.setEnabled(false);
			
		}
	
	}


	/* Set default widget status and setting */
    private void setDefaultStatus() {
    	m_blnOrientEnabled = false;
    	m_blnGyroEnabled = false;
    	m_blnAcclEnabled = false;
    	m_blnLinearAcclEnabled = false;
    	m_blnGravityEnabled = false;
    	m_blnMagnetEnabled = false;
    	m_blnLightEnabled = false;
    	m_blnBarometerEnabled = false;
    	
    	m_blnWifiEnabled  = false;
    	m_blnGPSEnabled = false;
    	
    	m_blnMicEnabled = false;
    	m_blnCellularEnabled = false;

    	m_nOrientMode = SensorManager.SENSOR_DELAY_FASTEST;
    	m_nGyroMode = SensorManager.SENSOR_DELAY_FASTEST;
    	m_nAcclMode = SensorManager.SENSOR_DELAY_FASTEST;
    	m_nLinearAcclMode = SensorManager.SENSOR_DELAY_FASTEST;
    	m_nGravityMode = SensorManager.SENSOR_DELAY_FASTEST;
    	m_nMagnetMode = SensorManager.SENSOR_DELAY_FASTEST;
    	m_nLightMode = SensorManager.SENSOR_DELAY_FASTEST;
    	m_nBarometerMode = SensorManager.SENSOR_DELAY_FASTEST;
    	
    	m_chkOrient.setChecked(false);
    	m_chkGyro.setChecked(false);
    	m_chkAccl.setChecked(false);
    	m_chkLinearAccl.setChecked(false);
    	m_chkGravity.setChecked(false);    	
    	m_chkMagnet.setChecked(false);
    	m_chkLight.setChecked(false);
    	m_chkBarometer.setChecked(false);
    	
    	m_chkWifi.setChecked(false);
    	m_chkGPS.setChecked(false);
    	
    	enableSensorSetting(true);
    	m_rdSensorOrient.setChecked(true);
    	m_rdSensorModeNormal.setChecked(true); 
    	m_blnRecordStatus = false;
    }
	    
    
	/* Event for sensor enable/disable */
	private OnCheckedChangeListener m_chkSensorEnableListener = new OnCheckedChangeListener() {
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			// TODO Auto-generated method stub
			if (buttonView == m_chkOrient) {
				if (isChecked == true) {
					m_blnOrientEnabled = true;
				} else {
					m_blnOrientEnabled = false;
				}
			}
			
			if (buttonView == m_chkGyro) {
				if (isChecked == true) {
					m_blnGyroEnabled = true;
				} else {
					m_blnGyroEnabled = false;
				}
			}
			
			if (buttonView == m_chkAccl) {
				if (isChecked == true) {
					m_blnAcclEnabled = true;
				} else {
					m_blnAcclEnabled = false;
				}
			}

			if (buttonView == m_chkLinearAccl) {
				if (isChecked == true) {
					m_blnLinearAcclEnabled = true;
				} else {
					m_blnLinearAcclEnabled = false;
				}
			}
			
			if (buttonView == m_chkGravity) {
				if (isChecked == true) {
					m_blnGravityEnabled = true;
				} else {
					m_blnGravityEnabled = false;
				}
			}
			
			
			if (buttonView == m_chkMagnet) {
				if (isChecked == true) {
					m_blnMagnetEnabled = true;
				} else {
					m_blnMagnetEnabled = false;
					if (m_blnOrientEnabled == false) {
					}
				}
			}
			
			if (buttonView == m_chkLight) {
				if (isChecked == true) {
					m_blnLightEnabled = true;
				} else {
					m_blnLightEnabled = false;	
				}
			}
			
			if (buttonView == m_chkBarometer) {
				if (isChecked == true) {
					m_blnBarometerEnabled = true;
				} else {
					m_blnBarometerEnabled = false;	
				}
			}
						
			if (buttonView == m_chkMic) {
				if (isChecked == true) {
					m_blnMicEnabled = true;
				} else {
					m_blnMicEnabled = false;
				}
			}

			if (buttonView == m_chkCellular) {
				if (isChecked == true) {
					m_blnCellularEnabled = true;
				} else {
					m_blnCellularEnabled = false;
				}
			}
			
			if (buttonView == m_chkWifi) {
				if (isChecked == true) {
					m_blnWifiEnabled = true;
				} else {
					m_blnWifiEnabled = false;
				}
			}
			
			if (buttonView == m_chkGPS) {
				if (isChecked == true) {
					m_blnGPSEnabled = true;
				} else {
					m_blnGPSEnabled = false;
				}
			}
										
		}			
	};
	
	    
    /* Event listener for sensor radiogroup selection */
    private RadioGroup.OnCheckedChangeListener m_rdgpSensorListener = new RadioGroup.OnCheckedChangeListener() {
		@Override
		public void onCheckedChanged(RadioGroup group, int checkedId) {
			// TODO Auto-generated method stub
			int iModeType = SensorManager.SENSOR_DELAY_FASTEST;
			
			if (m_rdSensorOrient.isChecked()) {
				iModeType = m_nOrientMode;
			} else if (m_rdSensorGyro.isChecked()) {
				iModeType = m_nGyroMode;
			} else if (m_rdSensorAccl.isChecked()) {
				iModeType = m_nAcclMode;
			} else if (m_rdSensorLinearAccl.isChecked()) {
				iModeType = m_nLinearAcclMode;
			} else if (m_rdSensorGravity.isChecked()) {
				iModeType = m_nGravityMode;				
			} else if (m_rdSensorMagnet.isChecked()) {
				iModeType = m_nMagnetMode;
			} else if (m_rdSensorLight.isChecked()) {
				iModeType = m_nLightMode;
			} else if (m_rdSensorBarometer.isChecked()) {
				iModeType = m_nBarometerMode;
			}
			
			if (iModeType == SensorManager.SENSOR_DELAY_FASTEST) {
				m_rdSensorModeFastest.setChecked(true);
			} else if (iModeType == SensorManager.SENSOR_DELAY_GAME) {
				m_rdSensorModeGame.setChecked(true);
			} else if (iModeType == SensorManager.SENSOR_DELAY_NORMAL) {
				m_rdSensorModeNormal.setChecked(true);
			} else if (iModeType == SensorManager.SENSOR_DELAY_UI) {
				m_rdSensorModeUI.setChecked(true);
			}

		}
    };
    
    /* Event listener for sensor mode radiogroup selection */
    private RadioGroup.OnCheckedChangeListener m_rdgpSensorModeListener = new RadioGroup.OnCheckedChangeListener() {
		@Override
		public void onCheckedChanged(RadioGroup group, int checkedId) {
				// TODO Auto-generated method stub
			int iDelayMode = SensorManager.SENSOR_DELAY_FASTEST;
				
			if (m_rdSensorModeFastest.isChecked()) {
				iDelayMode = SensorManager.SENSOR_DELAY_FASTEST;
			} else if (m_rdSensorModeGame.isChecked()) {
				iDelayMode = SensorManager.SENSOR_DELAY_GAME;	
			} else if (m_rdSensorModeNormal.isChecked()) {
				iDelayMode = SensorManager.SENSOR_DELAY_NORMAL;
			} else if (m_rdSensorModeUI.isChecked()) {
				iDelayMode = SensorManager.SENSOR_DELAY_UI;
			}

			if (m_rdSensorOrient.isChecked()) {
				m_nOrientMode = iDelayMode;
			} else if (m_rdSensorGyro.isChecked()) {
				m_nGyroMode = iDelayMode;
			} else if (m_rdSensorAccl.isChecked()) {
				m_nAcclMode = iDelayMode;
			} else if (m_rdSensorLinearAccl.isChecked()) {
				m_nLinearAcclMode = iDelayMode;
			} else if (m_rdSensorGravity.isChecked()) {
				m_nGravityMode = iDelayMode;				
			} else if (m_rdSensorMagnet.isChecked()){
				m_nMagnetMode = iDelayMode;
			} else if (m_rdSensorLight.isChecked()) {
				m_nLightMode = iDelayMode;
			} else if (m_rdSensorBarometer.isChecked()) {
				m_nBarometerMode = iDelayMode;
			}
								
		}
    };

     /* Get GPS provider */
    private boolean getGPSProvider() {
    	Location location = null;
 		Criteria crit = new Criteria();
 		float fLat,fLng,fAlt;

		m_sGPSProvider = m_locManager.getBestProvider(crit, true); //false?
		if (m_sGPSProvider != null) {
			m_blnGPSSignalEnabled = true;
			location = m_locManager.getLastKnownLocation(m_sGPSProvider);
			if (location != null ) {
				fLat = (float)(location.getLatitude());
				fLng = (float)(location.getLongitude());
				if (location.hasAltitude()) {
					fAlt = (float)(location.getAltitude());
				}
			}
			return true;
		} else {
			m_chkGPS.setEnabled(false);
			return false;
		} 
    }
     
    private LocationListener m_locListener = new LocationListener() {
    	public void onLocationChanged(Location location) {
    		if (location != null) {    		
    			recordLocation(location);
    		} else {
    			m_blnGpsUp = false;
    			show_screen5_GpsUp();
    		}
    	}

    	public void onProviderDisabled(String provider) {    		 
    		if (provider.equals(m_sGPSProvider)) {
    			m_blnGPSSignalEnabled = false;
    			m_chkGPS.setEnabled(false);   					 
    		}
    	}
    	 
    	public void onProviderEnabled(String provider) {
    		if (provider.equals(m_sGPSProvider)) {
    			m_blnGPSSignalEnabled = true;
    			if (m_blnRecordStatus == false) {
    				m_chkGPS.setEnabled(true);
    			}
    		} 
     	}
    	 
    	public void onStatusChanged(String provider, int status, Bundle extras) {
    		if (provider.equals(m_sGPSProvider)) {
    			if (status == LocationProvider.OUT_OF_SERVICE) {
    				m_blnGPSSignalEnabled = false;
    				m_chkGPS.setEnabled(false);
    				
    				m_blnGpsUp = false;
        			show_screen5_GpsUp();
    			} else {
    				m_blnGPSSignalEnabled = true;
    				if (m_blnRecordStatus == false) {
    					m_chkGPS.setEnabled(true);
    				}
    			}
    		} else {
				m_blnGpsUp = false;
    			show_screen5_GpsUp();   			
    		}
    	}
    	 
    };
               
     
    private Button.OnClickListener m_btnScreen1_Next_Listener = new Button.OnClickListener() {
    	public void onClick(View v) {
    		int nIndex = 0;
    		String sTmpProjectName;
    		String sTmpUsername;
    		int nPos = -1;
    		String sLine;
    		 
    		if (m_rdProjectNameType_New.isChecked() == false && m_rdProjectNameType_Existing.isChecked() == false) {
    			Toast.makeText(getApplicationContext(), "Please set project name", Toast.LENGTH_SHORT).show();
    			return;
    		}
    		 
    		if (m_rdUsernameType_New.isChecked() == false && m_rdUsernameType_Existing.isChecked() == false) {
    			Toast.makeText(getApplicationContext(), "Please set username", Toast.LENGTH_SHORT).show();
    			return;
    		}
    		     		 
    		//Process project name
    		if (m_rdProjectNameType_New.isChecked()) {
    			m_nProjectNameType = 0;
    			m_sProjectName = m_etProjectName_New.getText().toString();
    			 
    			if (m_sProjectName.length() == 0) {
    				Toast.makeText(getApplicationContext(), "Please input project name!", Toast.LENGTH_SHORT).show();
    				return;
    			}
    			
    			if (containComma(m_sProjectName)) {
    				Toast.makeText(getApplicationContext(), "Comma should not be used in project name!", Toast.LENGTH_SHORT).show();
    				return;    				
    			}
    			
    			     			 
    		} else if (m_rdProjectNameType_Existing.isChecked()) {
    			m_nProjectNameType = 1;
    			nIndex = m_spnScreen1_ExistingProjectName.getSelectedItemPosition();
    			m_sProjectName = m_lstExistingProjectName.get(nIndex); 			 
    		}
    		 
    		//Process username
    		if (m_rdUsernameType_New.isChecked()) {
    			m_nUsernameType = 0;
    			m_sUsername = m_etUsername_New.getText().toString();
    			 
    			if (m_sUsername.length() == 0) {
    				Toast.makeText(getApplicationContext(), "Please input username!", Toast.LENGTH_SHORT).show();
    				return;
    			}
   
    			if (containComma(m_sUsername)) {
    				Toast.makeText(getApplicationContext(), "Comma should not be used in user name!", Toast.LENGTH_SHORT).show();
    				return;    				
    			}
    			    			 
    		} else if (m_rdUsernameType_Existing.isChecked()) {
    			m_nUsernameType = 1;
    			nIndex = m_spnScreen1_ExistingUsername.getSelectedItemPosition();
    			m_sUsername = m_lstExistingUsername.get(nIndex);
    		}
    	 
    		if (m_nProjectNameType == 0) {
    			//Add the new Project name into m_lstExistingProjectName
    			nIndex = m_lstExistingProjectName.indexOf(m_sProjectName);
    			if (nIndex != -1) { //Exist
    				 
    			} else {
    				for (int i=0; i<m_lstExistingProjectName.size(); i++) {
    					sTmpProjectName = m_lstExistingProjectName.get(i);
    					if (m_sProjectName.compareToIgnoreCase(sTmpProjectName) < 0) {
    						nPos = i;
    						break;
    					}
    				}
    				 
    				if (nPos != -1) {
    					m_lstExistingProjectName.add(nPos, m_sProjectName);
    				} else {
    					m_lstExistingProjectName.add(m_sProjectName);
    				}
    				 
    			}
    			 
    			//Update the ExistingProjectName file
    			try {
    				String sExistingProjectNameFilePath = m_sSettingFoler + File.separator + m_sExistingProjectNameFileName;

    				FileWriter fwExistingProjectName = new FileWriter(sExistingProjectNameFilePath, false);   //Overwrite
    				 
    				for (String sProjectName : m_lstExistingProjectName) {
    					sLine = sProjectName + "\n";
    					fwExistingProjectName.write(sLine);
    				}
    				 
    				fwExistingProjectName.close();
    			} catch (Exception e) {
    				 
    			}
    			 
    		}
    		 
    		if (m_nUsernameType == 0) {
    			//Add the new username into m_lstExistingUsername
    			nIndex = m_lstExistingUsername.indexOf(m_sUsername);
    			if (nIndex != -1) { //Exist, do not add
    				 
    			} else {
    				for (int i=0; i<m_lstExistingUsername.size(); i++) {
    					sTmpUsername = m_lstExistingUsername.get(i);
    					if (m_sUsername.compareToIgnoreCase(sTmpUsername) < 0) {
    						nPos = i;
    						break;
    					}
    				}
    				 
    				if (nPos != -1) {
    					m_lstExistingUsername.add(nPos, m_sUsername);
    				} else {
    					m_lstExistingUsername.add(m_sUsername);
    				}
    				 
    			}
    			 
    			//Update the ExistingUsername file
    			try {
    				String sExistingUsernameFilePath = m_sSettingFoler + File.separator + m_sExistingUsernameFileName;

    				FileWriter fwExistingUsername = new FileWriter(sExistingUsernameFilePath, false);   //Overwrite
    				 
    				for (String sUsername : m_lstExistingUsername) {
    					sLine = sUsername + "\n";
    					fwExistingUsername.write(sLine);
    				}
    				 
    				fwExistingUsername.close();
    			} catch (Exception e) {
    				 
    			}
    			 
    		}
    		 
    		show_screen2();
    	}
    };
     
     
    private Button.OnClickListener m_btnScreen1_Upload_Listener = new Button.OnClickListener() {
    	public void onClick(View v) {
    		if (m_blnUploadFunctionEnabled) {
    			m_nPreviousScreen = SCREEN_1;
    			show_screenUploadData();
    		}
    		
    	}
    };
    
    private Button.OnClickListener m_btnScreen2_Back_Listener = new Button.OnClickListener() {
    	public void onClick(View v) {
			m_nWifiScanSpeedIndex = m_spnScreen2_WiFiScanSpeed.getSelectedItemPosition();
    		show_screen1();
    	}
    };
     
     
    private Button.OnClickListener m_btnScreen2_Next_Listener = new Button.OnClickListener() {
    	public void onClick(View v) {
    		//Check whether any sensor is selected   		
    		if ((m_blnAcclEnabled == false) && 
    			(m_blnLinearAcclEnabled == false) &&
    			(m_blnGravityEnabled == false) &&    			
    			(m_blnGyroEnabled == false) && 
    			(m_blnOrientEnabled == false) &&
    			(m_blnMagnetEnabled == false) &&
    			(m_blnLightEnabled == false) && 
    			(m_blnBarometerEnabled == false) && 
    			(m_blnWifiEnabled == false) &&
    			(m_blnGPSEnabled == false) &&
    			(m_blnMicEnabled == false) &&
    			(m_blnCellularEnabled == false)) {
    			
    			Toast.makeText(getApplicationContext(), "Please select sensor!", Toast.LENGTH_SHORT).show();
    			return;
    		}
    		
    		UpdateSensorServiceRegistration();
    		
			m_nWifiScanSpeedIndex = m_spnScreen2_WiFiScanSpeed.getSelectedItemPosition();
    		
    		show_screen3();
    	}
    };
     
           
    private Button.OnClickListener m_btnScreen3_AddLabel_Listener = new Button.OnClickListener() {
    	public void onClick(View v) {
    		String sLabelName = m_etSensorDataLabel.getText().toString();
    		String sTmpLabel = "";
    		int nIndex = -1;
    		int nLast;
    		String sLabelList = "";
    		
    		if (sLabelName.length() > 0) {
    			
    			if (containComma(sLabelName)) {
    				Toast.makeText(getApplicationContext(), "Comma should not be used in label!", Toast.LENGTH_SHORT).show();
    				return;    				
    			}
		
    			for (int i=0; i<m_lstSensorDataLabel.size(); i++) {
    				sTmpLabel = m_lstSensorDataLabel.get(i);
    				if (sLabelName.compareToIgnoreCase(sTmpLabel) == 0) {
    					nIndex = -2;  //Same label exists
    					break;
    				}
    				
    				if (sLabelName.compareToIgnoreCase(sTmpLabel) < 0) {
    					nIndex = i;
    					break;
    				}
    			}
    			
    			if (nIndex == -2) {  
    				//Same label exists, do nothing
    			} else if (nIndex == -1) {
    				m_lstSensorDataLabel.add(sLabelName);
    			} else {
    				m_lstSensorDataLabel.add(nIndex, sLabelName);
    			}
    		}
    		
    		nLast = m_lstSensorDataLabel.size() - 1;
    		for (int i=0; i<m_lstSensorDataLabel.size(); i++) {
    			if (i != nLast) {
    				sLabelList = sLabelList + m_lstSensorDataLabel.get(i) + "\n";
    			} else {
    				sLabelList = sLabelList + m_lstSensorDataLabel.get(i);
    			}
    		}
    		
    		m_etScreen3LabelList.setText(sLabelList);
    	
    		m_etSensorDataLabel.setText("");
    	} 
    };
     

    //Update Sensor Data file
    private void updateSensorDataLabel() {
    	 String sLine;
    	 String sLabels;
    	 boolean bExist;
    	 int nIndex;
    	 
    	 sLabels = m_etScreen3LabelList.getText().toString();
    	 
    	 String sArrLabel[] = sLabels.split("\n");
    	 
    	 m_lstSensorDataLabel.clear();
    	 
    	 for (int i=0; i<sArrLabel.length; i++) {
    		 bExist = false;
    		 nIndex = -1;
    		 
    		 for (int j=0; j<m_lstSensorDataLabel.size(); j++) {
    			 if (sArrLabel[i].compareToIgnoreCase(m_lstSensorDataLabel.get(j)) == 0) {
    				 bExist = true;
    				 break;
    			 }
    			 
    			 if (sArrLabel[i].compareToIgnoreCase(m_lstSensorDataLabel.get(j)) < 0) {
    				 bExist = false;
    				 nIndex = j;
    			 }
    		 }
    		 
    		 if (bExist == false) {
    			 if (nIndex == -1) {
    				 m_lstSensorDataLabel.add(sArrLabel[i]);
    			 } else {
    				 m_lstSensorDataLabel.add(nIndex, sArrLabel[i]);
    			 }
    		 }
    		 
    	 }    	 
    	 
		 try {
			 String sExistingLabelFilePath = m_sSettingFoler + File.separator + m_sExistingLabelFileName;

			 FileWriter fwExistingLabel = new FileWriter(sExistingLabelFilePath, false);   //Overwrite
			 
			 for (String sLabel : m_lstSensorDataLabel) {
				 sLine = sLabel + "\n";
				 fwExistingLabel.write(sLine);
			 }
			 
			 fwExistingLabel.close();
		 } catch (Exception e) {
			 
		 }
    	 
    }
     
     
    private Button.OnClickListener m_btnScreen3_Back_Listener = new Button.OnClickListener() {
    	public void onClick(View v) {
    		updateSensorDataLabel();
    		
    		show_screen2();
    	}
    };
     
          
    private Button.OnClickListener m_btnScreen3_Next_Listener = new Button.OnClickListener() {
    	public void onClick(View v) {    		
    		updateSensorDataLabel();
    		
    		show_screen4();
    	}
    };
     
    private Button.OnClickListener m_btnScreen4_Back_Listener = new Button.OnClickListener() {
    	public void onClick(View v) {
    		show_screen3();
    	}
    };
     
    private OnCheckedChangeListener m_chkNoLabel_Listener = new OnCheckedChangeListener() {
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			 if (m_chkNoLabel.isChecked()) {
				 m_blnNoLabel = true;
			 } else {
				 m_blnNoLabel = false;
			 }
		}
	};
  
	
    //Process when the user uses temp label during sensing
 	private OnCheckedChangeListener m_chkScreen5_UseTempLabelListener = new OnCheckedChangeListener() {
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			
			if (m_chkScreen5_UseTempLabel.isChecked()) {
				String sTempLabel = m_etScreen5_TempLabel.getText().toString();
				if (sTempLabel.length() == 0) {
					m_chkScreen5_UseTempLabel.setChecked(false);
					m_arrLabelButton[m_nCurrentLabelIndex].setChecked(true);
					m_sCurrentLabel = m_lstSensorDataLabel.get(m_nCurrentLabelIndex);
					Toast.makeText(getApplicationContext(), "Please input label", Toast.LENGTH_SHORT).show();
				} else {
	    			if (containComma(sTempLabel)) {
	    				Toast.makeText(getApplicationContext(), "Comma should not be used in label!", Toast.LENGTH_SHORT).show();
	    				m_chkScreen5_UseTempLabel.setChecked(false);
	    				return;    				
	    			}

					m_sCurrentLabel = sTempLabel;
					m_lstUsedSensorDateLabel.add(m_sCurrentLabel);
					m_arrLabelButton[m_nCurrentLabelIndex].setChecked(false);
					m_chkScreen5_UseTempLabel.setChecked(true);
				}
			} else {
				m_arrLabelButton[m_nCurrentLabelIndex].setChecked(true);
				m_sCurrentLabel = m_lstSensorDataLabel.get(m_nCurrentLabelIndex);
			}
		}
	};
     
	   
	
    /* Event listener for sensor radiogroup selection */
    private RadioGroup.OnCheckedChangeListener m_rdgpScreen5_LabelRadioButtonListener = new RadioGroup.OnCheckedChangeListener() {
		@Override
		public void onCheckedChanged(RadioGroup group, int checkedId) {
			// TODO Auto-generated method stub
			for (int i=0; i<m_lstSensorDataLabel.size(); i++) {
				if (m_arrLabelButton[i].isChecked()) {
					m_nCurrentLabelIndex = i;
					m_sCurrentLabel = m_lstSensorDataLabel.get(i);
					m_lstUsedSensorDateLabel.add(m_sCurrentLabel);
					break;
				}
			}
			
			m_chkScreen5_UseTempLabel.setChecked(false);
		}
	};
	 
    private Button.OnClickListener m_btnScreen6_Back_Listener = new Button.OnClickListener() {
    	public void onClick(View v) {
    		show_screen4();
    	}
    };
     
     
    private Button.OnClickListener m_btnScreen6_Upload_Listener = new Button.OnClickListener() {
    	public void onClick(View v) {
    		
    		if (m_blnUploadFunctionEnabled) {
    		
    			m_nPreviousScreen = SCREEN_6;
    			show_screenUploadData();
    		}
    		
    	}
    };

    
    //Processing for uploading data
    private Button.OnClickListener m_btnScreenUploadData_Upload_Listener = new Button.OnClickListener() {
    	public void onClick(View v) {
    		if (m_blnUpload == false) {
    			if (m_lstUnUploadedFileName.size() == 0) return;
    				
	    		List<String> lstSelectedUploadFile = new ArrayList<String>();
	    		List<Integer> lstSelectedUploadFileID = new ArrayList<Integer>();
	    		
	    		for (int i=0; i<m_lstUnUploadedFileName.size(); i++) {
	    			if (m_arrChkFilename[i].isChecked() == true) {
	    				lstSelectedUploadFile.add(m_lstUnUploadedFileName.get(i));
	    				lstSelectedUploadFileID.add(i);
	    			}
	    		}
	    		
	    		if (lstSelectedUploadFile.size() == 0) {
	    			Toast.makeText(getApplicationContext(), "Please select file to upload!", Toast.LENGTH_SHORT).show();
	    			return;
	    		}
	    		
	    		m_btnScreenUploadData_Back.setEnabled(false);
	    		m_pgbActivityIndicator.setVisibility(View.VISIBLE);
	    		for (int i=0; i<m_lstUnUploadedFileName.size(); i++) {
	    			m_arrChkFilename[i].setEnabled(false);
	    		}
	    		m_btnScreenUploadData_Upload.setText(R.string.cancelupload);
	    		
	    		m_blnUpload = true;
	    		m_lstUploadedFileNameIndex.clear();
	    		
	    		new SensorDataUpload(lstSelectedUploadFile, lstSelectedUploadFileID).execute();
	    		
    		} else {  			

    			//Update the UnUploadedFileName.txt
    			for (int i=m_lstUploadedFileNameIndex.size()-1; i>=0; i--) {
    				m_lstUnUploadedFileName.remove(m_lstUploadedFileNameIndex.get(i));
    				
    				m_llUploadData_ChkLayoutFileName.removeViewAt(m_lstUploadedFileNameIndex.get(i));
    			}
    			
    			updateUnUploadedFileName(m_lstUnUploadedFileName);

    			refreshUploadFileNameBox();
    			
    			m_btnScreenUploadData_Back.setEnabled(true);
	    		m_pgbActivityIndicator.setVisibility(View.GONE);

    			m_btnScreenUploadData_Upload.setText(R.string.upload);
  				    		
	    		m_blnUpload = false;
    			
    		}
    	}
    };
     
     
    private Button.OnClickListener m_btScreenUploadData_Back_Listener = new Button.OnClickListener() {
    	public void onClick(View v) {
    		if (m_nPreviousScreen == SCREEN_1) {
    			show_screen1();
    		} else {
    			show_screen4();
    		}
    	}
    };
     
     
    private void UpdateSensorServiceRegistration() {
		//Register sensors according to the enable/disable status
//	 	if (m_blnAcclEnabled) {
//	 		m_smSurScan.registerListener(m_actHome, m_smSurScan.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION), m_nAcclMode);
//	 		m_smSurScan.registerListener(m_actHome, m_smSurScan.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), m_nAcclMode);
//	 	} else {
//	 		m_smSurScan.unregisterListener(m_actHome, m_smSurScan.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION));
//	 		m_smSurScan.unregisterListener(m_actHome, m_smSurScan.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
//	 	}

	 	if (m_blnAcclEnabled) {
	 		m_smSurScan.registerListener(m_actHome, m_smSurScan.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), m_nAcclMode);
	 	} else {
	 		m_smSurScan.unregisterListener(m_actHome, m_smSurScan.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
	 	}

	 	if (m_blnLinearAcclEnabled) {
	 		m_smSurScan.registerListener(m_actHome, m_smSurScan.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION), m_nLinearAcclMode);
	 	} else {
	 		m_smSurScan.unregisterListener(m_actHome, m_smSurScan.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION));
	 	}

	 	if (m_blnGravityEnabled) {
	 		m_smSurScan.registerListener(m_actHome, m_smSurScan.getDefaultSensor(Sensor.TYPE_GRAVITY), m_nGravityMode);
	 	} else {
	 		m_smSurScan.unregisterListener(m_actHome, m_smSurScan.getDefaultSensor(Sensor.TYPE_GRAVITY));
	 	}
	 	
	 	if (m_blnGyroEnabled) {
	 		m_smSurScan.registerListener(m_actHome, m_smSurScan.getDefaultSensor(Sensor.TYPE_GYROSCOPE), m_nGyroMode);
	 	} else {
	 		m_smSurScan.unregisterListener(m_actHome, m_smSurScan.getDefaultSensor(Sensor.TYPE_GYROSCOPE));
	 	}
	 
	 	if (m_blnMagnetEnabled) {
	 		m_smSurScan.registerListener(m_actHome, m_smSurScan.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), m_nMagnetMode);
	 	} else {
	 		m_smSurScan.unregisterListener(m_actHome, m_smSurScan.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD));
	 	}

// Changed on 20150910
//	 	if (m_blnOrientEnabled) {
//			if (!m_blnAcclEnabled) { 
//				m_smSurScan.registerListener(m_actHome, m_smSurScan.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), m_nOrientMode);
//			}
//			
//			if (!m_blnMagnetEnabled) {
//				m_smSurScan.registerListener(m_actHome, m_smSurScan.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), m_nOrientMode);
//			}    	 		
//	 	} else {
//	 		if (!m_blnAcclEnabled) {
//	 			m_smSurScan.unregisterListener(m_actHome, m_smSurScan.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
//	 		}
//	 		
//	 		if (!m_blnMagnetEnabled) {
//	 			m_smSurScan.unregisterListener(m_actHome, m_smSurScan.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD));
//	 		}
//	 	}

	 	if (m_blnOrientEnabled) {
	 		m_smSurScan.registerListener(m_actHome, m_smSurScan.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR), m_nOrientMode);
	 	} else {
	 		m_smSurScan.unregisterListener(m_actHome, m_smSurScan.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR));
	 	}
	 	
	 	
	 	if (m_blnLightEnabled) {
	 		m_smSurScan.registerListener(m_actHome, m_smSurScan.getDefaultSensor(Sensor.TYPE_LIGHT), m_nLightMode);
	 	} else {
	 		m_smSurScan.unregisterListener(m_actHome, m_smSurScan.getDefaultSensor(Sensor.TYPE_LIGHT));
	 	}
	 	
		if (m_blnBarometerEnabled) {
			m_smSurScan.registerListener(m_actHome, m_smSurScan.getDefaultSensor(Sensor.TYPE_PRESSURE), m_nBarometerMode);
		} else {
			m_smSurScan.unregisterListener(m_actHome, m_smSurScan.getDefaultSensor(Sensor.TYPE_PRESSURE));
		}
	 				    	 
    }
     
         
	/* Event listener for Start Record button on Screen 4 */
	private Button.OnClickListener m_btnStartRecordListener = new Button.OnClickListener() {
		public void onClick(View v) {
			String sDataDir;
			File flDataFolder;
			boolean blnSensorSelected = false;
			boolean blnWifiSelected = false;

			if (m_blnRecordStatus == false) {

				if ((m_blnOrientEnabled == false)
						&& (m_blnGyroEnabled == false)
						&& (m_blnAcclEnabled == false)
						&& (m_blnLinearAcclEnabled == false)
						&& (m_blnGravityEnabled == false)						
						&& (m_blnMagnetEnabled == false)
						&& (m_blnLightEnabled == false)
						&& (m_blnBarometerEnabled == false)) {
					// No sensor has been selected
					blnSensorSelected = false;
				} else {
					blnSensorSelected = true;
				}

				if ((m_blnWifiSignalEnabled == true)
						&& (m_blnWifiEnabled == true)) {
					blnWifiSelected = true;
					// Start scanning Wifi
					m_receiverWifi = new WifiReceiver();
					registerReceiver(m_receiverWifi, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
					//m_mainWifi.startScan();
					
					startWiFiScan();
					
				}

				if (m_blnGPSSignalEnabled == true && m_blnGPSEnabled == true) {
					m_locManager.requestLocationUpdates(m_sGPSProvider, 0L, 0.0f, m_locListener);
				}

				if (blnSensorSelected == false
						&& blnWifiSelected == false
						&& m_blnMicEnabled == false
						&& m_blnCellularEnabled == false
						&& (m_blnGPSSignalEnabled == false || m_blnGPSEnabled == false)) {

					return;
				}

				// Start to record
				m_dtFileStart = new Date();
				final String DATE_FORMAT = "yyyyMMddHHmmss";
				final String DATE_FORMAT_READABLE = "yyyy/MM/dd HH:mm:ss";
				SimpleDateFormat spdCurrentTime = new SimpleDateFormat(DATE_FORMAT);
				SimpleDateFormat spdCurrentTimeReadable = new SimpleDateFormat(DATE_FORMAT_READABLE);

				if (blnSensorSelected || m_blnGPSEnabled || blnWifiSelected || m_blnMicEnabled || m_blnCellularEnabled) {
					m_sRecordFile = spdCurrentTime.format(m_dtFileStart);
					m_sSensingStartTime = spdCurrentTimeReadable.format(m_dtFileStart);
				}

				if (blnSensorSelected || m_blnGPSEnabled || blnWifiSelected || m_blnMicEnabled || m_blnCellularEnabled) {
					m_sFullPathFile = m_sSensorDataFolder + File.separator;

					if (m_sUsername.length() > 0) {
						m_sFullPathFile = m_sFullPathFile + m_sUsername + "-";
					}

					if (m_sProjectName.length() > 0) {
						m_sFullPathFile = m_sFullPathFile + m_sProjectName + "-";
					}

					m_sFullPathFile = m_sFullPathFile + m_sRecordFile;

					// Append file type information to the file name
					m_sFullPathFile = m_sFullPathFile + getFileType();

					try {
						m_fwSensorRecord = new FileWriter(m_sFullPathFile);
					} catch (IOException e) {
						m_fwSensorRecord = null;
						return;
					}

				}

				if (m_blnMicEnabled) {

					if (m_blnRecordSoundFile == true) {
						m_sRecordFullPathFile = m_sSensorDataFolder + File.separator;

						if (m_sUsername.length() > 0) {
							m_sRecordFullPathFile = m_sRecordFullPathFile + m_sUsername + "-";
						}

						if (m_sProjectName.length() > 0) {
							m_sRecordFullPathFile = m_sRecordFullPathFile + m_sProjectName + "-";
						}

						m_sRecordFullPathFile = m_sRecordFullPathFile + m_sRecordFile;
					}

					startAudioRecording();
				}

				if (m_blnCellularEnabled) {
					startGettingCellularNetworkInfo();
				}

				// Disable setting for sensor when recording
				enableSensorSetting(false);

				m_nCurrentLabelIndex = m_spnScreen4_SelectSensorDataLabel.getSelectedItemPosition();
				m_sCurrentLabel = m_lstSensorDataLabel.get(m_nCurrentLabelIndex);
				m_lstUsedSensorDateLabel.clear();

				// Save to file once an interval
				m_blnRecordStatus = true;

				m_wakeLock.acquire();

				show_screen5();
			}
		}
	};     

	/* Event listener for Stop Recording button on Screen 5 */
	private Button.OnClickListener m_btnStopRecordListener = new Button.OnClickListener() {
		public void onClick(View v) {

			if (m_blnRecordStatus == true) {
				// Stop record. Close Sensor Record File
				if (m_fwSensorRecord != null) {
					try {
						m_fwSensorRecord.close();
						m_fwSensorRecord = null;
					} catch (IOException e) {
						//
					}
				}

				if (m_blnMicEnabled) {
					stopAudioRecording();
				}

				if (m_blnCellularEnabled) {
					stopGettingCellularNetworkInfo();
				}

				if ((m_blnWifiSignalEnabled == true) && (m_blnWifiEnabled == true)) {
					stopWiFiScan();
				}

				if (m_blnGPSSignalEnabled && m_blnGPSEnabled) {
					m_locManager.removeUpdates(m_locListener);
				}

				Date dtTmp = new Date();
				final String DATE_FORMAT_READABLE = "yyyy/MM/dd HH:mm:ss";
				SimpleDateFormat spdCurrentTime = new SimpleDateFormat(DATE_FORMAT_READABLE);
				m_sSensingEndTime = spdCurrentTime.format(dtTmp);

				// Enable sensor setting when recording stops
				enableSensorSetting(true);

				resetValues();
				m_blnRecordStatus = false;

				updateUnUploadedFileName(m_sFullPathFile);

				m_wakeLock.release();
				
				show_screen5_GpsUp();
				
				show_screen6();
			}
		}
	};     
     
     
	/* Check the availability of sensors, disable relative widgets */
	private void checkSensorAvailability() {
		//List<Sensor> lstSensor = m_smSurScan.getSensorList(Sensor.TYPE_ORIENTATION);
		List<Sensor> lstSensor = m_smSurScan.getSensorList(Sensor.TYPE_ROTATION_VECTOR);    //Changed on 20150910
		if (lstSensor.size() > 0) {
			m_blnOrientPresent = true;
		} else {
			m_blnOrientPresent = false;
			m_blnOrientEnabled = false;
		}

		lstSensor = m_smSurScan.getSensorList(Sensor.TYPE_GYROSCOPE);
		if (lstSensor.size() > 0) {
			m_blnGyroPresent = true;
		} else {
			m_blnGyroPresent = false;
			m_blnGyroEnabled = false;
		}

		lstSensor = m_smSurScan.getSensorList(Sensor.TYPE_ACCELEROMETER);
		if (lstSensor.size() > 0) {
			m_blnAcclPresent = true;
		} else {
			m_blnAcclPresent = false;
			m_blnAcclEnabled = false;
		}

		lstSensor = m_smSurScan.getSensorList(Sensor.TYPE_LINEAR_ACCELERATION);
		if (lstSensor.size() > 0) {
			m_blnLinearAcclPresent = true;
		} else {
			m_blnLinearAcclPresent = false;
			m_blnLinearAcclEnabled = false;
		}

		lstSensor = m_smSurScan.getSensorList(Sensor.TYPE_GRAVITY);
		if (lstSensor.size() > 0) {
			m_blnGravityPresent = true;
		} else {
			m_blnGravityPresent = false;
			m_blnGravityEnabled = false;
		}
		
		lstSensor = m_smSurScan.getSensorList(Sensor.TYPE_MAGNETIC_FIELD);
		if (lstSensor.size() > 0) {
			m_blnMagnetPresent = true;
		} else {
			m_blnMagnetPresent = false;
			m_blnMagnetEnabled = false;
		}

		lstSensor = m_smSurScan.getSensorList(Sensor.TYPE_LIGHT);
		if (lstSensor.size() > 0) {
			m_blnLightPresent = true;
		} else {
			m_blnLightPresent = false;
			m_blnLightEnabled = false;
		}

		lstSensor = m_smSurScan.getSensorList(Sensor.TYPE_PRESSURE);
		if (lstSensor.size() > 0) {
			m_blnBarometerPresent = true;
		} else {
			m_blnBarometerPresent = false;
			m_blnBarometerEnabled = false;
		}

	}    

	
    private void configSensorOptionStatus() {
    	if (m_blnOrientPresent == false) {
    		m_chkOrient.setEnabled(false);
    		m_rdSensorOrient.setEnabled(false);
    	}
    	
    	if (m_blnGyroPresent == false) {
    		m_chkGyro.setEnabled(false);
    		m_rdSensorGyro.setEnabled(false);
    	}

    	if (m_blnAcclPresent == false) {
    		m_chkAccl.setEnabled(false);
    		m_rdSensorAccl.setEnabled(false);
    	}

    	if (m_blnLinearAcclPresent == false) {
    		m_chkLinearAccl.setEnabled(false);
    		m_rdSensorLinearAccl.setEnabled(false);
    	}

    	if (m_blnGravityPresent == false) {
    		m_chkGravity.setEnabled(false);
    		m_rdSensorGravity.setEnabled(false);
    	}
   	
    	if (m_blnMagnetPresent == false) {
    		m_chkMagnet.setEnabled(false);
    		m_rdSensorMagnet.setEnabled(false);
    	}
    	 
    	if (m_blnLightPresent == false) {
    		m_chkLight.setEnabled(false);
    		m_rdSensorLight.setEnabled(false);
    	}

    	if (m_blnBarometerPresent == false) {
    		m_chkBarometer.setEnabled(false);
    		m_rdSensorBarometer.setEnabled(false);
    	}

    }
    
    /* Calculate orientation from accelerometer and magnetic field value */
    private boolean calculateOrientation() {
    	float[] arrfValues = new float[3];
    	float[] arrfR = new float[9];
    	float[] arrfI = new float[9];
    	
        if ((m_arrfAcclValues == null) || (m_arrfMagnetValues == null)) {
    		return false;
    	}

        //if (SensorManager.getRotationMatrix(arrfR, null, m_arrfAcclValues, m_arrfMagnetValues)) {    //Ignore inclintion matrix calculation will make the execution faster
    	    	
    	if (SensorManager.getRotationMatrix(arrfR, arrfI, m_arrfAcclValues, m_arrfMagnetValues)) {
    		SensorManager.getOrientation(arrfR, arrfValues);
    		m_arrfOrientValues[0] = (float)Math.toDegrees(arrfValues[0]);
    		m_arrfOrientValues[1] = (float)Math.toDegrees(arrfValues[1]);
    		m_arrfOrientValues[2] = (float)Math.toDegrees(arrfValues[2]);
    		
    		if (m_arrfOrientValues[0] < 0) {
    			m_arrfOrientValues[0] = m_arrfOrientValues[0] + 360;  // Make Azimuth 0 ~ 360
    		}

    		return true;
    	} else {
    		return false;
    	}
    	
    }
     
    private BroadcastReceiver m_brcvWifiStateChangedReceiver = new BroadcastReceiver() {
    	/* Monitor Wifi Enable/Disable status */
    	public void onReceive(Context context, Intent intent) {
    		int iExtraWifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);
    		
    		if (iExtraWifiState == WifiManager.WIFI_STATE_ENABLED) {
    			m_blnWifiSignalEnabled = true;
    			m_chkWifi.setEnabled(true);
    		} else if (iExtraWifiState == WifiManager.WIFI_STATE_DISABLED) {
    			m_blnWifiSignalEnabled = false;
    			m_chkWifi.setEnabled(false);
    		}
    	}
    };
    
    
    public void show_screen1() {
    	int nIndex = -1;
    	setContentView(R.layout.screen_1_user_project);
    	
    	m_rdgpProjectNameType = (RadioGroup)findViewById(R.id.RdGpProjectNameType);
    	m_rdProjectNameType_New = (RadioButton)findViewById(R.id.RdProjectNameType_New);
    	m_rdProjectNameType_Existing = (RadioButton)findViewById(R.id.RdProjectNameType_Existing);
    	
    	m_rdgpUsernameType = (RadioGroup)findViewById(R.id.RdGpUsernameType);
    	m_rdUsernameType_New = (RadioButton)findViewById(R.id.RdUsernameType_New);
    	m_rdUsernameType_Existing = (RadioButton)findViewById(R.id.RdUsernameType_Existing);
    	
    	m_etProjectName_New = (EditText)findViewById(R.id.etProjectNameNew);
    	m_etUsername_New = (EditText)findViewById(R.id.etUsernameNew);
    	
    	m_spnScreen1_ExistingProjectName = (Spinner)findViewById(R.id.spnSelectExistingProjectName);
    	m_spnScreen1_ExistingUsername = (Spinner)findViewById(R.id.spnSelectExistingUsername);
    	
    	m_btnScreen1_Next = (Button)findViewById(R.id.Screen1_Next);    	
    	m_btnScreen1_Next.setOnClickListener(m_btnScreen1_Next_Listener);
   
    	m_btnScreen1_Upload = (Button)findViewById(R.id.btScreen1_Upload);
    	m_btnScreen1_Upload.setOnClickListener(m_btnScreen1_Upload_Listener);
    	
       
    	m_adpExistingProjectName = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, m_lstExistingProjectName);
    	m_adpExistingProjectName.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        m_spnScreen1_ExistingProjectName.setAdapter(m_adpExistingProjectName);
   	
    	m_adpExistingUsername = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, m_lstExistingUsername);
    	m_adpExistingUsername.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        m_spnScreen1_ExistingUsername.setAdapter(m_adpExistingUsername);
                
    	if (m_nProjectNameType == 0) {
    		m_rdProjectNameType_New.setChecked(true);
    		if (m_sProjectName.length() > 0) {
    			m_etProjectName_New.setText(m_sProjectName);
    		}
    	} else if (m_nProjectNameType == 1) {
    		m_rdProjectNameType_Existing.setChecked(true);
    		
    		if (m_lstExistingProjectName.size() > 0) {
    			nIndex = m_lstExistingProjectName.indexOf(m_sProjectName);
    			if (nIndex != -1) {
    				m_spnScreen1_ExistingProjectName.setSelection(nIndex);
    			}
    		}
    		
    	}
    	
    	
    	if (m_nUsernameType == 0) {
    		m_rdUsernameType_New.setChecked(true);
    		if (m_sUsername.length() > 0) {
    			m_etUsername_New.setText(m_sUsername);
    		}
    	} else if (m_nUsernameType == 1) {
    		m_rdUsernameType_Existing.setChecked(true);

    		if (m_lstExistingUsername.size() > 0) {
    			nIndex = m_lstExistingUsername.indexOf(m_sUsername);
    			if (nIndex != -1) {
    				m_spnScreen1_ExistingUsername.setSelection(nIndex);
    			}
    		}
    		
    	}
        
    }
    
    
    public void show_screen2() {
    	int i;
    	Location location = null;

        setContentView(R.layout.screen_2_sensors);

        m_chkAccl = (CheckBox)findViewById(R.id.chkAccl);
        m_chkLinearAccl = (CheckBox)findViewById(R.id.chkLinearAccl);
        m_chkGravity = (CheckBox)findViewById(R.id.chkGravity);        
        m_chkGyro = (CheckBox)findViewById(R.id.chkGyro);
        m_chkOrient = (CheckBox)findViewById(R.id.chkOrient);
        m_chkMagnet = (CheckBox)findViewById(R.id.chkMagnetic);
        m_chkLight = (CheckBox)findViewById(R.id.chkLight);
        m_chkBarometer = (CheckBox)findViewById(R.id.chkBarometer);
        
        m_chkMic = (CheckBox)findViewById(R.id.chkMic);
        m_chkCellular = (CheckBox)findViewById(R.id.chkCellular);

        m_chkGPS = (CheckBox)findViewById(R.id.chkGPS);        
        m_chkWifi = (CheckBox)findViewById(R.id.chkWifi);

        m_chkAccl.setOnCheckedChangeListener(m_chkSensorEnableListener);
        m_chkLinearAccl.setOnCheckedChangeListener(m_chkSensorEnableListener);
        m_chkGravity.setOnCheckedChangeListener(m_chkSensorEnableListener);        
        m_chkGyro.setOnCheckedChangeListener(m_chkSensorEnableListener);        
        m_chkOrient.setOnCheckedChangeListener(m_chkSensorEnableListener);
        m_chkMagnet.setOnCheckedChangeListener(m_chkSensorEnableListener);
        m_chkLight.setOnCheckedChangeListener(m_chkSensorEnableListener);
        m_chkBarometer.setOnCheckedChangeListener(m_chkSensorEnableListener);
        m_chkMic.setOnCheckedChangeListener(m_chkSensorEnableListener);
        m_chkCellular.setOnCheckedChangeListener(m_chkSensorEnableListener);

        m_chkGPS.setOnCheckedChangeListener(m_chkSensorEnableListener);        
        m_chkWifi.setOnCheckedChangeListener(m_chkSensorEnableListener);
        
         
        m_spnScreen2_WiFiScanSpeed = (Spinner)findViewById(R.id.Screen2_spnWiFiScanSpeed);
        
        m_adpWiFiScanSpeed = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, m_arrsWiFiScanSpeed);
        m_adpWiFiScanSpeed.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    	m_spnScreen2_WiFiScanSpeed.setAdapter(m_adpWiFiScanSpeed);
    	m_spnScreen2_WiFiScanSpeed.setSelection(m_nWifiScanSpeedIndex);

        
        m_rdgpSensor = (RadioGroup)findViewById(R.id.RdGpSensor);
    	m_rdSensorAccl = (RadioButton)findViewById(R.id.RdSensor_Accl);
    	m_rdSensorLinearAccl = (RadioButton)findViewById(R.id.RdSensor_LinearAccl);   
    	m_rdSensorGravity = (RadioButton)findViewById(R.id.RdSensor_Gravity);       	
        m_rdSensorGyro = (RadioButton)findViewById(R.id.RdSensor_Gyro);
        m_rdSensorOrient = (RadioButton)findViewById(R.id.RdSensor_Orient);
        m_rdSensorMagnet = (RadioButton)findViewById(R.id.RdSensor_Magnetic);
        m_rdSensorLight = (RadioButton)findViewById(R.id.RdSensor_Light);
    	m_rdSensorBarometer = (RadioButton)findViewById(R.id.RdSensor_Barometer);
       	
        m_rdgpSensorMode = (RadioGroup)findViewById(R.id.RdGpSensorMode);
        m_rdSensorModeFastest = (RadioButton)findViewById(R.id.RdSensorMode_Fastest);
        m_rdSensorModeGame = (RadioButton)findViewById(R.id.RdSensorMode_Game);
        m_rdSensorModeNormal = (RadioButton)findViewById(R.id.RdSensorMode_Normal);
        m_rdSensorModeUI = (RadioButton)findViewById(R.id.RdSensorMode_UI);
                              
        m_rdgpSensor.setOnCheckedChangeListener(m_rdgpSensorListener);
        m_rdgpSensorMode.setOnCheckedChangeListener(m_rdgpSensorModeListener);
                        
        configSensorOptionStatus();
        
		m_mainWifi = (WifiManager)getSystemService(Context.WIFI_SERVICE);
		if (m_mainWifi.getWifiState() == WifiManager.WIFI_STATE_ENABLED) {
			m_blnWifiSignalEnabled = true;
		} else {
			m_blnWifiSignalEnabled = false;
			m_chkWifi.setEnabled(false);
		}
		
		this.registerReceiver(m_brcvWifiStateChangedReceiver, new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION));
		
		m_locManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
		m_blnGPSSignalEnabled = m_locManager.isProviderEnabled(m_sGPSProvider);
				
		if ((m_locManager != null) && (m_blnGPSSignalEnabled == true)) {
			location = m_locManager.getLastKnownLocation(m_sGPSProvider);
			if (location != null) {
				float fLat = (float)(location.getLatitude());
				float fLng = (float)(location.getLongitude());
				if (location.hasAltitude()) {
					float fAlt = (float)(location.getAltitude());
				}
			}
		}
				
        m_btnScreen2_Back = (Button) findViewById(R.id.btScreen2_Back);
        m_btnScreen2_Back.setOnClickListener(m_btnScreen2_Back_Listener);
        m_btnScreen2_Next = (Button) findViewById(R.id.btScreen2_Next);
        m_btnScreen2_Next.setOnClickListener(m_btnScreen2_Next_Listener);
        
        /* No sensor is installed, disable other widgets and show information to user */
		if ((m_blnOrientPresent == false) && 
			(m_blnGyroPresent == false) &&
			(m_blnAcclPresent == false) &&
			(m_blnLinearAcclPresent == false) &&
			(m_blnGravityPresent == false) &&			
			(m_blnMagnetPresent == false) &&
			(m_blnLightPresent == false) &&
			(m_blnBarometerPresent == false)) {
			
			m_rdSensorModeFastest.setEnabled(false);
			m_rdSensorModeGame.setEnabled(false);
			m_rdSensorModeNormal.setEnabled(false);
			m_rdSensorModeUI.setEnabled(false);
			
		}
		
		if (m_blnAcclEnabled == true) {
			m_chkAccl.setChecked(true);
		}

		if (m_blnLinearAcclEnabled == true) {
			m_chkLinearAccl.setChecked(true);
		}

		if (m_blnGravityEnabled == true) {
			m_chkGravity.setChecked(true);
		}
		
		if (m_blnGyroEnabled == true) {
			m_chkGyro.setChecked(true);
		}
	
        if (m_blnOrientEnabled == true) {
        	m_chkOrient.setChecked(true);
        }

        if (m_blnMagnetEnabled == true) {
        	m_chkMagnet.setChecked(true);
        }
        		
		if (m_blnLightEnabled == true) {
			m_chkLight.setChecked(true);
		}
		
		if (m_blnBarometerEnabled == true) {
			m_chkBarometer.setChecked(true);
		}
				
		if (m_blnMicEnabled == true) {
			m_chkMic.setChecked(true);
		}
		
		if (m_blnCellularEnabled == true) {
			m_chkCellular.setChecked(true);
		}
		
		
		if (m_blnGPSEnabled == true) {
			m_chkGPS.setChecked(true);
		}

		if (m_blnWifiEnabled == true) {
			m_chkWifi.setChecked(true);
		}
		
		Settings.System.putInt(getContentResolver(), Settings.System.WIFI_SLEEP_POLICY,  Settings.System.WIFI_SLEEP_POLICY_NEVER);
   	
    }
    
    public void show_screen3() {
		int nLast;
		String sLabelList = "";

    	setContentView(R.layout.screen_3_label);

    	m_etSensorDataLabel = (EditText)findViewById(R.id.etSensorDataLabel);
    	
    	m_btnScreen3_AddLabel = (Button)findViewById(R.id.btScreen3_AddLabel);
    	m_btnScreen3_AddLabel.setOnClickListener(m_btnScreen3_AddLabel_Listener);
    	m_etScreen3LabelList = (EditText)findViewById(R.id.tvScreen3LabelList);
    	    	    	
    	m_btnScreen3_Back = (Button)findViewById(R.id.btScreen3_Back);  	
    	m_btnScreen3_Back.setOnClickListener(m_btnScreen3_Back_Listener);
    	
    	m_btnScreen3_Next = (Button)findViewById(R.id.btScreen3_Next);
    	m_btnScreen3_Next.setOnClickListener(m_btnScreen3_Next_Listener);
    		
		nLast = m_lstSensorDataLabel.size() - 1;
		for (int i=0; i<m_lstSensorDataLabel.size(); i++) {
			if (i != nLast) {
				sLabelList = sLabelList + m_lstSensorDataLabel.get(i) + "\n";
			} else {
				sLabelList = sLabelList + m_lstSensorDataLabel.get(i);
			}
		}
		
		m_etScreen3LabelList.setText(sLabelList);
		   	
    }
    
    public void show_screen4() {
    	
    	setContentView(R.layout.screen_4_start);

    	m_spnScreen4_SelectSensorDataLabel = (Spinner)findViewById(R.id.Screen4_spnSelectSensorDataLabel);
       
    	m_adpSensorDataLabel = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, m_lstSensorDataLabel);
    	m_adpSensorDataLabel.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    	m_spnScreen4_SelectSensorDataLabel.setAdapter(m_adpSensorDataLabel);
    	
    	m_chkNoLabel = (CheckBox)findViewById(R.id.chkNoLabel);   
    	                                    	
    	m_btnScreen4_Start = (Button)findViewById(R.id.btScreen4_Start);
    	m_btnScreen4_Start.setOnClickListener(m_btnStartRecordListener);

    	m_btnScreen4_Back = (Button)findViewById(R.id.btScreen4_Back);  	
    	m_btnScreen4_Back.setOnClickListener(m_btnScreen4_Back_Listener);
    	
    	m_chkNoLabel.setChecked(m_blnNoLabel);
    	m_chkNoLabel.setOnCheckedChangeListener(m_chkNoLabel_Listener);
    }

    
    public void show_screen5() {
    	String sSensorList = "";
    	
    	setContentView(R.layout.screen_5_stop);

    	m_tvGpsUp = (TextView)findViewById(R.id.Screen5_GpsUp);
    	m_tvSensingInfo = (TextView)findViewById(R.id.Screen5_SensingInfo);
    	
    	m_btnScreen5_Stop = (Button)findViewById(R.id.btScreen5_Stop);
    	m_btnScreen5_Stop.setOnClickListener(m_btnStopRecordListener);
    	  	

    	if (m_blnAcclEnabled) {
    		sSensorList = sSensorList + "   - Accelerometer" + "\n";
    	}

    	if (m_blnLinearAcclEnabled) {
    		sSensorList = sSensorList + "   - Linear Accl" + "\n";
    	}

    	if (m_blnGravityEnabled) {
    		sSensorList = sSensorList + "   - Gravity" + "\n";
    	}
   	
    	if (m_blnGyroEnabled) {
    		sSensorList = sSensorList + "   - Gyroscope" + "\n";
    	}

    	if (m_blnOrientEnabled) {
    		sSensorList = sSensorList + "   - Orientation" + "\n";
    	}
    	
    	if (m_blnMagnetEnabled) {
    		sSensorList = sSensorList + "   - Magnetic Field" + "\n";
    	}

    	if (m_blnLightEnabled) {
    		sSensorList = sSensorList + "   - Light" + "\n";
    	}
  	
    	if (m_blnBarometerEnabled) {
    		sSensorList = sSensorList + "   - Barometer" + "\n";
    	}

    	if (m_blnMicEnabled) {
    		sSensorList = sSensorList + "   - Microphone" + "\n";
    	}

    	if (m_blnCellularEnabled) {
    		sSensorList = sSensorList + "   - Cellular ID" + "\n";
    	}
    	
    	if (m_blnGPSEnabled) {
    		sSensorList = sSensorList + "   - GPS" + "\n";
    	}

    	if (m_blnWifiEnabled) {
    		sSensorList = sSensorList + "   - WiFi" + "\n";
    	}
   	
    	m_tvSensingInfo.setText(sSensorList);
    	
  	
    	m_etScreen5_TempLabel = (EditText)findViewById(R.id.Screen5_etTempLabel);
    	m_chkScreen5_UseTempLabel = (CheckBox)findViewById(R.id.Screen5_chkUseTempLabel);
    	m_chkScreen5_UseTempLabel.setOnCheckedChangeListener(m_chkScreen5_UseTempLabelListener);
    	
    	m_svScreen5_Label = (ScrollView) findViewById(R.id.Screen5_scrollview);
    	
    	m_rdgpLabelRadioButtonGroup = (RadioGroup)findViewById(R.id.Screen5_LabelRadioButtonGroup);
    	m_rdgpLabelRadioButtonGroup.setOnCheckedChangeListener(m_rdgpScreen5_LabelRadioButtonListener);
    	
    	m_arrLabelButton = new RadioButton[m_lstSensorDataLabel.size()];
    	
    	for (int i=0; i<m_lstSensorDataLabel.size(); i++) {
    		m_arrLabelButton[i] = new RadioButton(this);
    		m_arrLabelButton[i].setText(m_lstSensorDataLabel.get(i));
    		
    		m_rdgpLabelRadioButtonGroup.addView(m_arrLabelButton[i], m_lpLabelButtonParams);
    		if (i == m_nCurrentLabelIndex) {
    			m_arrLabelButton[i].setChecked(true);
    		}
    	}
    	    	
    	m_svScreen5_Label.post(new Runnable() {
    		public void run() {
    			if (m_nCurrentLabelIndex != -1) {
    				int nY = (int) (m_rdgpLabelRadioButtonGroup.getHeight() *  m_nCurrentLabelIndex/m_lstSensorDataLabel.size());
    			    			
    				m_svScreen5_Label.scrollTo(0, nY);
    			}

    		}
    	});
    	
    	
    	String sInfo = "";
    	
    	if (m_blnNoLabel == true) {
    		sInfo = "No Label";
    	} else {
    		sInfo = "Labeling";
    	}

    	m_tvGpsUp.setText(sInfo);
    }

    
    public void show_screen5_GpsUp() {
    	String sInfo = "";
    	
    	if (m_blnNoLabel == true) {
    		sInfo = "No Label";
    	} else {
    		sInfo = "Labeling";
    	}
    	
    	if (m_blnGpsUp == false) {
    		sInfo = sInfo + "";
    		//m_tvGpsUp.setText("");
    	} else {
    		sInfo = sInfo + " *GPS ON*";
    		//m_tvGpsUp.setText("**GPS ON**");
    	}
    	
    	m_tvGpsUp.setText(sInfo);
    }
    
    public void show_screen6() {
    	List<String> lstLabelNoDuplicate = new ArrayList<String>();
    	String sTmpLabel = "";
    	
    	setContentView(R.layout.screen_6_summary);
    	    	
    	m_tvScreen6_Username = (TextView)findViewById(R.id.Screen6_tvUsername);
    	m_tvScreen6_ProjectName = (TextView)findViewById(R.id.Screen6_tvProjectName);
    	m_tvScreen6_StartTime = (TextView)findViewById(R.id.Screen6_tvStartTime);
    	m_tvScreen6_EndTime = (TextView)findViewById(R.id.Screen6_tvEndTime);
    	m_tvScreen6_LabelNumber = (TextView)findViewById(R.id.Screen6_tvLabelNumber);
    	
    	m_btnScreen6_Back = (Button)findViewById(R.id.btScreen6_Back);  	
    	m_btnScreen6_Back.setOnClickListener(m_btnScreen6_Back_Listener);
    	
    	m_btnScreen6_Upload = (Button)findViewById(R.id.btScreen6_Upload);
    	m_btnScreen6_Upload.setOnClickListener(m_btnScreen6_Upload_Listener);
    	    	
    	m_tvScreen6_Username.setText(m_sUsername);
    	m_tvScreen6_ProjectName.setText(m_sProjectName);
    	m_tvScreen6_StartTime.setText(m_sSensingStartTime);
    	m_tvScreen6_EndTime.setText(m_sSensingEndTime);
    	
    	if (m_lstUsedSensorDateLabel.size() > 0) {
    		lstLabelNoDuplicate.add(m_lstUsedSensorDateLabel.get(0));
    	}
    	
    	for (int i=1; i<m_lstUsedSensorDateLabel.size(); i++) {
    		sTmpLabel = m_lstUsedSensorDateLabel.get(i);
    		if (lstLabelNoDuplicate.contains(sTmpLabel) == false) {
    			lstLabelNoDuplicate.add(sTmpLabel);
    		}
    	}
    	
    	m_tvScreen6_LabelNumber.setText(Integer.valueOf(lstLabelNoDuplicate.size()).toString());
    	
    }
   
    //Get the list of sensor data files which have not been uploaded
    private void readUnUploadedFileName() {
    	String sUnUploadedFileNameFilePath = "";
    	File flFile;
    	FileReader fr;
    	BufferedReader br;
    	String sLine = "";
    	
    	m_lstUnUploadedFileName.clear();
    	
		sUnUploadedFileNameFilePath = m_sSettingFoler + File.separator + m_sUnUploadedFileName;
		
		flFile = new File(sUnUploadedFileNameFilePath);
		if (flFile.exists()) {
			//Derive existing labels
			try{
				fr = new FileReader(sUnUploadedFileNameFilePath);
				br = new BufferedReader(fr);
				
				while( (sLine = br.readLine()) != null) {
					m_lstUnUploadedFileName.add(sLine);
				}
				
				fr.close();
			} catch (Exception e) {
				
			}					
		}
    	
    }
    
    private void updateUnUploadedFileName(String sFilePathName) {
		 try {
			 String sUnUploadedFileNameFilePath = m_sSettingFoler + File.separator + m_sUnUploadedFileName;

			 FileWriter fwUnUploadedFile = new FileWriter(sUnUploadedFileNameFilePath, true);   //Append
			 
			 String sLine = sFilePathName + "\n";
			 fwUnUploadedFile.write(sLine);
			 
			 fwUnUploadedFile.close();
		 } catch (Exception e) {
			 
		 }
    	
    }
    
    
    private void updateUnUploadedFileName(List<String> lstFilePathName) {    	 
		 try {
			 String sUnUploadedFileNameFilePath = m_sSettingFoler + File.separator + m_sUnUploadedFileName;

			 FileWriter fwUnUploadedFile = new FileWriter(sUnUploadedFileNameFilePath, false);   //Overwrite
			 
			 String sLine;
			 
			 for (int i=0; i<lstFilePathName.size(); i++) {
				 sLine = lstFilePathName.get(i) + "\n";
				 fwUnUploadedFile.write(sLine);
			 }
			 
			 fwUnUploadedFile.close();
		 } catch (Exception e) {
			 
		 }
    	
    }
   
    private String getFileName(String sFilePathName) {
    	String sFileName;
    	int nPos = -1;
    	
    	nPos = sFilePathName.lastIndexOf('/');
    	sFileName = sFilePathName.substring(nPos+1);
    	return sFileName;
    }
    
    private void show_screenUploadData() {
    	int nCount;
    	
    	setContentView(R.layout.screen_upload_data);
    	m_btnScreenUploadData_Back = (Button)findViewById(R.id.btScreenUploadData_Back);  	
    	m_btnScreenUploadData_Back.setOnClickListener(m_btScreenUploadData_Back_Listener);
    	
    	m_btnScreenUploadData_Upload = (Button)findViewById(R.id.btScreenUploadData_Upload);
    	m_btnScreenUploadData_Upload.setText(R.string.upload);
    	m_btnScreenUploadData_Upload.setOnClickListener(m_btnScreenUploadData_Upload_Listener);
    	
    	m_blnUpload = false;
    	
    	m_pgbActivityIndicator = (ProgressBar)findViewById(R.id.ScreenUploadData_UploadProgress);
    	 
    	m_llUploadData_ChkLayoutFileName = (LinearLayout) findViewById(R.id.ScreenUploadData_ChkLayoutFileName);
    	
    	readUnUploadedFileName();
    	
    	nCount = m_lstUnUploadedFileName.size();
    	m_arrChkFilename = new CheckBox[nCount];
    	
    	for (int i=0; i<nCount; i++) {
    		m_arrChkFilename[i] = new CheckBox(this);   		
    		m_arrChkFilename[i].setId(i);
    		m_arrChkFilename[i].setText(getFileName(m_lstUnUploadedFileName.get(i)));
    		m_arrChkFilename[i].setOnCheckedChangeListener(null);
    		m_arrChkFilename[i].setLayoutParams(m_lpFilenameChkParams);
    		m_llUploadData_ChkLayoutFileName.addView(m_arrChkFilename[i]);

    	}    	
    	
    }
    
    //This function prepare the existing project names/usernames/labels
    private void preconfigSetting() {
    	String sAppFolder = "";
    	String sExistingProjectNameFilePath = "";
    	String sExistingUsernameFilePath = "";
    	String sExistingLabelFilePath = "";
    	File flFolder, flFile;
    	FileReader fr;
    	BufferedReader br;
    	String sLine = "";
    	
		// Check whether SD Card has been plugged in
		if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
			sAppFolder = Environment.getExternalStorageDirectory().getAbsolutePath() + 
									File.separator + getString(R.string.app_folder);
			flFolder = new File(sAppFolder);
			//Check whether /mnt/sdcard/Sensing/ exists
			if (!flFolder.exists()) {
				//Does not exist, create it
				if (flFolder.mkdir()) {

				} else {
					//Failed to create
					Toast.makeText(getApplicationContext(), "SD Card is not accessible!", Toast.LENGTH_SHORT).show();
					return;
				}
			} 
						
			m_sSettingFoler = Environment.getExternalStorageDirectory().getAbsolutePath() + 
					File.separator + getString(R.string.app_folder) + File.separator + getString(R.string.setting_folder);
			flFolder = new File(m_sSettingFoler);
			//Check whether /mnt/sdcard/Sensing/Setting/ exists
			if (!flFolder.exists()) {
				//Does not exist, create it
				if (flFolder.mkdir()) {

				} else {
					//Failed to create
					Toast.makeText(getApplicationContext(), "SD Card is not accessible!", Toast.LENGTH_SHORT).show();
					return;
				}
			} else {
				sExistingProjectNameFilePath = m_sSettingFoler + File.separator + m_sExistingProjectNameFileName;

				flFile = new File(sExistingProjectNameFilePath);
				if (flFile.exists()) {
					//Derive existing project names
					try{
						fr = new FileReader(sExistingProjectNameFilePath);
						br = new BufferedReader(fr);
						
						while( (sLine = br.readLine()) != null) {
							m_lstExistingProjectName.add(sLine);
						}
						
						fr.close();
					} catch (Exception e) {
						
					}

				}
				
				sExistingUsernameFilePath = m_sSettingFoler + File.separator + m_sExistingUsernameFileName;

				flFile = new File(sExistingUsernameFilePath);
				if (flFile.exists()) {
					//Derive existing usernames
					try{
						fr = new FileReader(sExistingUsernameFilePath);
						br = new BufferedReader(fr);
						
						while( (sLine = br.readLine()) != null) {
							m_lstExistingUsername.add(sLine);
						}
						
						fr.close();
					} catch (Exception e) {
						
					}
					
				}
								
				sExistingLabelFilePath = m_sSettingFoler + File.separator + m_sExistingLabelFileName;
				
				flFile = new File(sExistingLabelFilePath);
				if (flFile.exists()) {
					//Derive existing labels
					try{
						fr = new FileReader(sExistingLabelFilePath);
						br = new BufferedReader(fr);
						
						while( (sLine = br.readLine()) != null) {
							m_lstSensorDataLabel.add(sLine);
						}
						
						fr.close();
					} catch (Exception e) {
						
					}					
				}

								
			}
			
			m_sSensorDataFolder = Environment.getExternalStorageDirectory().getAbsolutePath() + 
					File.separator + getString(R.string.app_folder) + File.separator + getString(R.string.sensordata_folder);
			flFolder = new File(m_sSensorDataFolder);
			//Check whether /mnt/sdcard/Sensing/SensorData/ exists
			if (!flFolder.exists()) {
				//Does not exist, create it
				if (flFolder.mkdir()) {

				} else {
					//Failed to create
					Toast.makeText(getApplicationContext(), "SD Card is not accessible!", Toast.LENGTH_SHORT).show();
					return;
				}
			} 

			
		} else {        				
			//NO SD Card
			Toast.makeText(getApplicationContext(), "Please insert SD Card!", Toast.LENGTH_SHORT).show();
			return;
		}
    	
    }
    
    
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	int i;
    	Location location = null;
     	
    	super.onCreate(savedInstanceState);
    	   	
    	if (android.os.Build.VERSION.SDK_INT > 9) {
    	    StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
    	    StrictMode.setThreadPolicy(policy);
    	}
    	
    	try {
            Class.forName("android.os.AsyncTask");
    	} catch (ClassNotFoundException e) {
            e.printStackTrace();
    	}
    	
    	m_smSurScan = (SensorManager) getSystemService(SENSOR_SERVICE);
    	
        PackageManager pm = getPackageManager();
        m_riHome = pm.resolveActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),0);
        
        m_nBufferSize = AudioRecord.getMinBufferSize(m_nAudioSampleRate, AudioFormat.CHANNEL_IN_STEREO,AudioFormat.ENCODING_PCM_16BIT);
        
        m_tmCellular = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        
        checkSensorAvailability();
        
    	//Get Existing Project Name and Existing User Name
    	preconfigSetting();
    	
    	/* When the power button is pressed and the screen goes off, the sensors will stop work by default,
    	 * Here keep the CPU on to keep sensor alive and also use SCREEN_OFF notification to re-enable GPS/WiFi
    	 */
    	PowerManager pwrManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
    	m_wakeLock = pwrManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
    	IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
    	filter.addAction(Intent.ACTION_SCREEN_OFF);
    	
    	registerReceiver(m_ScreenOffReceiver,filter);
    	
    	show_screen1();	
    }
    
    
    public void startActivitySafely(Intent intent) {
    	intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    	try {
    		startActivity(intent);
    	} catch (ActivityNotFoundException e) {
    		Toast.makeText(this, "unable to open", Toast.LENGTH_SHORT).show();
    	} catch (SecurityException e) {
    		Toast.makeText(this, "unable to open", Toast.LENGTH_SHORT).show();
    	}
    	
    }
    
    public boolean onKeyDown(int keyCode, KeyEvent event) {
    	if (keyCode == KeyEvent.KEYCODE_BACK) {
    		//Show MAIN app without finishing current activity
    		ActivityInfo ai = m_riHome.activityInfo;
    		Intent startIntent = new Intent(Intent.ACTION_MAIN);
    		startIntent.addCategory(Intent.CATEGORY_LAUNCHER);
    		startIntent.setComponent(new ComponentName(ai.packageName,ai.name));
    		startActivitySafely(startIntent);
    		return true;
    	} else {
    		return super.onKeyDown(keyCode, event);
    	}
    }
    
    protected void onResume() {
    	super.onResume();
    }
        
    protected void onStop() {
    	super.onStop();    	
    }

    //Record Sound Level
    public void recordSoundLevel(double fSoundLevelDb) {
    	SensorData senData = new SensorData(DATA_TYPE_MIC, fSoundLevelDb);
    	recordSensingInfo(senData);
    }
    
    //Record Cellular Network ID
    public void recordCellInfo(int nCellId) {
    	SensorData senData = new SensorData(DATA_TYPE_CELLULAR, nCellId);
    	recordSensingInfo(senData);
    }
    
    //Record GPS Readings
    public void recordLocation(Location location) {
    	SensorData senData = new SensorData(DATA_TYPE_GPS, location);
    	recordSensingInfo(senData);
    }
    
    //Record Sensor readings
    public void onSensorChanged(SensorEvent event) {
    	SensorData senData = new SensorData(DATA_TYPE_SENSOR, event);
    	recordSensingInfo(senData);    	
    }
    
    //Organize the sensing information and save into CSV file
    public void recordSensingInfo(SensorData senData) {
    	String sRecordLine;
    	String sTimeField;
    	Date dtCurDate;
    	int i;
    	long lStartTime = 0;
    	long lCurrentTime = 0;
		SimpleDateFormat spdRecordTime,spdCurDateTime;
        final String DATE_FORMAT = "yyyyMMddHHmmss";
		final String DATE_FORMAT_S = "yyMMddHHmmssSSS"; //"yyyyMMddHHmmssSSS"
		int nSensorReadingType = SENSOR_EVENT_NULL; 
		int nSensorDataType;
		
        if (m_blnRecordStatus == false) { //Stopped
        	return;
        }

        dtCurDate = new Date();
		        
		// Timestamp for the record
        spdRecordTime = new SimpleDateFormat(DATE_FORMAT_S);
		sTimeField = spdRecordTime.format(dtCurDate);
				
		nSensorDataType = senData.getSensorDataType();
		
		if (nSensorDataType == DATA_TYPE_SENSOR) {
			SensorEvent event;
			
			event = senData.getSensorEvent();
			
			synchronized(this) {
	    		switch (event.sensor.getType()){
	
				    			
		    		case Sensor.TYPE_ACCELEROMETER:
		    			//X, Y, Z
		    			if (m_blnAcclEnabled) {
		    				m_sAccl = Float.toString(event.values[0]) + "," + 
		    						Float.toString(event.values[1]) + "," + 
		    						Float.toString(event.values[2]) + ",";
		    			
		    				nSensorReadingType = SENSOR_EVENT_ACCL;
		    			}
		    			
//		    			if (m_blnOrientEnabled) {
//			    			m_arrfAcclValues = event.values.clone();
//		    				
//			    			if (calculateOrientation()) {
//			    				//Azimuth (rotation around z-axis); Pitch (rotation around x-axis), Roll (rotation around y-axis)
//		    					m_sOrient = Float.toString(m_arrfOrientValues[0]) + "," + 
//		    								Float.toString(m_arrfOrientValues[1]) + "," + 
//		    								Float.toString(m_arrfOrientValues[2]) + ",";
//		    					
//		    					nSensorReadingType = SENSOR_EVENT_ORIENT;
//		    					
//		    				}
//		    			}
		    			break;
		    			
		    		case Sensor.TYPE_LINEAR_ACCELERATION:
		    			//X,Y,Z
		    			if (m_blnLinearAcclEnabled) {
		    				m_sLinearAccl = Float.toString(event.values[0]) + "," + 
		    								Float.toString(event.values[1]) + "," + 
		    								Float.toString(event.values[2]) + ",";
		    			
		    				nSensorReadingType = SENSOR_EVENT_LINEAR_ACCL;
		    			}
		    			
		    			break;
		    			
		    		case Sensor.TYPE_GRAVITY:
		    			//X,Y,Z
		    			if (m_blnGravityEnabled) {
		    				m_sGravity = Float.toString(event.values[0]) + "," + 
		    							 Float.toString(event.values[1]) + "," + 
		    							 Float.toString(event.values[2]) + ",";
		    				
		    				nSensorReadingType = SENSOR_EVENT_GRAVITY;
		    			}
		    			
	    				break;

		    		case Sensor.TYPE_GYROSCOPE:
		    			//X,Y,Z
		    			m_sGyro = Float.toString(event.values[0]) + "," + 
		    					  Float.toString(event.values[1]) + "," + 
		    					  Float.toString(event.values[2]) + ",";
		    			nSensorReadingType = SENSOR_EVENT_GYRO;
		    			break;
		    			
		    		case Sensor.TYPE_MAGNETIC_FIELD:
		    			// Values are in micro-Tesla (uT) and measure the ambient magnetic field 
		    			if (m_blnMagnetEnabled) {
		    				m_sMagnet = Float.toString(event.values[0]) + "," + 
		    							Float.toString(event.values[1]) + "," + 
		    							Float.toString(event.values[2]) + ",";
		    				
		    				nSensorReadingType = SENSOR_EVENT_MAGNET;
		    			}
	
//		    			if (m_blnOrientEnabled) {
//		    				m_arrfMagnetValues = event.values.clone();
//		    				
//		    				if (calculateOrientation()) {
//		    					//Azimuth (rotation around z-axis); Pitch (rotation around x-axis), Roll (rotation around y-axis)
//		    					m_sOrient = Float.toString(m_arrfOrientValues[0]) + "," + 
//	    									Float.toString(m_arrfOrientValues[1]) + "," + 
//	    									Float.toString(m_arrfOrientValues[2]) + ",";
//		    							    					
//		    					if (nSensorReadingType != SENSOR_EVENT_MAGNET) {
//		    						nSensorReadingType = SENSOR_EVENT_ORIENT;
//		    					}
//		    				}
//		    			}
		    			
		    			break;
		    		
		    		case Sensor.TYPE_ROTATION_VECTOR:  //Added on 20150910
		    			if (m_blnOrientEnabled) {
		    				float[] arrfRotVal = new float[3];
			    	    	float[] arrfR = new float[9];
			    	    	float[] arrfValues = new float[3];

		    				try {
		    					System.arraycopy(event.values, 0, arrfRotVal, 0, event.values.length);
		    				} catch (IllegalArgumentException e) {
		    					//Hardcode the size to handle a bug on Samsung devices
		    					System.arraycopy(event.values, 0, arrfRotVal, 0, 3);
		    				}
		    				
		    				SensorManager.getRotationMatrixFromVector(arrfR, arrfRotVal);
		    				SensorManager.getOrientation(arrfR, arrfValues);
		    				
		    	    		m_arrfOrientValues[0] = (float)Math.toDegrees(arrfValues[0]);
		    	    		m_arrfOrientValues[1] = (float)Math.toDegrees(arrfValues[1]);
		    	    		m_arrfOrientValues[2] = (float)Math.toDegrees(arrfValues[2]);
		    	    		
		    	    		if (m_arrfOrientValues[0] < 0) {
		    	    			m_arrfOrientValues[0] = m_arrfOrientValues[0] + 360;  // Make Azimuth 0 ~ 360
		    	    		}
		    				
//	    					//Azimuth (rotation around z-axis); Pitch (rotation around x-axis), Roll (rotation around y-axis)
	    					m_sOrient = Float.toString(m_arrfOrientValues[0]) + "," + 
    									Float.toString(m_arrfOrientValues[1]) + "," + 
    									Float.toString(m_arrfOrientValues[2]) + ",";
		    	    		
	    					//m_tvGpsUp.setText(m_sOrient); //Show orientation
		    	    		nSensorReadingType = SENSOR_EVENT_ORIENT;
		    			}
		    			
		    			break;
			    			
		    		case Sensor.TYPE_LIGHT:
		    			// Ambient light level in SI lux units 
		    			m_sLight = Float.toString(event.values[0]) + ",";
		    			nSensorReadingType = SENSOR_EVENT_LIGHT;
		    			break;
		    		
		    		case Sensor.TYPE_PRESSURE:
		    			// Atmospheric pressure in hPa (millibar)
		    			m_sBarometer = Float.toString(event.values[0]) + ",";
		    			nSensorReadingType = SENSOR_EVENT_BAROMETER;
		    			break;
		    			
	    		}
	    	}
		} else if (nSensorDataType == DATA_TYPE_GPS){
			Location locationGps;
			locationGps = senData.getGpsLocation();
			
			if (locationGps != null) {

				m_location = new Location(locationGps);

				//Change from double to float
				m_sGPS = Float.valueOf((float)(locationGps.getLatitude())).toString() + "," +
						Float.valueOf((float)(locationGps.getLongitude())).toString() + ",";
				if (locationGps.hasAltitude()) {
					m_sGPS = m_sGPS + Float.valueOf((float)(locationGps.getAltitude())).toString() + ",";
					GeomagneticField geoField = new GeomagneticField(
								Double.valueOf(locationGps.getLatitude()).floatValue(),
								Double.valueOf(locationGps.getLongitude()).floatValue(),
								Double.valueOf(locationGps.getAltitude()).floatValue(),
								System.currentTimeMillis());
					// Append Declination, in Degree
					m_sGPS = m_sGPS + Float.valueOf((float)(geoField.getDeclination())).toString() + "," + Float.valueOf((float)(geoField.getInclination())).toString() + ",";
				} else {
					m_sGPS = m_sGPS + ",,,";
					//m_sGPS = m_sGPS + ",";
				}
				
				//New add 201408270009
				if (locationGps.hasSpeed()) {
					m_sGPS = m_sGPS + Float.valueOf((float)(locationGps.getSpeed())).toString() + ",";
				} else {
					m_sGPS = m_sGPS + ",";
				}
				
				if (locationGps.hasBearing()) {
					m_sGPS = m_sGPS + Float.valueOf((float)(locationGps.getBearing())).toString() + ",";
				} else {
					m_sGPS = m_sGPS + ",";
				}
				
				nSensorReadingType = SENSOR_EVENT_GPS;
				
				m_blnGpsUp = true;
				show_screen5_GpsUp();
			} else {
				m_blnGpsUp = false;
				show_screen5_GpsUp();				
			}
		} else if (nSensorDataType == DATA_TYPE_MIC) {
			double fSoundLevelDb;
			fSoundLevelDb = senData.getSoundLevelDb();
			m_sSouldLevel = new BigDecimal(fSoundLevelDb).setScale(0, BigDecimal.ROUND_HALF_UP) + ",";
			
			nSensorReadingType = SENSOR_EVENT_MIC;
			
		} else if (nSensorDataType == DATA_TYPE_CELLULAR) {
			int nCellId;
			nCellId = senData.getCellId();
			m_sCellId = Integer.valueOf(nCellId).toString() + ",";
			nSensorReadingType = SENSOR_EVENT_CELLULAR;
			
		} else if (nSensorDataType == DATA_TYPE_WIFI) {
			List<WifiData> lstWifiData = senData.getListWifiData();
			int nWifiCnt = Math.min(WIFI_COUNT, lstWifiData.size());
			m_sWifi = "";
			for (i=0; i< nWifiCnt; i++) {
				//m_sWifi = m_sWifi + lstWifiData.get(i).getSSID() + "," + lstWifiData.get(i).getBSSID() + "," + lstWifiData.get(i).getSignalLevel() + ",";
				m_sWifi = m_sWifi + lstWifiData.get(i).getBSSID() + "," + lstWifiData.get(i).getSignalLevel() + ",";

			}
			
			for (i=1; i<=WIFI_COUNT-nWifiCnt; i++) {
				//m_sWifi = m_sWifi + ",,,";
				m_sWifi = m_sWifi + ",,";
			}
			
			nSensorReadingType = SENSOR_EVENT_WIFI;
		}
		
		if (nSensorReadingType == SENSOR_EVENT_NULL) {
			return;
		}
		
		sRecordLine = sTimeField + ",";
		
		if (m_blnNoLabel == false) {
			sRecordLine = sRecordLine + m_sCurrentLabel + ",";
		}
		
		sRecordLine = sRecordLine + Integer.valueOf(nSensorReadingType) + ",";
		
		//New: Every field always there
		//Field in each line:
		/*
		 *  1) Timestamp
		 *  2) Label
		 *  3) SensingEventType
		 *  4-6) Accl
		 *  7-9) Linear Accl
		 *  10-12) Gravity
		 *  13-15) Gyro
		 *  16-18) Orientation
		 *  19-21) Magnet
		 *  22) Light
		 *  23) Barometer
		 *  24) Sould Level (Decibel)
		 *  25) Cell ID
		 *  26-32) GPS (Lat, Long, Alt, Declination, Inclination, Speed, Bearing)
		 *  33-72) WiFi (<BSSID, Level>) 
		 */
//		sRecordLine = sRecordLine  + m_sAccl + m_sGyro + m_sOrient + m_sMagnet + 
//									m_sLight + m_sBarometer +  
//									m_sSouldLevel + m_sCellId +
//									m_sGPS + m_sWifi;

		sRecordLine = sRecordLine  + m_sAccl + m_sLinearAccl + m_sGravity + m_sGyro + m_sOrient + 
									m_sMagnet + m_sLight + m_sBarometer +  
									m_sSouldLevel + m_sCellId + m_sGPS + m_sWifi;
		
		////////////////////////////
//		String sAngle = calculateRot(m_sAccl, m_sGravity);
//		String sarrAngle[] = sAngle.split(",");
//		String sShow = sarrAngle[0] + "\n" + sarrAngle[1];
		
//		String sShow = "";
		
//		if (m_sGravity.length() > 3) {
//			String sarrAngle[] = m_sGravity.split(",");
//			double fX = Double.valueOf(sarrAngle[0]).doubleValue();
//			double fY = Double.valueOf(sarrAngle[1]).doubleValue();
//			double fZ = Double.valueOf(sarrAngle[2]).doubleValue();
//			
//			double fTotal = Math.sqrt(fX*fX + fY*fY + fZ*fZ);
//			
//			double fAngleZ = Math.acos(fZ/fTotal)/Math.PI*180;
//			double fAngleY = 90 - Math.acos(fY/fTotal)/Math.PI*180;
//			double fAngleX = 90 - Math.acos(fX/fTotal)/Math.PI*180;
//			
//			sShow = "X:  " +  fAngleX + "\n";
//			sShow = sShow + "Y:  " +  fAngleY + "\n";
//			sShow = sShow + "Z:  " +  fAngleZ;
//			
//							
//		}
		
//		if (m_sGravity.length() > 3) {
//			String sarrAngle[] = m_sGravity.split(",");
//			double fX = Double.valueOf(sarrAngle[0]).doubleValue();
//			double fY = Double.valueOf(sarrAngle[1]).doubleValue();
//			double fZ = Double.valueOf(sarrAngle[2]).doubleValue();
//			
//			int nSymbol = 0;
//			if (fX < 0)  {
//				sShow = sShow + "- X" + "\n";
//			} else if (fX > 0) {
//				sShow = sShow + "+ X" + "\n";
//			}
//			
//			if (fY < 0)  {
//				sShow = sShow + "- Y" + "\n";
//			} else if (fY > 0) {
//				sShow = sShow + "+ Y" + "\n";
//			}
//			
//			if (fZ < 0)  {
//				sShow = sShow + "- Z";
//			} else if (fZ > 0) {
//				sShow = sShow + "+ Z";
//			}
//							
//		}
//		
//		if (m_sGyro.length() > 3) {
//			String sarrAngle[] = m_sGyro.split(",");
//			double fX = Double.valueOf(sarrAngle[0]).doubleValue();
//			double fY = Double.valueOf(sarrAngle[1]).doubleValue();
//			double fZ = Double.valueOf(sarrAngle[2]).doubleValue();
//			
//			int nSymbol = 0;
//			if (fX < 0)  {
//				nSymbol = -1;
//			} else if (fX > 0) {
//				nSymbol = 1;
//			}
//			
//			if (fY < 0)  {
//				nSymbol = nSymbol + (-1);
//			} else if (fY > 0) {
//				nSymbol = nSymbol + 1;
//			}
//			
//			if (fZ < 0)  {
//				nSymbol = nSymbol + (-1);
//			} else if (fZ > 0) {
//				nSymbol = nSymbol + 1;
//			}
//				
//			if (nSymbol < 0) {
//				nSymbol = -1;
//			} else if (nSymbol > 0) {
//				nSymbol = 1;
//			}
//			
//			sShow = sShow + "\n\n" + nSymbol + "";
//		}
		
//		m_tvSensingInfo.setText(sShow);
		////////////////////////////
		
    	sRecordLine = sRecordLine + System.getProperty("line.separator");
    	
    	if (m_fwSensorRecord != null) {
			//Write information into file
			//Compose information into recordLine
    		try {
    			m_fwSensorRecord.write(sRecordLine);
    		} catch (IOException e) {
    			
    		}
    	}

    }
    
    public String calculateRot(String sAccl, String sGravity) {
    	String sResult = "0,0";
    	String[] sarrAccl = null;
    	String[] sarrGravity = null;
    	
    	if ((sAccl.length() > 3) && (sGravity.length() > 3)) {       	
    		sarrAccl = sAccl.split(",");
    		sarrGravity = sGravity.split(",");
    		double fAcclX = Double.valueOf(sarrAccl[0]).doubleValue();
    		double fAcclY = Double.valueOf(sarrAccl[1]).doubleValue();
    		double fGravityX = Double.valueOf(sarrGravity[0]).doubleValue();
    		double fGravityY = Double.valueOf(sarrGravity[1]).doubleValue();
    		
    		double alpha, beta;
    		
    		if (fGravityX > 0) {
    			alpha = Math.asin(fAcclX/fGravityX)/Math.PI*180;
    		} else {
    			alpha = 0;
    		}

    		if (fGravityY > 0) {
    			beta = Math.asin(fAcclY/fGravityY)/Math.PI*180;
    		} else {
    			beta = 0;
    		}
    	
    		sResult = alpha + "     ,"  + beta;
    	}
    	
    	return sResult;
    }
    
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    	switch(sensor.getType()) {
			case Sensor.TYPE_ACCELEROMETER:
				m_sSensorAccuracy = "1,"; //Accl
				break;
	
			case Sensor.TYPE_LINEAR_ACCELERATION:
				m_sSensorAccuracy = "2,"; //LinearAccl
				break;
	
	    	case Sensor.TYPE_GYROSCOPE:
    			m_sSensorAccuracy = "3,"; //Gyro
    			break;

    		case Sensor.TYPE_MAGNETIC_FIELD:
    			m_sSensorAccuracy = "4,"; //Magnet
    			break;

    		case Sensor.TYPE_GRAVITY:
    			m_sSensorAccuracy = "5,"; //Gravity		
    			break;
    			
    		case Sensor.TYPE_PROXIMITY:
    			m_sSensorAccuracy = "6,"; //Proxi
    			break;

    		case Sensor.TYPE_LIGHT:
    			m_sSensorAccuracy = "7,"; //Light
    			break;

    		case Sensor.TYPE_PRESSURE:
    			m_sSensorAccuracy = "8,"; //Barometer
    			break;

    		case Sensor.TYPE_RELATIVE_HUMIDITY:
    			m_sSensorAccuracy = "9,"; //Humidity
    			break;

    		case Sensor.TYPE_AMBIENT_TEMPERATURE:
    			m_sSensorAccuracy = "10,"; //Temperature
    			break;
    			
    		default:
    			m_sSensorAccuracy = "11,"; //Other
    	}
    	
    	switch (accuracy) {
    		case SensorManager.SENSOR_STATUS_ACCURACY_HIGH:
    			m_sSensorAccuracy = m_sSensorAccuracy + "1,"; //H
    			break;

    		case SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM:
    			m_sSensorAccuracy = m_sSensorAccuracy + "2,"; //M
    			break;
    			
    		case SensorManager.SENSOR_STATUS_ACCURACY_LOW:
    			m_sSensorAccuracy = m_sSensorAccuracy + "3,"; //L
    			break;
    			
    		case SensorManager.SENSOR_STATUS_UNRELIABLE:
    			m_sSensorAccuracy = m_sSensorAccuracy + "4,"; //U
    			break;
    	}
    }
 
    
    private void startWiFiScan() {
    	if (m_mainWifi != null) {
    		
	    	m_wifiScanThread = new Thread(new Runnable() {
											public void run() {
												scanWiFi();
											}
									},"Scanning WiFi Thread");
	
	    	m_wifiScanThread.start();
    	}
    }

    private void scanWiFi() {
    	while (m_wifiScanThread != null && m_mainWifi != null) {
    		m_mainWifi.startScan();
//    		try {
//    			Method startScanActiveMethod = WifiManager.class.getMethod("startScanActive");
//    			startScanActiveMethod.invoke(m_mainWifi);  		
//    		} catch (Exception e) {
//    			
//    		}
    		//PrintCurrentTime("StartScan");
    		
      	  	try {
      	  		Thread.sleep(m_arrlWiFiScanSpeed[m_nWifiScanSpeedIndex]);
      	  	} catch (InterruptedException e) {
    		  
      	  	}
   		
    	}
   	
    }
    
    private void stopWiFiScan() {
    	m_wifiScanThread = null;
    	if (m_receiverWifi != null) {
    		unregisterReceiver(m_receiverWifi);
    	}
    }
    
    private void startGettingCellularNetworkInfo() {
        
        if (m_tmCellular != null) {
        	
        	CellLocation cellLoc = m_tmCellular.getCellLocation();
        	
        	if (cellLoc instanceof GsmCellLocation) {   //GSM compatible network
        		
        		m_GsmCellLocation = (GsmCellLocation) cellLoc;
	
	        	if (m_GsmCellLocation != null) {
	            	m_cellularThreadGsm = new Thread(new Runnable() {
													public void run() {
														getGsmCellularInfo();
													}
											},"GSM Cellular Thread");
	
	            	m_cellularThreadGsm.start();
	        	}
        	} else if (cellLoc instanceof CdmaCellLocation) {   //CDMA compatible network
        		
        		m_CdmaCellLocation = (CdmaCellLocation) cellLoc;
        		
	        	if (m_CdmaCellLocation != null) {
	            	m_cellularThreadCdma = new Thread(new Runnable() {
													public void run() {
														getCdmaCellularInfo();
													}
											},"CDMA Cellular Thread");
	
	            	m_cellularThreadCdma.start();
	        	}
        		
        	}
        }
        
    }
    
    private void getGsmCellularInfo() {
    	
    	while (m_GsmCellLocation != null) {
    		m_GsmCellLocation.requestLocationUpdate();
    		
    		int nCellId = m_GsmCellLocation.getCid();
    		
    		recordCellInfo(nCellId);
    		
      	  	try {
      	  		Thread.sleep(500);
      	  	} catch (InterruptedException e) {
    		  
      	  	}
   		
    	}
    }

    private void getCdmaCellularInfo() {
    	
    	while (m_CdmaCellLocation != null) {
    		m_CdmaCellLocation.requestLocationUpdate();
    		
    		int nCellId = m_CdmaCellLocation.getBaseStationId();
    		
    		recordCellInfo(nCellId);
    		
      	  	try {
      	  		Thread.sleep(500);
      	  	} catch (InterruptedException e) {
    		  
      	  	}
   		
    	}
    }
   
    
    private void stopGettingCellularNetworkInfo() {
    	if (m_GsmCellLocation != null) {
    		m_GsmCellLocation = null;
    	}
    	
    	if (m_CdmaCellLocation != null) {
    		m_CdmaCellLocation = null;
    	}
    	
    	m_cellularThreadGsm = null;
    	m_cellularThreadCdma = null;
    }
    
    
    private void startAudioRecording() {
      	m_audioRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
      			m_nAudioSampleRate, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT, m_nBufferSize);
      	
      	if (m_audioRecorder == null) return;
      	
      	int i = m_audioRecorder.getState();
      	if (i == AudioRecord.STATE_INITIALIZED) {
      		m_audioRecorder.startRecording();
      	} else {
      		return;
      	}

      	if (m_blnRecordSoundLevel == true || m_blnRecordSoundFile == true) {
      		m_processSoundThread = new Thread(new Runnable() {
      									public void run() {
      										processAudioData();
      									}
      								},"Audio Thread");

      		m_processSoundThread.start();
      		
      		if (m_blnRecordSoundLevel == true) {
      			m_soundLevelThread = new Thread(new Runnable() {
												public void run() {
													calculateAudioSoundLevel();
												}
									},"Sould Level Thread");

      			m_soundLevelThread.start();
      		}
      	}
     
    }

    
    private void processAudioData() {        
        String sFullAudioPathFileTmp = m_sRecordFullPathFile + ".wav.tmp"; 
        FileOutputStream os = null;

        m_btAudioBuffer = new byte[m_nBufferSize];
        m_nAudioReadCount = AudioRecord.ERROR_INVALID_OPERATION;
        
        if (m_blnRecordSoundFile == true) {
	        try {
	            os = new FileOutputStream(sFullAudioPathFileTmp);
	        } catch (FileNotFoundException e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
	        }
        }
        
        while (m_audioRecorder != null) {
        	
        	m_nAudioReadCount = m_audioRecorder.read(m_btAudioBuffer, 0, m_nBufferSize);
	        
	        if (m_nAudioReadCount != AudioRecord.ERROR_INVALID_OPERATION) {
	        	
	        	if (m_blnRecordSoundFile == true && os != null) {
                    try {                    	
                    	os.write(m_btAudioBuffer);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }      	        		  
	        	}
	        }

        }  //While
        
        
        if (m_blnRecordSoundFile == true && os != null) {
            try {
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        	
        }       
    	
    }
    
    
    private void calculateAudioSoundLevel() {
    	long nTotalVal;
    	int nLen;
    	short shtBuffer[];
    	
    	while (m_soundLevelThread != null && m_blnRecordSoundLevel == true) {
    		
    		if (m_nAudioReadCount != AudioRecord.ERROR_INVALID_OPERATION) {
	    		nTotalVal = 0;
	    		
	    		shtBuffer = DataUtil.Bytes2Shorts(m_btAudioBuffer, m_nAudioReadCount);
	        	
	    		nLen = shtBuffer.length;
	    		
	        	for (int i = 0; i < nLen; i++) {
	        		nTotalVal = nTotalVal + shtBuffer[i]*shtBuffer[i];
	        	}
	    				        	  
	        	double fMean = nTotalVal*1.0/nLen;
	        	  
	        	if (fMean == 0) {
	        		m_fSoundLevelDb = 1000;
	        	} else {
	        		m_fSoundLevelDb = 10*Math.log10(fMean);
	        	}
	         			        		        			    		
	    		if (Double.isInfinite(m_fSoundLevelDb) || Double.isNaN(m_fSoundLevelDb)) {
	    			
	    		} else {
	    			recordSoundLevel(m_fSoundLevelDb);
	    		}
	    		
    		}  // if
    		
        	try {
        		Thread.sleep(100);
        	} catch (InterruptedException e) {
        		  
        	}
    	}  // while
    }
           
    
	private void writeAudioDataToFile(){
	    byte data[] = new byte[m_nBufferSize];
	    String sFullAudioPathFileTmp = m_sRecordFullPathFile + ".wav.tmp"; 
	    FileOutputStream os = null;
	  
	    try {
	        os = new FileOutputStream(sFullAudioPathFileTmp);
	    } catch (FileNotFoundException e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
	    }
	      
	    int read = 0;
	      
	    if (os != null) {
	        while (m_audioRecorder != null) {
	            read = m_audioRecorder.read(data, 0, m_nBufferSize);
	                      
	            if (AudioRecord.ERROR_INVALID_OPERATION != read) {
	                try {
	                    os.write(data);
	                } catch (IOException e) {
	                    e.printStackTrace();
	                }
	            }
	        }
	              
	        try {
	            os.close();
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
	    }
	}
      

    private void stopAudioRecording() {
    	      	  
        if (m_audioRecorder != null) {
            int i = m_audioRecorder.getState();
              
            if (i == AudioRecord.STATE_INITIALIZED) {
            	m_audioRecorder.stop();
            	m_audioRecorder.release();
            }
              
            m_audioRecorder = null;
              
            if (m_blnRecordSoundLevel == true || m_blnRecordSoundFile == true) {
            	m_processSoundThread = null;
            	m_soundLevelThread = null;
            }
              
        }
          
        if (m_blnRecordSoundFile == true) {
        	copyWaveFile();
        	deleteTempFile();
        }
    }
      

    private void deleteTempFile() {
      	String sFullAudioPathFileTmp = m_sRecordFullPathFile + ".wav.tmp"; 
      	
        File file = new File(sFullAudioPathFileTmp);
          
        file.delete();
    }    
      
      
    private void copyWaveFile() {
        FileInputStream in = null;
        FileOutputStream out = null;
        long totalAudioLen = 0;
        long totalDataLen = totalAudioLen + 36;
        int channels = 2;
        long byteRate = 16 * m_nAudioSampleRate * channels/8;
        String sFullAudioPathFileTmp = m_sRecordFullPathFile + ".wav.tmp"; 
        String sFullAudioPathFile = m_sRecordFullPathFile + ".wav"; 
          
        byte[] data = new byte[m_nBufferSize];
          
        try {
            in = new FileInputStream(sFullAudioPathFileTmp);
            out = new FileOutputStream(sFullAudioPathFile);
            totalAudioLen = in.getChannel().size();
            totalDataLen = totalAudioLen + 36;
              
            WriteWaveFileHeader(out, totalAudioLen, totalDataLen,
            						m_nAudioSampleRate, channels, byteRate);
              
            while (in.read(data) != -1){
                out.write(data);
            }
                  
            in.close();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }    
     
      
    private void WriteWaveFileHeader(FileOutputStream out, long totalAudioLen,
            						long totalDataLen, long longSampleRate, int channels,
            						long byteRate) throws IOException {
      
    	byte[] header = new byte[44];
    	    
    	header[0] = 'R';  // RIFF/WAVE header
    	header[1] = 'I';
    	header[2] = 'F';
    	header[3] = 'F';
    	header[4] = (byte) (totalDataLen & 0xff);
    	header[5] = (byte) ((totalDataLen >> 8) & 0xff);
    	header[6] = (byte) ((totalDataLen >> 16) & 0xff);
    	header[7] = (byte) ((totalDataLen >> 24) & 0xff);
    	header[8] = 'W';
    	header[9] = 'A';
    	header[10] = 'V';
    	header[11] = 'E';
    	header[12] = 'f';  // 'fmt ' chunk
    	header[13] = 'm';
    	header[14] = 't';
    	header[15] = ' ';
    	header[16] = 16;  // 4 bytes: size of 'fmt ' chunk
    	header[17] = 0;
    	header[18] = 0;
    	header[19] = 0;
    	header[20] = 1;  // format = 1
    	header[21] = 0;
    	header[22] = (byte) channels;
    	header[23] = 0;
    	header[24] = (byte) (longSampleRate & 0xff);
    	header[25] = (byte) ((longSampleRate >> 8) & 0xff);
    	header[26] = (byte) ((longSampleRate >> 16) & 0xff);
    	header[27] = (byte) ((longSampleRate >> 24) & 0xff);
    	header[28] = (byte) (byteRate & 0xff);
    	header[29] = (byte) ((byteRate >> 8) & 0xff);
    	header[30] = (byte) ((byteRate >> 16) & 0xff);
    	header[31] = (byte) ((byteRate >> 24) & 0xff);
    	header[32] = (byte) (2 * 16 / 8);  // block align
    	header[33] = 0;
    	header[34] = 16;  // bits per sample
    	header[35] = 0;
    	header[36] = 'd';
    	header[37] = 'a';
    	header[38] = 't';
    	header[39] = 'a';
    	header[40] = (byte) (totalAudioLen & 0xff);
    	header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
    	header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
    	header[43] = (byte) ((totalAudioLen >> 24) & 0xff);
    	
    	out.write(header, 0, 44);
    }
      
        
}
