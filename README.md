# Sistema de Solicitudes de Reembolso de Gastos Corporativos

# Parte 1: Análisis de Requisitos, Diseño Propuesto e Implementación

## 1. Diseño Propuesto (Previo a la Implementación)

| Requisito Funcional | Punto de Variabilidad | Patrón de Diseño Propuesto | Justificación (Test de Explicabilidad en 1 oración) |
| :--- | :--- | :--- | :--- |
| **RF1 — Validación en varias etapas** | La secuencia y cantidad de verificaciones independientes (tope de categoría, documentos soporte, duplicados y futuras reglas) que debe superar una solicitud. | **Chain of Responsibility** | Se elige Chain of Responsibility porque permite encadenar verificaciones desacopladas donde cada eslabón decide si rechaza la solicitud o delega al siguiente, permitiendo agregar nuevas reglas de validación en el futuro sin modificar las existentes. |
| **RF2 — Cálculo distinto por categoría de gasto** | La fórmula matemática de cálculo del monto aprobado según la categoría del gasto (`VIAJE`, `REPRESENTACION`, `CAPACITACION` y futuras categorías). | **Strategy** | Se elige Strategy porque encapsula cada algoritmo de cálculo por categoría en una clase independiente bajo una interfaz común, permitiendo agregar nuevas categorías de gasto sin alterar las fórmulas existentes ni usar bloques condicionales rígidos. |
| **RF3 — Notificación simultánea a varios interesados** | El conjunto de destinatarios (Empleado, Contabilidad, Auditoría y futuros paneles) y la reacción particular de cada uno cuando una solicitud cambia de estado. | **Observer** | Se elige Observer porque permite suscribir múltiples interesados a las transiciones de estado de una solicitud para que reaccionen independientemente sin acoplar el servicio de aplicación a los detalles de cada notificador. |

## COMPILACION DEL PROYECTO
![Captura de consola de la compilacion del proyecto sin que el dominio importe clases](<./evidencias/parte-1/Compillacion del proyecto sin que el dominio importe.png>)

---

## 2. Decisiones de Diseño de la Parte 1

### Decisión de Diseño 1: Orden de encadenamiento en `CadenaValidadores`

#### Orden elegido
$$\text{ValidadorDocumentosSoporte} \longrightarrow \text{ValidadorMontoMaximo} \longrightarrow \text{ValidadorDuplicados}$$

#### Justificación con criterio de eficiencia (*Fail-Fast*)
La cadena de validación se ordenó evaluando el costo computacional de menor a mayor complejidad algorítmica:

1. **`ValidadorDocumentosSoporte` ($\mathcal{O}(1)$ directo):** Evalúa un valor booleano en memoria (`solicitud.isDocumentosSoporte()`). Es una operación casi instantánea a nivel de CPU.
2. **`ValidadorMontoMaximo` ($\mathcal{O}(1)$ con overhead mínimo):** Realiza una búsqueda por clave en un mapa estático en memoria (`Map.get()`) y compara dos objetos `BigDecimal`.
3. **`ValidadorDuplicados` ($\mathcal{O}(n)$, donde $n$ es la cantidad de solicitudes previas):** Es el eslabón más costoso computacionalmente, pues debe recorrer mediante un `Stream` la lista de solicitudes pendientes en memoria cargada desde la base de datos y evaluar comparaciones de fechas y categorías para cada elemento.

#### Trade-offs: ¿Qué se gana y qué se pierde?
* **Qué se gana:** Se maximiza el rendimiento y la eficiencia del servidor bajo el principio *Fail-Fast*. Cualquier solicitud inválida por falta de soportes o por exceder el tope de categoría es rechazada en tiempo constante $\mathcal{O}(1)$, cortocircuitando el flujo de inmediato y evitando por completo la iteración $\mathcal{O}(n)$ sobre colecciones de la base de datos.
* **Qué se pierde:** Si una solicitud incumple múltiples condiciones en simultáneo (por ejemplo, no tiene documentos y además es un duplicado), el usuario solo conocerá primero el error de menor costo computacional (falta de documentos). No obstante, este comportamiento es preferible frente al desperdicio de CPU que implicaría evaluar listas contra registros duplicados para solicitudes que ni siquiera tienen soporte físico adjunto.

---

### Decisión de Diseño 2: Mecanismo de selección de calculadoras (`Map` inyectado vs. `switch` manual)

#### Mecanismo elegido
Construcción de un `Map<CategoriaGasto, CalculadoraReembolso>` dentro de `SelectorCalculadora`, auto-ensamblado mediante la inyección por constructor de la lista de todos los `@Component` que implementan `CalculadoraReembolso` descubiertos por el contenedor de Spring.

#### Justificación considerando RF2 (Principio Abierto/Cerrado — OCP)
El requisito funcional **RF2** declara explícitamente que el área de Finanzas agregará nuevas categorías en los próximos semestres (por ejemplo, "bienestar"), cada una con su fórmula propia y **sin modificar el código existente**:

* **Cumplimiento de OCP:** Con el mapa inyectado automáticamente, para incorporar la categoría `BIENESTAR` solo se requiere crear la nueva clase `CalculadoraBienestar implements CalculadoraReembolso` anotada con `@Component`. Spring la inyecta automáticamente en el mapa en tiempo de arranque; no es necesario modificar ni una sola línea de código en `SelectorCalculadora` ni en `SolicitudReembolsoService`.
* **Descarte del `switch` manual:** Un método basado en `switch (categoria)` violaría de forma directa el Principio Abierto/Cerrado, ya que cada nueva categoría forzaría a abrir y editar la clase selectora, reintroduciendo riesgos de regresión en código previamente probado y validado.

---

## 3. Checkpoints de Verificación de la Parte 1

### 3.1. Pruebas Automatizadas
El proyecto cuenta con una suite que cubre pruebas unitarias para cada eslabón y calculadora, además de una prueba de integración sobre el contexto de Spring:
* **Validadores:** `validadorDocumentosSoporteRechazaCuandoNoHaySoporte`, `validadorMontoMaximoRechazaCuandoSuperaTopeDeCategoria`, `validadorDuplicadosRechazaCuandoExistePendienteEnMismoMesYCategoria`.
* **Calculadoras:** `calculadoraViajeAplicaTopeDiarioMultiplicadoPorDias`, `calculadoraRepresentacionReembolsaMontoCompletoHastaTopeMensual`, `calculadoraCapacitacionReembolsaOchentaPorCientoHastaTopeAnual`.
* **Integración:** `flujoCompletoCrearEnTresCategoriasValidarCalcularYCambiarEstado` (@SpringBootTest).

Para ejecutar la verificación:
```bash
mvn clean test
```
## Pruebas en Verde

![Captura de consola de la correcta ejecucion de las pruebas](<./evidencias/parte-1/Ejecucion de pruebas.png>)


### 3.2. Verificación de Independencia del Validador de Duplicados
Se verifica que `ValidadorDuplicados` recibe la información pre-cargada por el servicio y no importa clases de infraestructura ni de acceso a datos:

* En sistemas UNIX (Linux/macOS):
  ```bash
  grep "import" src/main/java/com/empresa/reembolsos/procesamiento/validacion/ValidadorDuplicados.java
  ```
* En Windows CMD:
  ```cmd
  findstr "import" src\main\java\com\empresa\reembolsos\procesamiento\validacion\ValidadorDuplicados.java
  ```

## Resultado de los imports

![Captura de consola de los imports](<./evidencias/parte-1/Resultados de los Imports del dominio y Spring.png>)

# Parte 2: Decisión Arquitectónica y Análisis de Impacto

## 4. Desicion de diseño: Alcance de la Regla 1 de ArchUnit sobre org.springframework

#### Decisión
La regla `procesamientoSinPersistenciaNiWeb` prohíbe que `..procesamiento..` dependa de `org.springframework.data..` y `jakarta.persistence..`, pero permite el uso de anotaciones de estereotipo como `@Component` (`org.springframework.stereotype.Component`).

#### Justificación
 Existe una diferencia fundamental entre metadatos declarativos de cableado (`@Component`) y dependencias tecnológicas acoplantes (`Spring Data`, `JPA`, `Spring Web`). `@Component` es una anotación pasiva que permite a Spring descubrir los eslabones, calculadoras y observadores automáticamente sin impedir que dichas clases se instancien con `new` en pruebas unitarias puras en milisegundos. Prohibir todo `org.springframework..` obligaría a escribir métodos `@Bean` manuales en clases `@Configuration` para cada estrategia y validador sin aportar aislamiento real frente a la base de datos o la capa web.

 ## 5. Desicion de diseño: Qué se Descartó y Por Qué (Análisis de Impacto y Sobreingeniería)

 Durante el desarrollo se evaluaron y descartaron conscientemente las siguientes alternativas por constituir sobreingeniería frente a los requisitos actuales:

 ### 5.1. `Spring ApplicationEventPublisher` en lugar de `List<ObservadorCambioEstado>`

 #### Se considera
 Publicar eventos de dominio mediante `ApplicationEventPublisher` y suscribir métodos con `@EventListener` en lugar de inyectar `List<ObservadorCambioEstado>` directamente en `SolicitudReembolsoService`.
 #### Se descarta
 Con tres observadores conocidos en tiempo de compilación dentro del mismo módulo, un bus de eventos genérico agrega indirección innecesaria y dificulta seguir el flujo de llamadas en el IDE, mientras que la interfaz `ObservadorCambioEstado` ya cumple el desacoplamiento exigido por RF3.
 #### Futura señal que haria reconsiderar la desicion
 Si nuevos módulos externos al contexto de reembolsos necesitan reaccionar de forma asíncrona o atada a la confirmación de la transacción (`@TransactionalEventListener(phase = AFTER_COMMIT)`) sin acoplarse al paquete de procesamiento.

### 5.2. `Visitor` o `Abstract Factory` para exportación de reportes en varios formatos
 #### Se considera
 Crear anticipadamente una jerarquía de exportadores para formatos PDF, Excel y CSV.
 #### Se descarta
 Actualmente solo se requiere un formato; al no existir una segunda ni tercera variante real solicitada, no se cumple la Rule of Three y crear la abstracción violaría YAGNI.
 #### Futura señal que haria reconsiderar la desicion
 Cuando Finanzas o Contabilidad soliciten formalmente al menos dos formatos adicionales de exportación con representaciones estructurales distintas.

### 5.3. Jerarquía de Excepciones Específicas por cada Validador
 #### Se considera
Reemplazar el record `ResultadoValidacion(boolean valida, String motivoRechazo)` por una jerarquía de excepciones (`FaltaSoporteException`, `TopeCategoriaExcedidoException`, `SolicitudDuplicadaException` extendiendo de `SolicitudInvalidaException`) lanzadas directamente por cada eslabón de `ValidadorSolicitud`.
 #### Se descarta
Aplicando **YAGNI** y **KISS**, tanto `SolicitudReembolsoService` como `SolicitudReembolsoController` procesan cualquier rechazo de validación del mismo modo (abortar la creación y retornar HTTP 400 con el motivo textual). Crear una subclase de excepción por cada regla multiplicaría el número de clases y usaría excepciones como control de flujo ordinario sin que ningún consumidor las capture por separado. 
#### Futura señal que haria reconsiderar la desicion
 Cuando un sistema cliente o el frontend necesite ejecutar flujos de compensación automatizados distintos o recibir códigos HTTP diferenciados (por ejemplo, `409 Conflict` para duplicados vs. `422 Unprocessable Entity` para topes) según el validador específico que rechazó la solicitud.

### 5.4. Patrón `Builder` o `Factory Method` para construir SolicitudReembolso
 #### Se considera
Crear un `SolicitudReembolsoBuilder` o fábricas por categoría (`SolicitudViajeFactory`, `SolicitudCapacitacionFactory`) dado que el campo `diasViaje` solo es relevante cuando la categoría es `VIAJE`.
 #### Se descarta
Aplicando el test de explicabilidad y la ****Rule of Three**, `SolicitudReembolso` posee una estructura simple que Jackson deserializa directamente desde el cuerpo JSON de la petición REST; introducir fábricas o builders hoy añadiría clases intermedias sin resolver una complejidad real de inicialización.
#### Futura señal que haria reconsiderar la desicion
 Cuando al menos tres categorías de gasto incorporen conjuntos de atributos exclusivos complejos o reglas de inicialización mutuamente excluyentes que hagan ambiguo un único constructor o payload de entrada.