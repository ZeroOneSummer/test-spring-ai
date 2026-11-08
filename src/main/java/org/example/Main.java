package org.example;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.ApplicationListener;

import java.text.MessageFormat;

@EnableDiscoveryClient
@SpringBootApplication
public class Main implements ApplicationListener<ApplicationReadyEvent> {

    @Value("${server.port:-1}")
    Integer port;

    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        System.out.println(MessageFormat.format("===========================================================\n\t\t\t \u001b[37;42m {0} 启动成功！port: {1,number,#}\u001b[0m \n===========================================================",
                this.getClass().getSimpleName().split("\\$\\$")[0], port));
    }
}