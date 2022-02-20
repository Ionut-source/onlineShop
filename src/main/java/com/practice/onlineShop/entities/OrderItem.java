package com.practice.onlineShop.entities;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

import static javax.persistence.GenerationType.IDENTITY;

@Entity
@Setter
@Getter
public class OrderItem {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private long id;

    @OneToOne
    private Product product;
    private int quantity;

}
