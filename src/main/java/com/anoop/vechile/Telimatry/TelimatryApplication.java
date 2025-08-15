package com.anoop.vechile.Telimatry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka
public class TelimatryApplication {

	public static void main(String[] args) {
		SpringApplication.run(TelimatryApplication.class, args);
	}

}
