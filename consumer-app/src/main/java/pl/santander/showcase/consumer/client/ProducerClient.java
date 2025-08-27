package pl.santander.showcase.consumer.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import pl.santander.showcase.consumer.dto.DecisionResponse;

@FeignClient(name = "producer", url = "${producer.url:http://localhost:9090}")
public interface ProducerClient {
    @GetMapping("/api/producer/process")
    DecisionResponse process(@RequestParam("userId") String userId);
}
