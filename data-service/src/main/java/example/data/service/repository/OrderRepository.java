package example.data.service.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import example.data.service.entity.Order;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
}