package soselab.msdobot.aggregatebot;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import soselab.msdobot.aggregatebot.Service.Orchestrator;
import soselab.msdobot.aggregatebot.Service.RasaService;

@SpringBootApplication
public class AggregateBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(AggregateBotApplication.class, args);
    }
}
