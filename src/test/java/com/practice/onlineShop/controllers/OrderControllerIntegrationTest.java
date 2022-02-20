package com.practice.onlineShop.controllers;

import com.practice.onlineShop.entities.OrderItem;
import com.practice.onlineShop.entities.Orders;
import com.practice.onlineShop.entities.Product;
import com.practice.onlineShop.entities.User;
import com.practice.onlineShop.repositories.OrderRepository;
import com.practice.onlineShop.utils.UtilsComponent;
import com.practice.onlineShop.vos.OrderVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.practice.onlineShop.enums.Roles.*;
import static com.practice.onlineShop.utils.UtilsComponent.LOCALHOST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpEntity.EMPTY;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@SpringBootTest(webEnvironment = RANDOM_PORT)
class OrderControllerIntegrationTest {

    @TestConfiguration
    static class ProductControllerIntegrationTestContextConfiguration {
        @Bean
        public RestTemplate restTemplateForPatch() {
            return new RestTemplate(new HttpComponentsClientHttpRequestFactory());
        }
    }

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private RestTemplate restTemplateForPatch;

    @Autowired
    private UtilsComponent utilsComponent;

    @Autowired
    private OrderRepository orderRepository;

    @Test
    @Transactional
    public void addOrder_whenOrderIsValid_shouldAddItToDB() {
        User user = utilsComponent.saveUserWithRole(CLIENT);
        Product product = utilsComponent.storeTwoProductsInDatabase("code1", "code2");

        OrderVO orderVO = utilsComponent.createOrderVO(user, product);

        testRestTemplate.postForEntity(LOCALHOST + port + "/order", orderVO, Void.class);

        List<Orders> ordersIterable = (List<Orders>) orderRepository.findAll();
        Optional<OrderItem> orderItemOptional = ordersIterable.stream()
                .map(order -> ((List<OrderItem>) order.getOrderItems()))
                .flatMap(List::stream)
                .filter(orderItem -> orderItem.getProduct().getId() == product.getId())
                .findFirst();

        assertThat(orderItemOptional).isPresent();
    }

    @Test
    public void addOrder_whenRequestIsMadeByAdmin_shouldThrowAnException() {
        User user = utilsComponent.saveUserWithRole(ADMIN);
        Product product = utilsComponent.storeTwoProductsInDatabase("code1ForAdmin", "code2ForAdmin");

        OrderVO orderVO = utilsComponent.createOrderVO(user, product);

        ResponseEntity<String> responseEntity = testRestTemplate.postForEntity(LOCALHOST + port + "/order", orderVO, String.class);

        assertThat(responseEntity.getStatusCode()).isEqualTo(BAD_REQUEST);
        assertThat(responseEntity.getBody()).isEqualTo("Utilizatorul nu are permisiunea de a executa aceasta operatiune!");
    }

    @Test
    public void addOrder_whenRequestIsMadeByExpeditor_shouldThrowAnException() {
        User user = utilsComponent.saveUserWithRole(EXPEDITOR);
        Product product = utilsComponent.storeTwoProductsInDatabase("code1ForExpeditor", "code2ForExpeditor");

        OrderVO orderVO = utilsComponent.createOrderVO(user, product);

        ResponseEntity<String> responseEntity = testRestTemplate.postForEntity(LOCALHOST + port + "/order", orderVO, String.class);

        assertThat(responseEntity.getStatusCode()).isEqualTo(BAD_REQUEST);
        assertThat(responseEntity.getBody()).isEqualTo("Utilizatorul nu are permisiunea de a executa aceasta operatiune!");
    }

    @Test
    public void deliver_whenHavingAnOrderWhichIsNotCanceled_shouldDeliverItByExpeditor() {
        User expeditor = utilsComponent.saveUserWithRole(EXPEDITOR);
        User client = utilsComponent.saveUserWithRole(CLIENT);
        Product product = utilsComponent.storeTwoProductsInDatabase("code1ForExpeditorForDeliver", "code2ForExpeditorForDeliver");

        Orders orderWitsProducts = utilsComponent.generateOrderItems(product, client);
        orderRepository.save(orderWitsProducts);

        restTemplateForPatch.exchange(LOCALHOST + port + "/order/" + orderWitsProducts.getId() + "/" + expeditor.getId(),
                PATCH, EMPTY, Void.class);

        Orders orderFromDb = orderRepository.findById(orderWitsProducts.getId()).get();

        assertThat(orderFromDb.isDelivered()).isTrue();
    }

    @Test
    public void deliver_whenHavingAnOrderWhichIsNotCanceled_shouldNOTDeliverItByAdmin() {
        User adminAsExpeditor = utilsComponent.saveUserWithRole(ADMIN);
        User client = utilsComponent.saveUserWithRole(CLIENT);
        Product product = utilsComponent.storeTwoProductsInDatabase("code1ForExpeditorForDeliverWhenAdmin", "code2ForExpeditorForDeliverWhenAdmin");

        Orders orderWitsProducts = utilsComponent.generateOrderItems(product, client);
        orderRepository.save(orderWitsProducts);

       // ResponseEntity<String> responseEntity = testRestTemplate.postForEntity(LOCALHOST + port + "/order", product, String.class);

        try {
            restTemplateForPatch.exchange(LOCALHOST + port + "/order/" + orderWitsProducts.getId() + "/" + adminAsExpeditor.getId(),
                    PATCH, EMPTY, Void.class);
        } catch (RestClientException exception) {
            assertThat(exception.getMessage()).isEqualTo("400 : [Utilizatorul nu are permisiunea de a executa aceasta operatiune!]");

//            assertThat(responseEntity.getStatusCode()).isEqualTo(BAD_REQUEST);
//            assertThat(responseEntity.getBody()).isEqualTo("Utilizatorul nu are permisiunea de a executa aceasta operatiune!");
        }
    }


    @Test
    public void deliver_whenHavingAnOrderWhichIsNotCanceled_shouldNOTDeliverItByClient() {
        User clientAsExpeditor = utilsComponent.saveUserWithRole(CLIENT);
        User client = utilsComponent.saveUserWithRole(CLIENT);
        Product product = utilsComponent.storeTwoProductsInDatabase("code10ForExpeditorForDeliverWhenClient", "code20ForExpeditorForDeliverWhenClient");

        Orders orderWitsProducts = utilsComponent.generateOrderItems(product, client);
        orderRepository.save(orderWitsProducts);

        try {
            restTemplateForPatch.exchange(LOCALHOST + port + "/order/" + orderWitsProducts.getId() + "/" + clientAsExpeditor.getId(),
                    PATCH, EMPTY, Void.class);
        } catch (RestClientException exception) {
            assertThat(exception.getMessage()).isEqualTo("400 : [Utilizatorul nu are permisiunea de a executa aceasta operatiune!]");
        }
    }

    @Test
    public void deliver_whenHavingAnOrderWhichIsCanceled_shouldThrowAnException() {
        User expeditor = utilsComponent.saveUserWithRole(EXPEDITOR);
        User client = utilsComponent.saveUserWithRole(CLIENT);
        Product product = utilsComponent.storeTwoProductsInDatabase("code1ForExpeditorForCanceledOrder", "code2ForExpeditorForCanceledOrder2");

        Orders orderWitsProducts = utilsComponent.generateOrderItems(product, client);
        orderWitsProducts.setCanceled(true);
        orderRepository.save(orderWitsProducts);

        try {
            restTemplateForPatch.exchange(LOCALHOST + port + "/order/" + orderWitsProducts.getId() + "/" + expeditor.getId(),
                    PATCH, EMPTY, Void.class);
        } catch (RestClientException exception) {
            assertThat(exception.getMessage()).isEqualTo("400 : [Comanda a fost anulata!]");
        }
    }

    @Test
    public void cancel_whenValidOrder_shouldCancelIt() {
        User client = utilsComponent.saveUserWithRole(CLIENT);
        Product product = utilsComponent.storeTwoProductsInDatabase("productForCanceledOrder1", "productForCanceledOrder2");
        Orders orderWithProducts = utilsComponent.generateOrderItems(product, client);
        orderRepository.save(orderWithProducts);

        restTemplateForPatch.exchange(LOCALHOST + port + "/order/cancel/" + orderWithProducts.getId() + "/" + client.getId(),
                PATCH, EMPTY, Void.class);

        Orders orderFromDb = orderRepository.findById(orderWithProducts.getId()).get();

        assertThat(orderFromDb.isCanceled()).isTrue();
    }

    @Test
    public void cancel_whenOrderIsAlreadySent_shouldThrowAnException() {
        User client = utilsComponent.saveUserWithRole(CLIENT);
        Product product = utilsComponent.storeTwoProductsInDatabase("productForCanceledSentOrder1", "productForCanceledSentOrder2");
        Orders orderWithProducts = utilsComponent.generateOrderItems(product, client);
        orderWithProducts.setDelivered(true);
        orderRepository.save(orderWithProducts);

        try {
            restTemplateForPatch.exchange(LOCALHOST + port + "/order/cancel/" + orderWithProducts.getId() + "/" + client.getId(),
                    PATCH, EMPTY, Void.class);
        } catch (RestClientException exception) {
            assertThat(exception.getMessage()).isEqualTo("400 : [Comanda a fost deja expediata!]");
        }

    }

    @Test
    public void cancel_whenUserIsAdmin_shouldThrowAnException() {
        User admin = utilsComponent.saveUserWithRole(ADMIN);
        Product product = utilsComponent.storeTwoProductsInDatabase("productForCanceledOrder16ForAdmin", "productForCanceledOrder27ForAdmin");
        Orders orderWithProducts = utilsComponent.generateOrderItems(product, admin);
        orderRepository.save(orderWithProducts);

        try {
            restTemplateForPatch.exchange(LOCALHOST + port + "/order/cancel/" + orderWithProducts.getId() + "/" + admin.getId(),
                    PATCH, EMPTY, Void.class);
        } catch (RestClientException exception) {
            assertThat(exception.getMessage()).isEqualTo("400 : [Utilizatorul nu are permisiunea de a executa acesta operatiune!]");
        }
    }


    @Test
    public void cancel_whenUserIsAnExpeditor_shouldThrowAnException() {
        User expeditor = utilsComponent.saveUserWithRole(EXPEDITOR);
        Product product = utilsComponent.storeTwoProductsInDatabase("productForCanceledOrder1ForExpeditor", "productForCanceledOrder2ForExpeditor");
        Orders orderWithProducts = utilsComponent.generateOrderItems(product, expeditor);
        orderRepository.save(orderWithProducts);

        try {
            restTemplateForPatch.exchange(LOCALHOST + port + "/order/cancel/" + orderWithProducts.getId() + "/" + expeditor.getId(),
                    PATCH, EMPTY, Void.class);
        } catch (RestClientException exception) {
            assertThat(exception.getMessage()).isEqualTo("400 : [Utilizatorul nu are permisiunea de a executa acesta operatiune!]");
        }
    }

    @Test
    @Transactional
    public void return_whenOrderIsValid_shouldReturnIt() {
        User client = utilsComponent.saveUserWithRole(CLIENT);
        Product product = utilsComponent.storeTwoProductsInDatabase("productForReturn11", "productForReturn22");
        Orders orderWithProducts = utilsComponent.saveDeliveredOrder(client, product);

        restTemplateForPatch.exchange(LOCALHOST + port + "/order/return/" + orderWithProducts.getId() + "/" + client.getId(),
                PATCH, EMPTY, Void.class);

        Orders orderFromDb = orderRepository.findById(orderWithProducts.getId()).get();

        assertThat(orderFromDb.isReturned()).isTrue();
        assertThat(orderFromDb.getOrderItems().get(0).getProduct().getStock()).isEqualTo(product.getStock() + orderWithProducts.getOrderItems().get(0).getQuantity());
    }


    @Test
    public void return_whenOrderIsNOTDelivered_shouldThrowException() {
        User client = utilsComponent.saveUserWithRole(CLIENT);
        Product product = utilsComponent.storeTwoProductsInDatabase("productForReturn100", "productForReturn200");
        Orders orderWithProducts = utilsComponent.saveOrder(client, product);

        try {
            restTemplateForPatch.exchange(LOCALHOST + port + "/order/return/" + orderWithProducts.getId() + "/" + client.getId(),
                    PATCH, EMPTY, Void.class);
        } catch (RestClientException exception) {
            assertThat(exception.getMessage()).isEqualTo("400 : [Comanda nu poate fi returnata deoarece nu a fost livrata!]");
        }
    }

    @Test
    public void return_whenOrderIsCanceled_shouldThrowException() {
        User client = utilsComponent.saveUserWithRole(CLIENT);
        Product product = utilsComponent.storeTwoProductsInDatabase("productForReturn11ForCanceledOrder", "productForReturn22ForCanceledOrder");
        Orders orderWithProducts = utilsComponent.saveCanceledAndDeliveredOrder(client, product);

        try {
            restTemplateForPatch.exchange(LOCALHOST + port + "/order/return/" + orderWithProducts.getId() + "/" + client.getId(),
                    PATCH, EMPTY, Void.class);
        } catch (RestClientException exception) {
            assertThat(exception.getMessage()).isEqualTo("400 : [Comanda a fost anulata!]");
        }
    }

    @Test
    public void return_whenUserIsAdmin_shouldThrowException() {
        User adminAsClient = utilsComponent.saveUserWithRole(ADMIN);
        Product product = utilsComponent.storeTwoProductsInDatabase("productForReturn1ForAdmin", "productForReturn2ForAdmin");
        Orders orderWithProducts = utilsComponent.saveDeliveredOrder(adminAsClient, product);

        try {
            restTemplateForPatch.exchange(LOCALHOST + port + "/order/return/" + orderWithProducts.getId() + "/" + adminAsClient.getId(),
                    PATCH, EMPTY, Void.class);
        } catch (RestClientException exception) {
            assertThat(exception.getMessage()).isEqualTo("400 : [Utilizatorul nu are permisiunea de a executa aceasta operatiune!]");
        }
    }

    @Test
    public void return_whenUserIsExpeditor_shouldThrowException() {
        User expeditorAsClient = utilsComponent.saveUserWithRole(EXPEDITOR);
        Product product = utilsComponent.storeTwoProductsInDatabase("productForReturn10ForReturnForOrderNotDelivered", "productForReturn20ForReturnForOrderNotDelivered");
        Orders orderWithProducts = utilsComponent.saveDeliveredOrder(expeditorAsClient, product);

        try {
            restTemplateForPatch.exchange(LOCALHOST + port + "/order/return/" + orderWithProducts.getId() + "/" + expeditorAsClient.getId(),
                    PATCH, EMPTY, Void.class);
        } catch (RestClientException exception) {
            assertThat(exception.getMessage()).isEqualTo("400 : [Utilizatorul nu are permisiunea de a executa aceasta operatiune!]");
        }
    }

}
