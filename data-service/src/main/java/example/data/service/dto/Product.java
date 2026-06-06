package example.data.service.dto;

import lombok.Data;

@Data
public class Product { // Added 'public'
    private String title;
    private double price;
}