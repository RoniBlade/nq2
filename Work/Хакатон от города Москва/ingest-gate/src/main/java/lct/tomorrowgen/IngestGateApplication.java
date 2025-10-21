package lct.tomorrowgen;

import lct.tomorrowgen.adapter.IngestGateAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"lct.tomorrowgen"})
@Slf4j
public class IngestGateApplication implements CommandLineRunner {

    @Autowired
    private IngestGateAdapter adapter;

    public static void main(String[] args) {
        SpringApplication.run(IngestGateApplication.class, args);
    }

    @Override
    public void run(String[] args) {
        adapter.run();
    }
}