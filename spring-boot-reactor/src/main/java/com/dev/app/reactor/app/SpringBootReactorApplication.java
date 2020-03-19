package com.dev.app.reactor.app;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.dev.app.reactor.app.model.Comentarios;
import com.dev.app.reactor.app.model.Usuario;
import com.dev.app.reactor.app.model.UsuarioComentarios;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@SpringBootApplication
public class SpringBootReactorApplication implements CommandLineRunner {

	private static final Logger logger = LoggerFactory.getLogger(SpringBootReactorApplication.class);
	
	public static void main(String[] args) {
		SpringApplication.run(SpringBootReactorApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		ejemploContrapresion();
	}
	
	public void ejemploContrapresion() {
		Flux.range(1, 10)
			.log()
			.limitRate(2)
			.subscribe();
			/*
			.subscribe(new Subscriber<Integer>() {

				private Subscription s;
				
				private Integer limite = 5;
				private Integer consumido = 0;
				
				@Override
				public void onSubscribe(Subscription s) {
					this.s = s;
					s.request(limite);
				}

				@Override
				public void onNext(Integer t) {
					logger.info(t.toString());
					consumido++;
					if (consumido == limite) {
						consumido = 0;
						s.request(limite);
					}
				}

				@Override
				public void onError(Throwable t) {
					// TODO Auto-generated method stub
					
				}

				@Override
				public void onComplete() {
					// TODO Auto-generated method stub
					
				}
			});
			*/
	}
	
	public void ejemploIntervalDesdeCreate() {
		Flux.create(emitter -> {
			Timer timer = new Timer();
			timer.schedule(new TimerTask() {
				
				private Integer contador = 0;
				
				@Override
				public void run() {
					emitter.next(++contador);
					
					if (contador == 10) {
						timer.cancel();
						emitter.complete();
					}
					if (contador == 5) {
						timer.cancel();
						emitter.error(new InterruptedException("Error, se ha detenido el flux en 5!"));
					}
				}
			}, 1000, 1000);
		})
		//.doOnNext(next -> logger.info(next.toString()))
		//.doOnComplete(() -> logger.info("Hemos terminado"))
		.subscribe(next -> logger.info(next.toString()),
				(error) -> logger.error(error.getMessage()),
				() -> logger.info("Hemos terminado"));
	}
	
	public void ejemploIntervaloInfinito() throws InterruptedException {
		
		CountDownLatch latch = new CountDownLatch(1);
		
		Flux.interval(Duration.ofSeconds(1))
			.doOnTerminate(() -> latch.countDown())
			.flatMap((i) -> {
				if (i >= 5) {
					return Flux.error(new InterruptedException("Solo hasta 5!"));
				}
				return Flux.just(i);
			})
			.map(i -> "Hola " + i)
			.retry(2)
			//.doOnNext(s -> logger.info(s))
			.subscribe((s) -> logger.info(s),
					(error) -> logger.error(error.getMessage()));
		
		latch.await();
	}
	
	public void ejemploDelayElements() {
		
		Flux<Integer> rango = Flux.range(1, 12)
								.delayElements(Duration.ofSeconds(1))
								.doOnNext(i -> logger.info(i.toString()));
		rango.subscribe();
		// rango.blockLast(); // bloqueante
	}
	
	public void ejemploInterval() {
		
		Flux<Integer> rango = Flux.range(1, 12);
		Flux<Long> retraso = Flux.interval(Duration.ofSeconds(1));
		
		rango.zipWith(retraso, (ra, re) -> ra)
			.doOnNext(i -> logger.info(i.toString()))
			.blockLast();
	}
	
	public void ejemploZipWithRangos() {
		
		Flux<Integer> rangos = Flux.range(0, 4);
		Flux.just(1, 2, 3, 4)
			.map(i -> (i*2))
			.zipWith(rangos, (uno, dos) -> String.format("Primer Flux: %d, Segundo Flux: %d", uno, dos))
			.subscribe(texto -> logger.info(texto));
	}
	
	public void ejemploUsuarioComentariosZipWithForma2() {
		Mono<Usuario> usuarioMono = Mono.fromCallable(() -> new Usuario("John", "Doe"));

		Mono<Comentarios> comentariosUsuarioMono = Mono.fromCallable(() -> {
			Comentarios comentarios = new Comentarios();
			comentarios.addComentario("Hola amigos reactivos que tal !");
			comentarios.addComentario("xddd");
			comentarios.addComentario("Estoy en mi casa");
			return comentarios;
		});
		
		Mono<UsuarioComentarios> usuarioConComentarios = usuarioMono
					.zipWith(comentariosUsuarioMono)
					.map(tuple -> {
						Usuario u = tuple.getT1();
						Comentarios c = tuple.getT2();
						return new UsuarioComentarios(u, c);
					});
		
		usuarioConComentarios.subscribe((uC) -> logger.info(uC.toString()));
	}
	
	public void ejemploUsuarioComentariosZipWith() {
		Mono<Usuario> usuarioMono = Mono.fromCallable(() -> new Usuario("John", "Doe"));

		Mono<Comentarios> comentariosUsuarioMono = Mono.fromCallable(() -> {
			Comentarios comentarios = new Comentarios();
			comentarios.addComentario("Hola amigos reactivos que tal !");
			comentarios.addComentario("xddd");
			comentarios.addComentario("Estoy en mi casa");
			return comentarios;
		});
		
		Mono<UsuarioComentarios> usuarioConComentarios = usuarioMono
					.zipWith(comentariosUsuarioMono, (usuario, comentariosUsuario) -> {
								return new UsuarioComentarios(usuario, comentariosUsuario);
							});
		usuarioConComentarios.subscribe((uC) -> logger.info(uC.toString()));
	}
	
	public void ejemploUsuarioComentariosFlatMap() {
		Mono<Usuario> usuarioMono = Mono.fromCallable(() -> new Usuario("John", "Doe"));

		Mono<Comentarios> comentariosUsuarioMono = Mono.fromCallable(() -> {
			Comentarios comentarios = new Comentarios();
			comentarios.addComentario("Hola amigos reactivos que tal !");
			comentarios.addComentario("xddd");
			comentarios.addComentario("Estoy en mi casa");
			return comentarios;
		});
		
		Mono<UsuarioComentarios> usuarioConComentarios = usuarioMono
				.flatMap((usuario) -> comentariosUsuarioMono.map((comentarios) -> {
							return new UsuarioComentarios(usuario, comentarios);
						}));
		usuarioConComentarios.subscribe((uC) -> logger.info(uC.toString()));
	}
	
	public static void ejemploCollectList() throws Exception {
		
		List<Usuario> usuariosLista = new ArrayList<Usuario>();
		usuariosLista.add( new Usuario("Andres", "Guzman"));
		usuariosLista.add(new Usuario("Pedro", "Salas"));
		usuariosLista.add(new Usuario("Alexis", "Gutierrez"));
		usuariosLista.add(new Usuario("Diego", "Martinez"));
		usuariosLista.add(new Usuario("Juan", "Fuentes"));
		usuariosLista.add(new Usuario("Bruce", "Lee"));
		usuariosLista.add(new Usuario("Bruce", "Willis"));
		
		// Flux es un publisher un observable
		Flux.fromIterable(usuariosLista)
					// convierte a un mono que contiene un solo objeto
					.collectList()
					.subscribe((lista) -> {
						lista.forEach((item) -> logger.info(item.toString()));
					});
	}
	
	public void ejemploToString() throws Exception {
		
		List<Usuario> usuariosLista = new ArrayList<Usuario>();
		usuariosLista.add( new Usuario("Andres", "Guzman"));
		usuariosLista.add(new Usuario("Pedro", "Salas"));
		usuariosLista.add(new Usuario("Alexis", "Gutierrez"));
		usuariosLista.add(new Usuario("Diego", "Martinez"));
		usuariosLista.add(new Usuario("Juan", "Fuentes"));
		usuariosLista.add(new Usuario("Bruce", "Lee"));
		usuariosLista.add(new Usuario("Bruce", "Willis"));
		
		// Flux es un publisher un observable
		Flux.fromIterable(usuariosLista)
								.map((usuario) -> {
										return usuario.getNombre().toUpperCase().concat(" ".concat(usuario.getApellido().toUpperCase()));
								})
								.flatMap((nombre) -> {
									if (nombre.contains("bruce".toUpperCase())) {
										return Mono.just(nombre);
									} else {
										return Mono.empty();
									}
								})
								// utilizando referencia de metodo
								//.doOnNext(System.out::println);
								.map((nombre) -> {
									return nombre.toLowerCase();
								}).subscribe(
										// utilizando expresion lambda
										(e) -> logger.info(e.toString())
										);
	}
	
	public void ejemploFlatMap() throws Exception {
		
		List<String> usuariosLista = new ArrayList<String>();
		usuariosLista.add("Andres Guzman");
		usuariosLista.add("Pedro Salas");
		usuariosLista.add("Alexis Gutierrez");
		usuariosLista.add("Diego Martinez");
		usuariosLista.add("Juan Fuentes");
		usuariosLista.add("Bruce Lee");
		usuariosLista.add("Bruce Willis");
		
		// Flux es un publisher un observable
		Flux.fromIterable(usuariosLista).map((nombre) -> {
									String[] nombreCompleto = nombre.split(" ");
									return new Usuario(nombreCompleto[0].toUpperCase(), nombreCompleto[1].toUpperCase());
								})
								.flatMap((usuario) -> {
									if (usuario.getNombre().equalsIgnoreCase("bruce")) {
										return Mono.just(usuario);
									} else {
										return Mono.empty();
									}
								})
								// utilizando referencia de metodo
								//.doOnNext(System.out::println);
								.map((usuario) -> {
									String nombre = usuario.getNombre().toLowerCase();
									usuario.setNombre(nombre);
									return usuario;
								}).subscribe(
										// utilizando expresion lambda
										(e) -> logger.info(e.toString())
										);
	}
	
	public void ejemploIterable() throws Exception {
		
		List<String> usuariosLista = new ArrayList<String>();
		usuariosLista.add("Andres Guzman");
		usuariosLista.add("Pedro Salas");
		usuariosLista.add("Alexis Gutierrez");
		usuariosLista.add("Diego Martinez");
		usuariosLista.add("Juan Fuentes");
		usuariosLista.add("Bruce Lee");
		usuariosLista.add("Bruce Willis");
		
		// Flux es un publisher un observable
		Flux<String> nombres = Flux.fromIterable(usuariosLista);
				
//				Flux.just("Andres Guzman", "Pedro Salas", "Alexis Gutierrez", 
//										"Diego Martinez", "Juan Fuentes", "Bruce Lee", "Bruce Willis");
		
							Flux<Usuario> usuarios =  nombres.map((nombre) -> {
									String[] nombreCompleto = nombre.split(" ");
									return new Usuario(nombreCompleto[0].toUpperCase(), nombreCompleto[1].toUpperCase());
								})
								.filter((usuario) -> {
									return usuario.getNombre().toLowerCase().equals("bruce");
								})
								.doOnNext(usuario -> {
									if (usuario == null) {
										throw new RuntimeException("Nombres no pueden ser vacios");
									} 
									System.out.println(usuario.getNombre().concat(" ").concat(usuario.getApellido()));
									//System.out::println;
								})
								// utilizando referencia de metodo
								//.doOnNext(System.out::println);
								.map((usuario) -> {
									String nombre = usuario.getNombre().toLowerCase();
									usuario.setNombre(nombre);
									return usuario;
								});
		// utilizando expresion lambda
		usuarios.subscribe(
				(e) -> logger.info(e.toString()),
				(error) -> {
					logger.error(error.getMessage());
				}, new Runnable() {
					@Override
					public void run() {
						logger.info("Ha finalizado la ejecucion del observable con exito !");
					}
				});
	}
}
