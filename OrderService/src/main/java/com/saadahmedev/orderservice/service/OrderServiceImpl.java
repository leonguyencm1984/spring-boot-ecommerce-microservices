package com.saadahmedev.orderservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saadahmedev.orderservice.dto.*;
import com.saadahmedev.orderservice.dto.kafka.OrderEvent;
import com.saadahmedev.orderservice.dto.kafka.PaymentEvent;
import com.saadahmedev.orderservice.dto.orderResponse.OrderResponse;
import com.saadahmedev.orderservice.dto.product.Product;
import com.saadahmedev.orderservice.entity.*;
import com.saadahmedev.orderservice.feign.AuthService;
import com.saadahmedev.orderservice.feign.CartService;
import com.saadahmedev.orderservice.feign.ProductService;
import com.saadahmedev.orderservice.repository.OrderRepository;
import com.saadahmedev.orderservice.repository.PaymentStatusRepository;
import com.saadahmedev.orderservice.repository.ProductCountRepository;
import com.saadahmedev.orderservice.repository.ShippingDetailsRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class OrderServiceImpl implements OrderService, OnMessageReceived {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentStatusRepository paymentStatusRepository;

    @Autowired
    private ShippingDetailsRepository shippingDetailsRepository;

    @Autowired
    private ProductCountRepository productCountRepository;

    @Autowired
    private AuthService authService;

    @Autowired
    private CartService cartService;

    @Autowired
    private ProductService productService;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    private Logger logger = LoggerFactory.getLogger(OrderServiceImpl.class);

    @Override
    public ResponseEntity<?> createOrder(HttpServletRequest request, ShippingDetailsRequest shippingDetailsRequest) {
        long userId = getUserId(request);
        if (userId == -1) return userNotFound();

        List<Product> productList = cartService.getCartItems(userId).getBody();
        List<com.saadahmedev.orderservice.entity.Product> productCountList = new ArrayList<>();
        assert productList != null;
        if (productList.isEmpty()) return new ResponseEntity<>(new ApiResponse(false, "There are no products in your cart"), HttpStatus.BAD_REQUEST);

        double totalPrice = 0;
        double grandTotal = 0;

        for (Product product : productList) {
            productCountList.add(
                    com.saadahmedev.orderservice.entity.Product.builder()
                            .productId(product.getId())
                            .itemCount(product.getItemCount())
                            .build()
            );
            totalPrice += (product.getPrice() * product.getItemCount());
            double discountPrice;
            if (product.getDiscount() == 0) discountPrice = product.getPrice() * product.getItemCount();
            else {
                discountPrice = (product.getPrice() * product.getItemCount()) - (product.getPrice() * product.getItemCount() * product.getDiscount()) / 100;
            }
            grandTotal += discountPrice;
        }

        PaymentStatus paymentStatus = PaymentStatus.builder()
                .transactionId(UUID.randomUUID().toString())
                .status(PaymentStatusEnum.PENDING)
                .amount(grandTotal)
                .message(null)
                .payType(shippingDetailsRequest.getPayType())
                .build();

        ShippingDetails shippingDetails = ShippingDetails.builder()
                .name(shippingDetailsRequest.getName())
                .email(shippingDetailsRequest.getEmail())
                .phone(shippingDetailsRequest.getPhone())
                .address(shippingDetailsRequest.getAddress())
                .build();

        Date creationTime = new Date();
        Order order = Order.builder()
                .userId(userId)
                .totalPrice(totalPrice)
                .discount(0)
                .grandTotal(grandTotal)
                .products(productCountList)
                .deliveryStatus(DeliveryStatus.PENDING)
                .shippingDetails(shippingDetails)
                .paymentStatus(paymentStatus) //This will come from payment service using Kafka (SAGA Choreography Architecture)
                .createdAt(creationTime)
                .updatedAt(creationTime)
                .build();

        try {
            productCountRepository.saveAll(productCountList);
            shippingDetailsRepository.save(shippingDetails);
            paymentStatusRepository.save(paymentStatus);
            cartService.removeCartByUserId(userId);
            Order savedOrder = orderRepository.save(order);

            OrderEvent orderEvent = OrderEvent.builder()
                    .orderId(savedOrder.getId())
                    .userId(userId)
                    .amount(grandTotal)
                    .payType(shippingDetailsRequest.getPayType())
                    .message("An order has been created for user " + userId)
                    .build();

            String orderEventSerialized = new ObjectMapper().writeValueAsString(orderEvent);
            kafkaTemplate.send("order-event", orderEventSerialized);

            return new ResponseEntity<>(new OrderPlaceResponse(true, "Order placed successfully", savedOrder.getId()), HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(new ApiResponse(false, e.getLocalizedMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<?> getOrders(HttpServletRequest request) {
        long userId = getUserId(request);
        List<Order> orderList = orderRepository.findAllByUserIdOrderByIdDesc(userId);
        if (orderList.isEmpty()) return new ResponseEntity<>(new ArrayList<>(), HttpStatus.OK);

        List<OrderResponse> orderResponseList = new ArrayList<>();
        for (Order order : orderList) {
            orderResponseList.add(mapToOrderResponse(order));
        }

        return new ResponseEntity<>(orderResponseList, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<?> getOrder(HttpServletRequest request, long id) {
        long userId = getUserId(request);
        Optional<Order> optionalOrder = orderRepository.findByIdAndUserId(id, userId);
        if (optionalOrder.isEmpty()) return new ResponseEntity<>(new ApiResponse(false, "Order not found"), HttpStatus.BAD_REQUEST);

        return new ResponseEntity<>(mapToOrderResponse(optionalOrder.get()), HttpStatus.OK);
    }

    @Override
    @Transactional
    public ResponseEntity<?> deleteOrder(HttpServletRequest request, long id) {
        long userId = getUserId(request);
        Optional<Order> optionalOrder = orderRepository.findByIdAndUserId(id, userId);
        if (optionalOrder.isEmpty()) return new ResponseEntity<>(new ApiResponse(false, "Order not found"), HttpStatus.BAD_REQUEST);

        try {
            orderRepository.deleteByIdAndUserId(id, userId);
            return new ResponseEntity<>(new ApiResponse(true, "Order deleted successfully"), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(new ApiResponse(false, e.getLocalizedMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private OrderResponse mapToOrderResponse(Order order) {
        Map<Long, Integer> productItemCount = new HashMap<>();
        List<Long> productIds = new ArrayList<>();

        for (com.saadahmedev.orderservice.entity.Product product : order.getProducts()) {
            productItemCount.put(product.getProductId(), product.getItemCount());
            productIds.add(product.getProductId());
        }

        List<Product> productList = productService.getCartProducts(productIds).getBody();
        assert productList != null;
        for (Product product : productList) {
            product.setItemCount(productItemCount.get(product.getId()));
        }

        return OrderResponse.builder()
                .id(order.getId())
                .totalPrice(order.getTotalPrice())
                .discount(order.getDiscount())
                .grandTotal(order.getGrandTotal())
                .products(productList)
                .deliveryStatus(order.getDeliveryStatus())
                .shippingDetails(order.getShippingDetails())
                .paymentStatus(order.getPaymentStatus())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    private long getUserId(HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        UserResponse user = authService.getUser(new Token(token)).getBody();

        return user == null ? -1 : user.getId();
    }

    private ResponseEntity<?> userNotFound() {
        return new ResponseEntity<>(new ApiResponse(false, "User not found"), HttpStatus.BAD_REQUEST);
    }

    @Override
    public void onEvent(String event) throws JsonProcessingException {
        PaymentEvent paymentEvent = new ObjectMapper().readValue(event, PaymentEvent.class);
        logger.info(String.valueOf(paymentEvent));
        Order order = orderRepository.findById(paymentEvent.getOrderId()).get();
        order.getPaymentStatus().setStatus(paymentEvent.getPaymentStatus());
        order.getPaymentStatus().setMessage(paymentEvent.getMessage());

        try {
            orderRepository.save(order);
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }
}
