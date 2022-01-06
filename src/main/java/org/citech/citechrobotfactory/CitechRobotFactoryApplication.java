package org.citech.citechrobotfactory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"aop", "service","repository"})
public class CitechRobotFactoryApplication {

  public static void main(String[] args) {
    SpringApplication.run(CitechRobotFactoryApplication.class, args);
  }
}
