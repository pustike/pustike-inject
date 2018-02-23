/*
 * Copyright (C) 2016-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.pustike.inject.events;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import io.github.pustike.inject.Injector;
import io.github.pustike.inject.Injectors;

public class EventBusTest {
    private Injector injector;

    @Before
    public void setUp() {
        injector = Injectors.create(EventBus.createModule(), binder -> {
            binder.setDefaultScope(Singleton.class);
            binder.bind(OrderService.class);
            binder.bind(DeliveryService.class);
        });
    }

    @After
    public void tearDown() {
        injector.getIfPresent(EventBus.class).ifPresent(EventBus::close);
        Injectors.dispose(injector);
    }

    @Test
    public void testEventBus() {
        Order order = new Order("1", "Customer1");
        OrderService orderService = injector.getInstance(OrderService.class);
        DeliveryService deliveryService = injector.getInstance(DeliveryService.class);
        orderService.createOrder(order);
        Assert.assertEquals(1, deliveryService.eventCount);
        orderService.confirmOrder(order);
        Assert.assertEquals(2, deliveryService.eventCount);
    }

    public static class OrderService {
        private final EventBus eventBus;

        @Inject
        public OrderService(EventBus eventBus) {
            this.eventBus = eventBus;
        }

        private void createOrder(Order order) {
            eventBus.publish(new Event<>(order, "Created"));
        }

        private void confirmOrder(Order order) {
            eventBus.publish(new Event<>(order, "Confirmed"));
        }
    }

    public static class DeliveryService {
        private int eventCount = 0;
        @Observes
        private void observeEvent(Event<Order> event) {
            eventCount++;
            // System.out.println(event.getSource() + " is " + event.getContext());
        }
    }

    private static class Order {
        private final String orderId;
        private final String customer;

        private Order(String orderId, String customer) {
            this.orderId = orderId;
            this.customer = customer;
        }

        @Override
        public String toString() {
            return "Order{" + "orderId='" + orderId + '\'' + ", customer='" + customer + '\'' + '}';
        }
    }
}
