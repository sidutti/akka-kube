package com.sidutti.kube.akka.config;

import akka.actor.ActorSystem;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.sidutti.kube.akka.config.SpringExtension.SPRING_EXTENSION_PROVIDER;

@Configuration
public class Configurations {
    @Bean
    public ActorSystem actorSystem(ApplicationContext applicationContext) {
        ActorSystem system = ActorSystem.create("sidutti-kube");
        SPRING_EXTENSION_PROVIDER.get(system).initialize(applicationContext);
        return system;
    }
}
