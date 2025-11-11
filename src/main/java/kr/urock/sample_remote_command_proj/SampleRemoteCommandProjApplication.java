package kr.urock.sample_remote_command_proj;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SampleRemoteCommandProjApplication {

    public static void main(String[] args) {
        SpringApplication.run(SampleRemoteCommandProjApplication.class, args);
    }

}
