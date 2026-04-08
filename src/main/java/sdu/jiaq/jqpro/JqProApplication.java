package sdu.jiaq.jqpro;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class JqProApplication {

    public static void main(String[] args) {
        SpringApplication.run(JqProApplication.class, args);
    }

}
