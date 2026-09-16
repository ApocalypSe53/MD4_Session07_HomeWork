package com.rikkei.medical.patientservice;

import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PatientServiceApplication {

	public static void main(String[] args) {
		// Pin the JVM default zone to the canonical IANA id: some PostgreSQL builds
		// don't recognize the legacy alias "Asia/Saigon" that Windows reports here,
		// and the JDBC driver always sends TimeZone.getDefault() to the server.
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
		SpringApplication.run(PatientServiceApplication.class, args);
	}

}
