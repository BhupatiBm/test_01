package test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;

import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Properties;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import java.net.URL;
import java.lang.StringBuffer;




public class FHIRUtil {
	
	

       public class FHIRResponse {
		   private int responseCode;
		   private String json;
		   private String error;
	
		   public FHIRResponse(int responseCode,String json,String error) {
		       this.responseCode = responseCode;
		       this.json = json;
		       this.error = error;
		   }
		   
		   public int getResponseCode() {
		       return this.responseCode;
		   }
	
		   public String getError() {
		       return this.error;
		   }
	
		   public String getJson() {
		       return this.json;
		   }
       }
	

    public FHIRResponse postObservation(String fhirEndPoint, String jsonPayload) throws IOException, java.security.NoSuchAlgorithmException, java.security.KeyManagementException, UnrecoverableKeyException, KeyStoreException, CertificateException {
		URL url = new URL (fhirEndPoint);
		HttpsURLConnection con=null;
		SSLSocketFactory sslSocketFactory=null;
		try {
			Properties properties = getProperties();
			String p12file=properties.getProperty("p12file");
			String p12password=properties.getProperty("p12password");
			sslSocketFactory = getFactory(p12file, p12password);
		}catch(Exception e) {
			System.out.println("Exception occurred while creating Mutual SSL Context::"+e.getMessage());
			System.out.println("Will use normal SSL Context");
			SSLContext sslContext = SSLContext.getInstance("TLS");
	        sslContext.init(null, null, new SecureRandom());
	        sslSocketFactory = sslContext.getSocketFactory();
		}
		
        con = (HttpsURLConnection)url.openConnection();
		con.setSSLSocketFactory(sslSocketFactory);
		con.setRequestMethod("POST");
		con.setRequestProperty("Content-Type", "application/json; utf-8");
		con.setRequestProperty("Accept", "application/json");
		con.setDoOutput(true);
		OutputStream os = con.getOutputStream();
		byte[] input = jsonPayload.getBytes("utf-8");
		os.write(input, 0, input.length);
		os.flush();
		os.close();
		
		int code = con.getResponseCode();


		String json=null;
		String error=null;
		if (code > 299) {
		    BufferedReader in = new BufferedReader(new InputStreamReader(con.getErrorStream()));
		    String inputLine;
		    StringBuffer response = new StringBuffer();

		    while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		    }
		    in.close();
		    error = response.toString();
		} else {
		    BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
		    String inputLine;
		    StringBuffer response = new StringBuffer();

		    while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		    }
		    in.close();
		    json = response.toString();
		}
		
		return new FHIRResponse(code,json,error);
		
	}
	

	public Properties getProperties() throws FileNotFoundException, IOException{
		Properties props = new Properties();
		InputStream inputStream = getClass().getClassLoader().getSystemResourceAsStream("vcloudutility.properties");
		if (inputStream != null) {
			props.load(inputStream);
		} else {
			throw new FileNotFoundException("property file not found in the classpath");
		}
		return props;
	}

	private SSLSocketFactory getFactory(String pKeyFile, String pKeyPassword) throws NoSuchAlgorithmException, KeyStoreException, CertificateException, IOException, UnrecoverableKeyException, KeyManagementException{
		KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
		KeyStore keyStore = KeyStore.getInstance("PKCS12");
		ClassLoader classLoader = getClass().getClassLoader();
		InputStream keyInput = classLoader.getResourceAsStream(pKeyFile);
		keyStore.load(keyInput, pKeyPassword.toCharArray());
		//keyInput.close();
		keyManagerFactory.init(keyStore, pKeyPassword.toCharArray());
		SSLContext context = SSLContext.getInstance("TLS");
		context.init(keyManagerFactory.getKeyManagers(), getTrustManager(), new SecureRandom());
		return context.getSocketFactory();
	}

	private TrustManager[] getTrustManager() {
		TrustManager[] trustAllCerts = new TrustManager[] { 
				new X509TrustManager() {     
					public java.security.cert.X509Certificate[] getAcceptedIssuers() { 
						return new X509Certificate[0];
					} 

					public void checkClientTrusted( 
							java.security.cert.X509Certificate[] certs, String authType) {
					} 

					public void checkServerTrusted( 
							java.security.cert.X509Certificate[] certs, String authType) {
					}
				} 
		}; 
		return trustAllCerts;
	}
}