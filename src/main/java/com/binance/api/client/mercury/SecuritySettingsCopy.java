package com.binance.api.client.mercury;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.binary.Base64;

public class SecuritySettingsCopy {

	/**
	   *   your sqlite database file path.
	   */
public static final String DBNAME = "c:/sqlite/data/binancetrade.db";

	  /**
	   *   favorite pairs.
	   */
 public static final String FUELETH = "FUELETH";
 public static final String FUELBTC = "FUELBTC";
 public static final String IOTABTC = "IOTABTC";
 public static final String IOTAETH = "IOTAETH";
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	

	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	

	  /**
	   * your Key .
	   */
   public static final String sKEY = "KEY";
   /**
	   * your Secret .
	   */
   public static final String sSECRET = "SECRET";
   
   public String ApiSecurityExample(String totalParams) {
	   //   @check the following Binance API documentation under for further details:
	   // 	Binance API Url: https://www.binance.com/restapipub.html#user-content-enum-definitions 	   
	     try {
	      Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
	      SecretKeySpec secret_key = new SecretKeySpec(sSECRET.getBytes(), "HmacSHA256");
	      sha256_HMAC.init(secret_key);
	      String hash = Base64.encodeBase64String(sha256_HMAC.doFinal(totalParams.getBytes()));
	      return hash;
	     }
	     catch (Exception e){
	    	 return null;
	     }
	 }
}
