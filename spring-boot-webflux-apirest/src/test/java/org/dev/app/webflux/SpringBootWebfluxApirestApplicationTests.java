package org.dev.app.webflux;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;

import java.net.URI;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.dev.app.webflux.documents.Categoria;
import org.dev.app.webflux.documents.Producto;
import org.dev.app.webflux.service.ProductoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.publisher.Mono;

// RANDOM_PORT LEVANTA UN SERVIDOR REAL
// MOCK LEVANTA UN SERVIDOR SIMULADO
@AutoConfigureWebTestClient
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class SpringBootWebfluxApirestApplicationTests {

	@Autowired
	private WebTestClient testClient;
	
	@Autowired
	@Qualifier("productoService")
	private ProductoService productoService;
	
	@Value("${config.base.endpoint}")
	private String baseEndPoint;
	
	@Value("${config.base.endpointRest}")
	private String baseEndPointRest;
	
	@Test
	void listarTest() {
		testClient.get()
				.uri(baseEndPointRest.concat("/listar"))
				.accept(MediaType.APPLICATION_JSON)
				.exchange()
				.expectStatus().isOk()
				.expectHeader().contentType(MediaType.APPLICATION_JSON)
				.expectBodyList(Producto.class)
				.consumeWith((response) -> {
					List<Producto> productos = response.getResponseBody();
					productos.forEach((p) -> {
						System.out.println(p.getNombre());
					});
					
					Assertions.assertThat(productos.size() > 0).isTrue();
					
				});
				//.hasSize(7);
	}

	@Test
	void verTest() {
		Producto producto = productoService.findByNombre("Bianchi Bicicleta")
											.block();
		testClient.get()
				.uri(baseEndPointRest.concat("/getOne/{id}"), Collections.singletonMap("id", producto.getId()))
				.accept(MediaType.APPLICATION_JSON)
				.exchange()
				.expectStatus().isOk()
				.expectHeader().contentType(MediaType.APPLICATION_JSON)
				.expectBody(Producto.class)
				.consumeWith((response) -> {
					Producto p = response.getResponseBody();
					Assertions.assertThat(p.getId()).isNotEmpty();
					Assertions.assertThat(p.getNombre()).isEqualTo("Bianchi Bicicleta");
					Assertions.assertThat(p.getId().length() > 0).isTrue();				
				});
				/*
				.expectBody()
				.jsonPath("$.id").isEmpty()
				.jsonPath("$.nombre").isEqualTo("TV PANASONIC PANTALLA LCD");
				*/
	}
	
	@Test
	void crearTest() {
		
		Categoria categoria = productoService.findCategoriaByNombre("Muebles").block();
		Producto producto = new Producto("Mesa Comedor", 100.00, categoria);
		
		testClient.post()
				.uri(baseEndPointRest.concat("/save"))
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.body(Mono.just(producto), Producto.class)
				.exchange()
				.expectStatus().isCreated()
				.expectHeader().contentType(MediaType.APPLICATION_JSON)
				.expectBody()
				.jsonPath("$.producto.id").isNotEmpty()
				.jsonPath("$.producto.nombre").isEqualTo("Mesa Comedor")
				.jsonPath("$.producto.categoria.nombre").isEqualTo("Muebles");
	}
	
	@Test
	void crear2Test() {
		Categoria categoria = productoService.findCategoriaByNombre("Muebles").block();
		Producto producto = new Producto("Mesa Comedor", 100.00, categoria);
		
		testClient.post()
				.uri(baseEndPointRest.concat("/save"))
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.body(Mono.just(producto), Producto.class)
				.exchange()
				.expectStatus().isCreated()
				.expectHeader().contentType(MediaType.APPLICATION_JSON)
				/*usando functional endpoint
				.expectBody(Producto.class)
				.consumeWith((response) -> {
					Producto p = response.getResponseBody();
					Assertions.assertThat(p.getId()).isNotEmpty();
					Assertions.assertThat(p.getNombre()).isEqualTo("Mesa Comedor");
					Assertions.assertThat(p.getCategoria().getNombre()).isEqualTo("Muebles");
				});
				*/
				//usando restcontroller
				.expectBody(new ParameterizedTypeReference<LinkedHashMap<String, Object>>() {})
				.consumeWith((response) -> {
					Object o = response.getResponseBody().get("producto");
					Producto p = new ObjectMapper().convertValue(o, Producto.class);
					Assertions.assertThat(p.getId()).isNotEmpty();
					Assertions.assertThat(p.getNombre()).isEqualTo("Mesa Comedor");
					Assertions.assertThat(p.getCategoria().getNombre()).isEqualTo("Muebles");
				});
	}
	
//	@Test
//	void editarTest() {
//		Producto producto = productoService.findByNombre("SONY NOTEBOOK")
//				.block();
//		Categoria categoria = productoService.findCategoriaByNombre("Computacion").block();
//		
//		Producto productoEditado = new Producto("ASUS NOTEBOOK", 1800.00, categoria);
//		
//		testClient.put()
//				.uri("/api/v2/productos/{id}", Collections.singletonMap("id", producto.getId()))
//				.contentType(MediaType.APPLICATION_JSON)
//				.accept(MediaType.APPLICATION_JSON)
//				.body(Mono.just(productoEditado), Producto.class)
//				.exchange()
//				.expectStatus().isCreated()
//				.expectHeader().contentType(MediaType.APPLICATION_JSON)
//				.expectBody()
//				.jsonPath("$.id").isNotEmpty()
//				.jsonPath("$.nombre").isEqualTo("SONY NOTEBOOK")
//				.jsonPath("$.categoria.nombre").isEqualTo("Electronico");
//	}
	
	@Test
	void eliminarTest() {
		Producto producto = productoService.findByNombre("Mica Comoda 5 Cajones")
				.block();
		
		testClient.delete()
			.uri(baseEndPointRest.concat("/delete/{id}"), Collections.singletonMap("id", producto.getId()))
			.exchange()
			.expectStatus().isNoContent()
			.expectBody()
			.isEmpty();
		
		testClient.get()
		.uri(baseEndPointRest.concat("/getOne/{id}"), Collections.singletonMap("id", producto.getId()))
		.exchange()
		.expectStatus().isNotFound()
		.expectBody()
		.isEmpty();
			
	}
}
