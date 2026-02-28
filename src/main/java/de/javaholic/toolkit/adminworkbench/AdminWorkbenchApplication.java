package de.javaholic.toolkit.adminworkbench;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@EnableAspectJAutoProxy
@SpringBootApplication
@ComponentScan("de.javaholic.toolkit")
public class AdminWorkbenchApplication {
    public static void main(String[] args) {
        SpringApplication.run(AdminWorkbenchApplication.class, args);
    }
}
