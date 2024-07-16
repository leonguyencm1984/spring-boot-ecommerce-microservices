package com.nguyenna.orderservice.repository;

import com.nguyenna.orderservice.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductCountRepository extends JpaRepository<Product, Long> {
}
