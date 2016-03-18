<?php
$host = "xxxx";
$username = "xxxx";
$password = "xxxx";
$db_name = "xxxx";

$projectname = $_POST['projectname'];
$username = $_POST['username'];
$timestamp = (int)$_POST['timestamp'];
$label = $_POST['label'];
$datatype = (int)$_POST['datatype'];
$acclX = (double)$_POST['acclX'];
$acclY = (double)$_POST['acclY'];
$acclZ = (double)$_POST['acclZ'];
$linearAcclX = (double)$_POST['linearAcclX'];
$linearAcclY = (double)$_POST['linearAcclY'];
$linearAcclZ = (double)$_POST['linearAcclZ'];
$gyroX = (double)$_POST['gyroX'];
$gyroY = (double)$_POST['gyroY'];
$gyroZ = (double)$_POST['gyroZ'];
$orientX = (double)$_POST['orientX'];
$orientY = (double)$_POST['orientY'];
$orientZ = (double)$_POST['orientZ'];
$magnetX = (double)$_POST['magnetX'];
$magnetY = (double)$_POST['magnetY'];
$magnetZ = (double)$_POST['magnetZ'];
$light = (double)$_POST['light'];
$barometer = (double)$_POST['barometer'];
$soundlevel = (double)$_POST['soundlevel'];
$cellid = (int)$_POST['cellid'];
$gpsLat = (double)$_POST['gpsLat'];
$gpsLong = (double)$_POST['gpsLong'];
$gpsAlt = (double)$_POST['gpsAlt'];
$geoDeclination = (double)$_POST['geoDeclination'];
$geoInclination = (double)$_POST['geoInclination'];
$gpsSpeed = (double)$_POST['gpsSpeed'];
$gpsBearing = (double)$_POST['gpsBearing'];
$wifiSSID1 = $_POST['wifiSSID1'];
$wifiBSSID1 = $_POST['wifiBSSID1'];
$wifiRSS1 = (int)$_POST['wifiRSS1'];
$wifiSSID2 = $_POST['wifiSSID2'];
$wifiBSSID2 = $_POST['wifiBSSID2'];
$wifiRSS2 = (int)$_POST['wifiRSS2'];
$wifiSSID3 = $_POST['wifiSSID3'];
$wifiBSSID3 = $_POST['wifiBSSID3'];
$wifiRSS3 = (int)$_POST['wifiRSS3'];
$wifiSSID4 = $_POST['wifiSSID4'];
$wifiBSSID4 = $_POST['wifiBSSID4'];
$wifiRSS4 = (int)$_POST['wifiRSS4'];
$wifiSSID5 = $_POST['wifiSSID5'];
$wifiBSSID5 = $_POST['wifiBSSID5'];
$wifiRSS5 = (int)$_POST['wifiRSS5'];
$wifiSSID6 = $_POST['wifiSSID6'];
$wifiBSSID6 = $_POST['wifiBSSID6'];
$wifiRSS6 = (int)$_POST['wifiRSS6'];
$wifiSSID7 = $_POST['wifiSSID7'];
$wifiBSSID7 = $_POST['wifiBSSID7'];
$wifiRSS7 = (int)$_POST['wifiRSS7'];
$wifiSSID8 = $_POST['wifiSSID8'];
$wifiBSSID8 = $_POST['wifiBSSID8'];
$wifiRSS8 = (int)$_POST['wifiRSS8'];
$wifiSSID9 = $_POST['wifiSSID9'];
$wifiBSSID9 = $_POST['wifiBSSID9'];
$wifiRSS9 = (int)$_POST['wifiRSS9'];
$wifiSSID10 = $_POST['wifiSSID10'];
$wifiBSSID10 = $_POST['wifiBSSID10'];
$wifiRSS10 = (int)$_POST['wifiRSS10'];

mysql_connect("$host", "$username", "$password") or die("cannot connect");
mysql_select_db($db_name) or die("cannot select DB");

if (!empty($projectname)) {
        $tbl_name = "SensorDataTbl";
        $sql = "insert into $tbl_name values('$projectname', '$username', $timestamp, '$label',$datatype,$acclX,$acclY,$acclZ,$linearAcclX,$linearAcclY,$linearAcclZ,$gyroX,$gyroY,$gyroZ,$orientX,$orientY,$orientZ,$magnetX,$magnetY,$magnetZ,$light,$barometer,$soundlevel,$cellid,$gpsLat,$gpsLong,$gpsAlt,$geoDeclination,$geoInclination,$gpsSpeed,$gpsBearing,'$wifiSSID1','$wifiBSSID1',$wifiRSS1,'$wifiSSID2','$wifiBSSID2',$wifiRSS2,'$wifiSSID3','$wifiBSSID3',$wifiRSS3,'$wifiSSID4','$wifiBSSID4',$wifiRSS4,'$wifiSSID5','$wifiBSSID5',$wifiRSS5,'$wifiSSID6','$wifiBSSID6',$wifiRSS6,'$wifiSSID7','$wifiBSSID7',$wifiRSS7,'$wifiSSID8','$wifiBSSID8',$wifiRSS8,'$wifiSSID9','$wifiBSSID9',$wifiRSS9,'$wifiSSID10','$wifiBSSID10',$wifiRSS10)";
        $result = mysql_query($sql);
        print "Inserted!";
} else {
        print "Not inserted!";
}

?>
