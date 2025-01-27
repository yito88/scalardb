package com.scalar.db.server.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Properties;
import org.junit.Test;

public class ServerConfigTest {

  private static final int ANY_PORT = 9999;

  @Test
  public void constructor_NothingGiven_ShouldUseDefault() {
    // Arrange
    Properties props = new Properties();

    // Act
    ServerConfig config = new ServerConfig(props);

    // Assert
    assertThat(config.getPort()).isEqualTo(ServerConfig.DEFAULT_PORT);
    assertThat(config.getPrometheusHttpEndpointPort())
        .isEqualTo(ServerConfig.DEFAULT_PROMETHEUS_HTTP_ENDPOINT_PORT);
  }

  @Test
  public void constructor_ValidPortGiven_ShouldLoadProperly() {
    // Arrange
    Properties props = new Properties();
    props.setProperty(ServerConfig.PORT, Integer.toString(ANY_PORT));

    // Act
    ServerConfig config = new ServerConfig(props);

    // Assert
    assertThat(config.getPort()).isEqualTo(ANY_PORT);
  }

  @Test
  public void constructor_InvalidPortGiven_ShouldUseDefault() {
    // Arrange
    Properties props = new Properties();
    props.setProperty(ServerConfig.PORT, "abc");

    // Act
    ServerConfig config = new ServerConfig(props);

    // Assert
    assertThat(config.getPort()).isEqualTo(ServerConfig.DEFAULT_PORT);
  }

  @Test
  public void constructor_ValidPrometheusHttpEndpointPortGiven_ShouldLoadProperly() {
    // Arrange
    Properties props = new Properties();
    props.setProperty(ServerConfig.PROMETHEUS_HTTP_ENDPOINT_PORT, Integer.toString(ANY_PORT));

    // Act
    ServerConfig config = new ServerConfig(props);

    // Assert
    assertThat(config.getPrometheusHttpEndpointPort()).isEqualTo(ANY_PORT);
  }

  @Test
  public void constructor_InvalidPrometheusHttpEndpointPortGiven_ShouldUseDefault() {
    // Arrange
    Properties props = new Properties();
    props.setProperty(ServerConfig.PROMETHEUS_HTTP_ENDPOINT_PORT, "abc");

    // Act
    ServerConfig config = new ServerConfig(props);

    // Assert
    assertThat(config.getPrometheusHttpEndpointPort())
        .isEqualTo(ServerConfig.DEFAULT_PROMETHEUS_HTTP_ENDPOINT_PORT);
  }
}
