package example.data.service.service;

import example.data.service.entity.Order;
import example.data.service.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;

    public List<Order> getOrdersByUser(String userEmail) {

        return orderRepository
                .findByUserEmailOrderByCreatedAtDesc(userEmail);
    }
}