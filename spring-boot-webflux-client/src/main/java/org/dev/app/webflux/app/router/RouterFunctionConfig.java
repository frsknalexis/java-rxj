package org.dev.app.webflux.app.router;

import org.dev.app.webflux.app.handler.ProductoHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration
public class RouterFunctionConfig {

	@Bean
	public RouterFunction<ServerResponse> routes(ProductoHandler handler) {
		return RouterFunctions
				.route(RequestPredicates.GET("/api/v1/cliente/productos"), handler::listarProducto)
				.andRoute(RequestPredicates.GET("/api/v1/cliente/productos/{id}"), handler::verProducto)
				.andRoute(RequestPredicates.POST("/api/v1/cliente/productos"), handler::crearProducto)
				.andRoute(RequestPredicates.PUT("/api/v1/cliente/productos/{id}"), handler::editarProducto)
				.andRoute(RequestPredicates.DELETE("/api/v1/cliente/productos/{id}"), handler::deleteProducto)
				.andRoute(RequestPredicates.POST("/api/v1/cliente/productos/upload/{id}"), handler::upload);
	}
}
