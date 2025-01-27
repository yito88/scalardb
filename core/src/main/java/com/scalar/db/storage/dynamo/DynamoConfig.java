package com.scalar.db.storage.dynamo;

import com.google.common.base.Strings;
import com.scalar.db.config.DatabaseConfig;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.Properties;
import javax.annotation.concurrent.Immutable;

@Immutable
public class DynamoConfig extends DatabaseConfig {

  public static final String PREFIX = DatabaseConfig.PREFIX + "dynamo.";
  public static final String ENDPOINT_OVERRIDE = PREFIX + "endpoint-override";

  private Optional<String> endpointOverride;

  public DynamoConfig(File propertiesFile) throws IOException {
    super(propertiesFile);
  }

  public DynamoConfig(InputStream stream) throws IOException {
    super(stream);
  }

  public DynamoConfig(Properties properties) {
    super(properties);
  }

  @Override
  protected void load() {
    String storage = getProperties().getProperty(DatabaseConfig.STORAGE);
    if (storage == null || !storage.equals("dynamo")) {
      throw new IllegalArgumentException(DatabaseConfig.STORAGE + " should be 'dynamo'");
    }

    super.load();

    if (!Strings.isNullOrEmpty(getProperties().getProperty(ENDPOINT_OVERRIDE))) {
      endpointOverride = Optional.of(getProperties().getProperty(ENDPOINT_OVERRIDE));
    } else {
      endpointOverride = Optional.empty();
    }
  }

  public Optional<String> getEndpointOverride() {
    return endpointOverride;
  }
}
