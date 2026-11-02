package com.maintena.architecture;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import jakarta.persistence.Entity;
import org.mapstruct.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Set;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static java.util.Map.entry;

/**
 * Reglas de arquitectura verificadas en cada build.
 *
 * <p>La estructura por funcionalidad solo se mantiene si el compilador (y este
 * test) impiden las dependencias que la rompen. El mapa de dependencias entre
 * módulos se declara explícitamente en {@link #DEPENDENCIAS_PERMITIDAS}.</p>
 */
@AnalyzeClasses(packages = "com.maintena", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

	private static final String PAQUETE_BASE = "com.maintena.";

	/** Módulos transversales que cualquier módulo puede usar. */
	private static final Set<String> MODULOS_COMPARTIDOS = Set.of("common", "config", "exception", "security");

	/**
	 * Dependencias permitidas entre módulos de negocio. Si aparece una nueva,
	 * debe declararse aquí de forma consciente.
	 */
	private static final Map<String, Set<String>> DEPENDENCIAS_PERMITIDAS = Map.ofEntries(
		entry("asset", Set.of("category", "location", "audit")),
		entry("audit", Set.of("user")),
		entry("auth", Set.of("user", "audit", "notification", "role")),
		entry("category", Set.of("audit")),
		entry("dashboard", Set.of("maintenance", "asset", "user")),
		entry("health", Set.of()),
		entry("location", Set.of("audit")),
		entry("maintenance", Set.of("asset", "user", "audit", "notification")),
		entry("notification", Set.of("user")),
		entry("report", Set.of("maintenance", "audit", "asset", "user")),
		entry("role", Set.of("audit")),
		entry("user", Set.of("role", "audit"))
	);

	@ArchTest
	static final ArchRule controladores_no_acceden_a_repositorios = noClasses()
		.that().resideInAPackage("..controller..")
		.should().dependOnClassesThat().resideInAPackage("..repository..");

	@ArchTest
	static final ArchRule controladores_no_exponen_entidades_jpa = noClasses()
		.that().resideInAPackage("..controller..")
		.should().dependOnClassesThat().areAnnotatedWith(Entity.class);

	@ArchTest
	static final ArchRule entidades_no_dependen_de_capas_superiores = noClasses()
		.that().resideInAPackage("..entity..")
		.should().dependOnClassesThat().resideInAnyPackage(
			"..controller..", "..service..", "..repository..", "..mapper..", "..dto..");

	@ArchTest
	static final ArchRule repositorios_no_dependen_de_capas_superiores = noClasses()
		.that().resideInAPackage("..repository..")
		.should().dependOnClassesThat().resideInAnyPackage("..controller..", "..service..", "..mapper..");

	@ArchTest
	static final ArchRule servicios_no_dependen_de_controladores = noClasses()
		.that().resideInAPackage("..service..")
		.should().dependOnClassesThat().resideInAPackage("..controller..");

	@ArchTest
	static final ArchRule mappers_no_dependen_de_persistencia = noClasses()
		.that().resideInAPackage("..mapper..")
		.should().dependOnClassesThat().resideInAnyPackage("..repository..", "..service..", "..controller..");

	@ArchTest
	static final ArchRule controladores_son_rest_controllers = classes()
		.that().resideInAPackage("..controller..")
		.should().beAnnotatedWith(RestController.class)
		.andShould().haveSimpleNameEndingWith("Controller");

	@ArchTest
	static final ArchRule repositorios_son_interfaces = classes()
		.that().resideInAPackage("..repository..")
		.should().beInterfaces();

	@ArchTest
	static final ArchRule entidades_viven_en_su_paquete = classes()
		.that().areAnnotatedWith(Entity.class)
		.should().resideInAPackage("..entity..");

	@ArchTest
	static final ArchRule mappers_viven_en_su_paquete = classes()
		.that().areAnnotatedWith(Mapper.class)
		.should().resideInAPackage("..mapper..");

	@ArchTest
	static final ArchRule servicios_impl_estan_anotados = classes()
		.that().haveSimpleNameEndingWith("ServiceImpl")
		.should().beAnnotatedWith(Service.class);

	@ArchTest
	static final ArchRule sin_inyeccion_por_campo = noFields()
		.should().beAnnotatedWith(Autowired.class);

	@ArchTest
	static final ArchRule modulos_respetan_el_mapa_de_dependencias = classes()
		.that().resideInAPackage("com.maintena..")
		.should(new ArchCondition<>("respetar el mapa de dependencias entre módulos") {
			@Override
			public void check(JavaClass origen, ConditionEvents events) {
				String moduloOrigen = moduloDe(origen.getPackageName());
				if (moduloOrigen == null || MODULOS_COMPARTIDOS.contains(moduloOrigen)) {
					return;
				}
				for (Dependency dependencia : origen.getDirectDependenciesFromSelf()) {
					String moduloDestino = moduloDe(dependencia.getTargetClass().getPackageName());
					if (moduloDestino == null || moduloDestino.equals(moduloOrigen)
						|| MODULOS_COMPARTIDOS.contains(moduloDestino)) {
						continue;
					}
					Set<String> permitidos = DEPENDENCIAS_PERMITIDAS.getOrDefault(moduloOrigen, Set.of());
					if (!permitidos.contains(moduloDestino)) {
						String mensaje = origen.getName() + " (" + moduloOrigen + ") depende de "
							+ dependencia.getTargetClass().getName() + " (" + moduloDestino + ")";
						events.add(SimpleConditionEvent.violated(origen, mensaje));
					}
				}
			}
		});

	private static String moduloDe(String nombrePaquete) {
		if (!nombrePaquete.startsWith(PAQUETE_BASE)) {
			return null;
		}
		String resto = nombrePaquete.substring(PAQUETE_BASE.length());
		int punto = resto.indexOf('.');
		return punto < 0 ? resto : resto.substring(0, punto);
	}
}
