package com.example.sensingapp;

import java.nio.ByteOrder;

public class DataUtil {

	public DataUtil() {
		// TODO Auto-generated constructor stub
	}

	public static boolean testCPU() {  
        if (ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN) {  
            return true;  
        } else {  
            return false;  
        }  
    }
	
	
	public static short getShort(byte[] arrByteBuf, boolean blnBigEndian) {
		int i;
		short shtVal = 0;
        if (arrByteBuf == null) {  
            throw new IllegalArgumentException("byte array is null!");  
        }  
        
        if (arrByteBuf.length > 2) {  
            throw new IllegalArgumentException("byte array size > 2 !");  
        }  
        
        if (blnBigEndian) {  
            for (i = 0; i < arrByteBuf.length; i++) {  
            	shtVal <<= 8;  
            	shtVal |= (arrByteBuf[i] & 0x00ff);  
            }  
        } else {  
            for (i = arrByteBuf.length - 1; i >= 0; i--) {  
            	shtVal <<= 8;  
            	shtVal |= (arrByteBuf[i] & 0x00ff);  
            }  
        }  
  
        return shtVal;  
    }
	
	public static short getShort(byte[] btBuf) {  
        return getShort(btBuf, testCPU());  
    }  
    
    
	public static short[] Bytes2Shorts(byte[] arrBtBuf) {  
        byte btLen = 2;  
        
        short[] s = new short[arrBtBuf.length / btLen];  
        
        for (int i = 0; i < s.length; i++) {
        	
            byte[] temp = new byte[btLen];
            
            for (int j = 0; j < btLen; j++) {  
                temp[j] = arrBtBuf[i * btLen + j];  
            }
            
            s[i] = getShort(temp);  
        }
        
        return s;  
	}  
	
	public static short[] Bytes2Shorts(byte[] arrBtBuf, int nLen) {  
        byte btLen = 2;  
        
        short[] s = new short[nLen / btLen];  
        
        for (int i = 0; i < s.length; i++) {
        	
            byte[] temp = new byte[btLen];
            
            for (int j = 0; j < btLen; j++) {  
                temp[j] = arrBtBuf[i * btLen + j];  
            }
            
            s[i] = getShort(temp);  
        }
        
        return s;  
	}  
	
	
}
