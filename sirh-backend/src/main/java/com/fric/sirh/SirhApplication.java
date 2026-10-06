// src/main/java/com/fric/sirh/SirhApplication.java

package com.fric.sirh;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication(exclude = {
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class
})
@ComponentScan(basePackages = {"com.fric.sirh"})
public class SirhApplication {

    public static void main(String[] args) {
        SpringApplication.run(SirhApplication.class, args);
    }
}