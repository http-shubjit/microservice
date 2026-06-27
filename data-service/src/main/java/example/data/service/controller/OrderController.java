package example.data.service.controller;

import example.data.service.entity.Order;
import example.data.service.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping("/my-orders")
    public ResponseEntity<List<Order>> getMyOrders(
            @RequestHeader("X-User-Email") String userEmail) {

        List<Order> orders = orderService.getOrdersByUser(userEmail);

        return ResponseEntity.ok(orders);
    }
}