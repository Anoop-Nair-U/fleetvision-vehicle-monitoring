package com.anoop.vechile.Telimatry.Controller;


import com.anoop.vechile.Telimatry.Entity.User;
import com.anoop.vechile.Telimatry.KafkaProducer.KafkaProducerService;
import com.anoop.vechile.Telimatry.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Value("${kafka.user.topic}")
    private String userTopic;
    private final UserRepository userRepository;
    private final KafkaProducerService kafkaProducerService;

    public UserController(UserRepository userRepository, KafkaProducerService kafkaProducerService) {
        this.userRepository = userRepository;
        this.kafkaProducerService = kafkaProducerService;
    }

    @PostMapping
    public User createUser(@RequestBody User user) {
        User savedUser = userRepository.save(user);

        // Send event to Kafka after saving user
        kafkaProducerService.sendMessage(userTopic, "New user created: " + savedUser.getName());

        return savedUser;
    }
}
