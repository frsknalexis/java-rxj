package org.dev.app.webflux.controllers;

import java.io.File;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Date;
import java.util.UUID;

import javax.validation.Valid;

import org.dev.app.webflux.documents.Categoria;
import org.dev.app.webflux.documents.Producto;
import org.dev.app.webflux.service.ProductoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;
import org.thymeleaf.spring5.context.webflux.ReactiveDataDriverContextVariable;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@SessionAttributes("producto")
@Controller
public class ProductoController {

	private static final Logger logger = LoggerFactory.getLogger(ProductoController.class);
	
	@Autowired
	@Qualifier("productoService")
	private ProductoService productoService;
	
	@Value("${config.uploads.path}")
	private String uploadPath;
	
	@ModelAttribute("categorias")
	public Flux<Categoria> categorias() {
		Flux<Categoria> categorias = productoService.findAllCategorias();
		return categorias;
	}
	
	@GetMapping("/uploads/img/{nombreFoto:.+}")
	public Mono<ResponseEntity<Resource>> verFoto(@PathVariable(value = "nombreFoto") String nombreFoto) throws MalformedURLException {
		
		Path ruta = Paths.get(uploadPath).resolve(nombreFoto).toAbsolutePath();
		
		Resource image = new UrlResource(ruta.toUri());
		return Mono.just(
					ResponseEntity.ok()
								 .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + image.getFilename() + "\"")
								 .body(image)
				);
	}
	
	@GetMapping("/ver/{id}")
	public Mono<String> ver(@PathVariable(value = "id") String id, Model model) {
		return productoService.findById(id)
							.doOnNext((p) -> {
								model.addAttribute("producto", p);
								model.addAttribute("titulo", "Detalle Producto");
							})
							.switchIfEmpty(Mono.just(new Producto()))
							.flatMap((p) -> {
								if (p.getId() == null) {
									return Mono.error(new InterruptedException("No existe el Producto"));
								}
								return Mono.just(p);
							})
							.then(Mono.just("ver"))
							.onErrorResume((ex) -> Mono.just("redirect:/listar?error=no+existe+el+producto"));
	}
	
	@GetMapping({"/listar", "/"})
	public Mono<String> listar(Model model) {
		Flux<Producto> productos = productoService.findAllNombresToUpperCase();
		productos.subscribe((prod) -> logger.info(prod.getNombre()));
		
		model.addAttribute("productos", productos);
		model.addAttribute("titulo", "Listado de Productos");
		return Mono.just("listar");
	}
	
	@GetMapping("/form")
	public Mono<String> crearForm(Model model) {
		model.addAttribute("producto", new Producto());
		model.addAttribute("titulo", "Formulario de producto");
		model.addAttribute("boton", "Crear");
		return Mono.just("form");
	}
	
	@GetMapping("/form-v2/{id}")
	public Mono<String> editarV2(@PathVariable(value = "id") String id, Model model) {
		return productoService.findById(id)
								.doOnNext((p) -> {
									logger.info("Producto: ".concat(p.getNombre()));
									model.addAttribute("producto", p);
									model.addAttribute("titulo", "Editar Producto");
									model.addAttribute("boton", "Editar");
								})
								.defaultIfEmpty(new Producto())
								.flatMap((p) -> {
									if (p.getId() == null) {
										return Mono.error(new InterruptedException("No existe el producto"));
									}
									return Mono.just(p);
								})
								.then(Mono.just("form"))
								.onErrorResume((ex) -> Mono.just("redirect:/listar?error=no+existe+el+producto"));
	}
	
	@GetMapping("/form/{id}")
	public Mono<String> editar(@PathVariable(value = "id") String id, Model model) {
		Mono<Producto> producto = productoService.findById(id)
													.doOnNext((p) -> {
														logger.info("Producto: ".concat(p.getNombre()));
													})
													.defaultIfEmpty(new Producto());
		model.addAttribute("producto", producto);
		model.addAttribute("titulo", "Editar Producto");
		model.addAttribute("boton", "Editar");
		return Mono.just("form");
	}
	
	@PostMapping("/form")
	public Mono<String> guardarProducto(@Valid Producto producto, BindingResult result, Model model,
			@RequestPart FilePart file, SessionStatus status) {
		
		if (result.hasErrors()) {
			model.addAttribute("titulo", "Errores en Formulario Producto");
			model.addAttribute("boton", "Guardar");
			return Mono.just("form");
		} else {
			status.setComplete();
			
			if (producto.getCreateAt() == null) {
				producto.setCreateAt(new Date());
			}
			
			Mono<Categoria> categoria = productoService
											.findCategoriaById(producto.getCategoria().getId());
			
			return categoria
							.flatMap((c) -> {
								
								if (producto.getCreateAt() == null) {
									producto.setCreateAt(new Date());
								}
								
								if (!file.filename().isEmpty()) {
									producto.setFoto(UUID.randomUUID().toString().concat("_")
															.concat(file.filename()
																		.replace(" ", "")
																		.replace(":", "")
																		.replace("\\", "")));
								}
								
								producto.setCategoria(c);
								return productoService.saveProducto(producto);
							})
							.doOnNext((p) -> {
								logger.info("Categoria asignada: ".concat(p.getCategoria().getNombre().concat(" Id: ".concat(p.getCategoria().getId()))));
								logger.info("Producto guardado: ".concat(p.getNombre()).concat(" Id: ".concat(producto.getId())));
							})
							.flatMap((p) -> {
								if (!file.filename().isEmpty()) {
									return file.transferTo(new File(uploadPath + p.getFoto()));
								}
								return Mono.empty();
							})
							// retornamos un mono de tipo string
							.then(Mono.just("redirect:/listar?success=producto+guardado+con+exito"));
		}
	}
	
	@GetMapping("/eliminar/{id}")
	public Mono<String> eliminar(@PathVariable(value = "id") String id) {
		return productoService.findById(id)
								.defaultIfEmpty(new Producto())
								.flatMap((p) -> {
									if (p.getId() == null) {
										return Mono.error(new InterruptedException("No existe el Producto a Eliminar"));
									}
									return Mono.just(p);
								})
								.flatMap((p) -> {
									logger.info("Eliminando Producto ".concat(p.getNombre()));
									logger.info("Eliminando Producto Id: ".concat(p.getId()));
									return productoService.delete(p);
								})
								.then(Mono.just("redirect:/listar?success=producto+eliminado+con+exito"))
								.onErrorResume((ex) -> Mono.just("redirect:/listar?error=no+existe+el+producto+a+eliminar"));
	}
	
	@GetMapping("/listar-datadriver")
	public String listarDataDriver(Model model) {
		Flux<Producto> productos = productoService
										.findAllNombresToUpperCase()
										.delayElements(Duration.ofSeconds(1));
		
		model.addAttribute("productos", new ReactiveDataDriverContextVariable(productos, 1));
		model.addAttribute("titulo", "Listado de Productos");
		return "listar";
	}
	
	@GetMapping("/listar-full")
	public String listarFull(Model model) {
		Flux<Producto> productos = productoService
										.findAllNombresToUpperCaseRepeat();
		
		model.addAttribute("productos", productos);
		model.addAttribute("titulo", "Listado de Productos");
		return "listar";
	}
	
	@GetMapping("/listar-chunked")
	public String listarChunked(Model model) {
		Flux<Producto> productos = productoService
										.findAllNombresToUpperCaseRepeat();
		
		model.addAttribute("productos", productos);
		model.addAttribute("titulo", "Listado de Productos");
		return "listar-chunked";
	}
}