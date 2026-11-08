package com.learning.model;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    private String key;
    private String value;
}
