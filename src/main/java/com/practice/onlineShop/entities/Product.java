package com.practice.onlineShop.entities;

import com.practice.onlineShop.enums.Currencies;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

import static javax.persistence.EnumType.*;
import static javax.persistence.GenerationType.IDENTITY;

@Entity
@Setter
@Getter
public class Product {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    private long id;
    @Column(unique = true)
    private String code;
    private String description;
    private double price;
    @Enumerated(STRING)
    private Currencies currency;
    private int stock;
    private boolean valid;
}
