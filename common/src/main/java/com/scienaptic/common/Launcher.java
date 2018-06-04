package com.scienaptic.common;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Log4j2LogDelegateFactory;
import io.vertx.core.logging.LoggerFactory;

// http://vertx.io/docs/vertx-core/java/#_sub_classing_the_launcher
public class Launcher extends io.vertx.core.Launcher {

  public static void main(String[] args) {
    // To use log4j2 as logging system.
    System.setProperty(LoggerFactory.LOGGER_DELEGATE_FACTORY_CLASS_NAME, Log4j2LogDelegateFactory.class.getName());
    new Launcher().dispatch(args);
  }

  @Override
  public void beforeStartingVertx(VertxOptions options) {

    super.beforeStartingVertx(options);

    //    // Dropwizard Metrics
    //    options.setBlockedThreadCheckInterval(1500000);
    //    options.setWarningExceptionTime(1500000);
    //    options.setMetricsOptions(new DropwizardMetricsOptions().setEnabled(true).addMonitoredHttpServerUri(
    //      new Match().setValue("/.*").setType(MatchType.REGEX)));
    //
    //    // Clustering
    //    options.setClustered(true).setClusterHost("127.0.0.1");

  }

  @Override
  public void beforeDeployingVerticle(DeploymentOptions deploymentOptions) {
    super.beforeDeployingVerticle(deploymentOptions);
    if (deploymentOptions.getConfig() == null) {
      deploymentOptions.setConfig(new JsonObject());
    }
  }

}
