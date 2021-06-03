package test;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import test.FHIRUtil.FHIRResponse;

public class Test1 {
public static void main(String[] args) throws KeyManagementException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, CertificateException, IOException {
	FHIRUtil util=new FHIRUtil();
	String fhirEndPoint = "https://localhost:8243/1.0/1";
	String jsonPayload = "{ "
			+ "}";
	try {
		FHIRResponse postObservation = util.postObservation(fhirEndPoint, jsonPayload);
		System.out.println(postObservation.getJson());
		System.out.println(postObservation.getError());
	} catch (Exception e) {
		System.out.println(e.getMessage());
	}
	
}
}
