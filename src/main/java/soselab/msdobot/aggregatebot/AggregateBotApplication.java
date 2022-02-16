package soselab.msdobot.aggregatebot;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import soselab.msdobot.aggregatebot.Entity.RasaIntent;
import soselab.msdobot.aggregatebot.Service.Orchestrator;
import soselab.msdobot.aggregatebot.Service.RasaService;

import java.util.Scanner;

@SpringBootApplication
public class AggregateBotApplication {

    @Autowired
    RasaService rasaService;
    @Autowired
    Orchestrator orchestrator;

    public static void main(String[] args) {
        SpringApplication.run(AggregateBotApplication.class, args);
    }

//    /**
//     * do stuff after spring boot application startup
//     */
//    @EventListener(ApplicationReadyEvent.class)
//    public void doStuffAfterStartUp(){
//        Scanner scanner = new Scanner(System.in);
//        System.out.print("Input user utterance: ");
//        do{
//            String utterance = scanner.next();
//            if(utterance.contains("exit")) break;
//
//            RasaIntent intent = rasaService.restrictedIntentParsing(rasaService.analyze(utterance));
//            orchestrator.skillSelector(intent);
//            System.out.print("Input user utterance: ");
//        }while(scanner.hasNext());
////        rasaService.fakeNLPComponent("");
//    }
}
