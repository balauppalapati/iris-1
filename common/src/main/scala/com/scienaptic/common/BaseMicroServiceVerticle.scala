package com.scienaptic.common

import java.net.ConnectException

import com.scienaptic.common.config.Config.{ Consul, CircuitBreaker => CBOpts }
import com.scienaptic.common.util.JsonObjectImplicit.JsonObjectHelper
import com.scienaptic.lib.common.annotations.impure
import io.vertx.core.impl.NoStackTraceThrowable
import io.vertx.core.json.JsonObject
import io.vertx.lang.scala.ScalaVerticle
import io.vertx.scala.circuitbreaker.{ CircuitBreaker, CircuitBreakerOptions }
import io.vertx.scala.servicediscovery.types.{ EventBusService, HttpEndpoint, MessageSource }
import io.vertx.scala.servicediscovery.{ Record, ServiceDiscovery, ServiceDiscoveryOptions }
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.Future
import scala.util.control.NonFatal

class BaseMicroServiceVerticle extends ScalaVerticle with Logging {

  // scalastyle:off
  // https://github.com/vert-x3/vertx-lang-scala/blob/master/vertx-lang-scala-stack/vertx-service-discovery/vertx-service-discovery/src/main/asciidoc/scala/index.adoc#publishing-services
  // scalastyle:on
  @SuppressWarnings(Array("org.wartremover.warts.Var", "org.wartremover.warts.Null"))
  protected var discovery: ServiceDiscovery = _

  @SuppressWarnings(Array("org.wartremover.warts.Var"))
  protected var registeredRecords: List[Record] = List.empty[Record]

  // http://vertx.io/docs/vertx-circuit-breaker/scala/
  @SuppressWarnings(Array("org.wartremover.warts.Var", "org.wartremover.warts.Null"))
  protected var circuitBreaker: CircuitBreaker = _

  /**
    * Start Discovery Service & Circuit Breaker Instances
    */
  @impure override def start(): Unit = {

    // Discovery Service Options using Consul
    val discoveryOptions = new JsonObject()
      .put("host", config.getString(Consul.Host))
      .put("port", config.getInteger(Consul.Port))
      .put("dc", config.getString(Consul.DataCenter))

    // Circuit Breaker Name & Options
    val circuitBreakerName = config.getString(CBOpts.Name)
    val circuitBreakerOptions = CircuitBreakerOptions()
    // After these many failures, the circuit trips to the open-state.
      .setMaxFailures(config.getInteger(CBOpts.MaxFailures))
      // If the operation has not finished after these many seconds, it counts as a failure.
      .setTimeout(config.getLong(CBOpts.Timeout))
      // Wait for these many seconds in the open-state before a retry.
      .setResetTimeout(config.getLong(CBOpts.ResetTimeout))

    /* Create ServiceDiscovery instance to use the discovery infrastructure. */
    discovery = ServiceDiscovery.create(vertx, ServiceDiscoveryOptions().setBackendConfiguration(discoveryOptions))

    /* Create CircuitBreaker instance to implement circuit breaker pattern */
    circuitBreaker = CircuitBreaker
      .create(circuitBreakerName, vertx, circuitBreakerOptions)
      .closeHandler(_ => logger.info(s"$circuitBreakerName: circuit breaker closed"))
      .halfOpenHandler(_ => logger.error(s"$circuitBreakerName: circuit breaker half-opened"))
      .openHandler(_ => logger.error(s"$circuitBreakerName: circuit breaker opened"))
  }

  /**
    * Un-publish all registered records to discovery service and close the discovery service.
    */
  @impure override def stop(): Unit = {

    val futures = registeredRecords.map(record => discovery.unpublishFuture(record.getRegistration))

    // don’t need the service discovery object anymore. close it.
    if (futures.isEmpty) {
      discovery.close
    } else {
      Future
        .sequence(futures.toList)
        .map(_ => discovery.close)
        .recover {
          case NonFatal(e) => logger.error(s"Cannot close discovery service. ${e.getMessage}", e)
        }
    }
  }

  /**
    * Publish HttpEndpoint records to Service Discovery.
    *
    * @param name         the service name
    * @param host         the host (IP or DNS name), it must be the _public_ IP / name
    * @param port         the port, it must be the _public_ port
    * @param endpointType http client end point type
    */
  protected final def publishHttpEndpointFuture(name: String,
                                                host: String,
                                                port: Int,
                                                endpointType: String): Future[Record] =
    publishFuture(HttpEndpoint.createRecord(name, host, port, "/").setType(endpointType))

  /**
    * Publish MessageSource records to Service Discovery.
    *
    * @param name    the name of the service
    * @param address the address on which the data is sent
    */
  protected final def publishMessageSourceFuture(name: String, address: String): Future[Record] =
    publishFuture(MessageSource.createRecord(name, address))

  /**
    * Publish EventBusService records to Service Discovery.
    *
    * @param name     the name of the service.
    * @param address  the event bus address on which the service available
    * @param clazz    Event bus service class
    * @param metadata the metadata
    */
  protected final def publishEventBusServiceFuture(name: String,
                                                   address: String,
                                                   clazz: String,
                                                   metadata: JsonObject): Future[Record] =
    publishFuture(EventBusService.createRecord(name, address, clazz, metadata))

  /**
    * Publish records to Service Discovery and add the published records to [[registeredRecords]] collection.
    *
    * @param record information shared between consumer and provider
    */
  @SuppressWarnings(Array("org.wartremover.warts.Throw"))
  @impure private def publishFuture(record: Record): Future[Record] =
    /* Publish a record for a specific service provider and add it to registeredRecords on success. */
    discovery
      .publishFuture(record)
      .map { result =>
        logger.info(s"${record.getName}: ${record.getType} service successfully published")
        registeredRecords = registeredRecords :+ record
        result
      }
      .recover {
        /*
         * NoStackTraceThrowable occurs when there is no valid response from discovery service.
         * ConnectException occurs when discovery service isn't running.
         */
        case e @ (_: NoStackTraceThrowable | _: ConnectException) =>
          val errorMsg = s"${record.getName}: Unable to publish the service. ${e.getMessage}"
          logger.error(errorMsg, e)
          throw new IllegalStateException(errorMsg)
        case NonFatal(e) => {
          logger.error(e)
          throw new RuntimeException(e)
        }
      }

}
