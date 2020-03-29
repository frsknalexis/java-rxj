package org.dev.app.webflux;

import org.dev.app.webflux.handler.ProductoHandler;
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
				.route(RequestPredicates
							.GET("/api/v2/productos")
							.or(RequestPredicates
							.GET("/api/v3/productos")), 
								request -> {
									return handler.listarProductos(request);
									/*
									return ServerResponse.ok()
													.contentType(MediaType.APPLICATION_JSON)
													.body(productoService.findAll(), Producto.class);
									*/
								})
				.andRoute(RequestPredicates.GET("/api/v2/productos/{id}"), 
							request -> {
								return handler.verDetalle(request);
								})
				.andRoute(RequestPredicates.POST("/api/v2/productos"),
							request -> {
								return handler.saveProducto(request);
								})
				.andRoute(RequestPredicates.PUT("/api/v2/productos/{id}"),
							request -> {
								return handler.editarProducto(request);
								})
				.andRoute(RequestPredicates.DELETE("/api/v2/productos/{id}"),
							request -> {
								return handler.eliminarProducto(request);
							})
				.andRoute(RequestPredicates.POST("/api/v2/productos/upload/{id}"), 
							request -> {
								return handler.upload(request);
							})
				.andRoute(RequestPredicates.POST("/api/v2/productos/crear"), 
							request -> {
								return handler.crearConFoto(request);
							});
	}
}
