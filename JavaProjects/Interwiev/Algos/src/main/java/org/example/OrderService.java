//package org.example;
//import java.math.BigDecimal;
//import java.util.*;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//import java.util.stream.Collectors;
//
////@Service
//public class OrderService {
//
//    private final Map<UUID, Order> orders = new HashMap<>();
//    private final ExecutorService executorService = Executors.newFixedThreadPool(5);
//
//    public void addOrder(Order order) {
//        if (orders.containsKey(order.getId())) {
//            throw new IllegalArgumentException("Order with ID already exists: " + order.getId());
//        }
//        orders.put(order.getId(), order);
//    }
//
//    public void updateOrderStatus(UUID orderId, OrderStatus status) {
//        Order order = orders.get(orderId);
//        if (order == null) {
//            throw new RuntimeException("Order not found");
//        }
//        order.setStatus(status);
//    }
//
//    public List<Order> getOrdersByStatus(OrderStatus status) {
//        return orders.values().stream()
//                .filter(order -> order.getStatus().equals(status))
//                .collect(Collectors.toList());
//    }
//
//    public void processOrders() {
//        for (Order order : orders.values()) {
//            if (order.getStatus() == OrderStatus.NEW) {
//                executorService.submit(() -> {
//                    try {
//                        order.setStatus(OrderStatus.IN_PROGRESS);
//                        Thread.sleep(1000); // Simulate processing
//                        order.setStatus(OrderStatus.COMPLETED);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                });
//            }
//        }
//    }
//
//    public List<Order> getTopOrders() {
//        return orders.values().stream()
//                .sorted((o1, o2) -> o2.getTotalPrice().compareTo(o1.getTotalPrice()))
//                .limit(3)
//                .collect(Collectors.toList());
//    }
//}
//
//class Order {
//    private UUID id;
//    private String customerName;
//    private List<String> items;
//    private BigDecimal totalPrice;
//    private OrderStatus status;
//
//
//}
//
//enum OrderStatus {
//    NEW, IN_PROGRESS, COMPLETED, CANCELLED
//}
