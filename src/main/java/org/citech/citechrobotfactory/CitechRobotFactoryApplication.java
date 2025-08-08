package org.citech.citechrobotfactory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

import java.util.Arrays;

@SpringBootApplication
@ComponentScan(basePackages = {"aop", "service", "repository", "controller"})
//@EnableJpaRepositories(basePackages="controller")
public class CitechRobotFactoryApplication {

    public static void main(String[] args) {
        Arrays.stream(new int[]{1,2,3}).max();
        SpringApplication.run(CitechRobotFactoryApplication.class, args);
    }
}
