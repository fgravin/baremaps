/*
 * Copyright (C) 2011 The Baremaps Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.baremaps.cli.command;

import static com.baremaps.cli.option.TileReaderOption.fast;

import com.baremaps.cli.option.TileReaderOption;
import com.baremaps.tiles.TileStore;
import com.baremaps.tiles.config.Config;
import com.baremaps.tiles.database.FastPostgisTileStore;
import com.baremaps.tiles.database.SlowPostgisTileStore;
import com.baremaps.tiles.store.FileSystemTileStore;
import com.baremaps.tiles.util.TileUtil;
import com.baremaps.util.fs.FileSystem;
import com.baremaps.util.postgis.PostgisHelper;
import com.baremaps.util.tile.Tile;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.Callable;
import java.util.stream.Stream;
import javax.sql.DataSource;
import org.apache.commons.dbcp2.PoolingDataSource;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.io.ParseException;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = "export", description = "Export vector tiles from the Postgresql database.")
public class Export implements Callable<Integer> {

  private static Logger logger = LogManager.getLogger();

  @Mixin
  private Mixins mixins;

  @Option(
      names = {"--database"},
      paramLabel = "JDBC",
      description = "The JDBC url of the Postgres database.",
      required = true)
  private String database;

  @Option(
      names = {"--config"},
      paramLabel = "YAML",
      description = "The YAML configuration file.",
      required = true)
  private URI config;

  @Option(
      names = {"--repository"},
      paramLabel = "URL",
      description = "The tile repository URL.",
      required = true)
  private URI repository;

  @Option(
      names = {"--minZoom"},
      paramLabel = "MIN",
      description = "The minimal zoom level.")
  private int minZoom = 0;

  @Option(
      names = {"--maxZoom"},
      paramLabel = "MAX",
      description = "The maximal zoom level.")
  private int maxZoom = 14;

  @Option(
      names = {"--delta"},
      paramLabel = "DELTA",
      description = "The input delta file.")
  private URI delta;

  @Option(
      names = {"--reader"},
      paramLabel = "READER",
      description = "The tile reader.")
  private TileReaderOption tileReader = fast;

  @Override
  public Integer call() throws SQLException, ParseException, IOException {
    Configurator.setRootLevel(Level.getLevel(mixins.logLevel.name()));
    logger.info("{} processors available.", Runtime.getRuntime().availableProcessors());

    // Initialize the file system
    FileSystem fileSystem = mixins.fileSystem();

    // Read the configuration file
    logger.info("Reading configuration.");
    try (InputStream input = fileSystem.read(this.config)) {
      Yaml yaml = new Yaml(new Constructor(Config.class));
      Config config = yaml.load(input);
      PoolingDataSource datasource = PostgisHelper.poolingDataSource(database);

      // Initialize tile source and target tile stores
      logger.info("Initializing source and target tile stores.");
      TileStore tileSource = tileSource(datasource, config);
      TileStore tileTarget = tileTarget(fileSystem, repository);

      // Export the tiles
      logger.info("Generating the tiles.");
      Stream<Tile> tiles = tileStream(fileSystem, datasource);
      tiles.parallel().forEach(tile -> {
        try {
          byte[] bytes = tileSource.read(tile);
          if (bytes != null) {
            tileTarget.write(tile, bytes);
          }
        } catch (IOException ex) {
          throw new RuntimeException("An error occurred while creating the tiles", ex);
        }
      });
    }

    return 0;
  }

  public TileStore tileSource(PoolingDataSource dataSource, Config config) {
    switch (tileReader) {
      case fast:
        return new FastPostgisTileStore(dataSource, config);
      case slow:
        return new SlowPostgisTileStore(dataSource, config);
      default:
        throw new UnsupportedOperationException("Unsupported tile reader");
    }
  }

  private TileStore tileTarget(FileSystem fileSystem, URI repository) {
    return new FileSystemTileStore(fileSystem, repository);
  }

  private Stream<Tile> tileStream(FileSystem fileSystem, DataSource datasource)
      throws IOException, SQLException, ParseException {
    if (delta != null) {
      try (BufferedReader reader = new BufferedReader(
          new InputStreamReader(fileSystem.read(delta)))) {
        Stream<String> lines = reader.lines();
        return lines.flatMap(line -> {
          String[] array = line.split(",");
          int x = Integer.parseInt(array[0]);
          int y = Integer.parseInt(array[1]);
          int z = Integer.parseInt(array[2]);
          Tile tile = new Tile(x, y, z);
          return Tile.getTiles(tile.envelope(), minZoom, maxZoom);
        });
      }
    } else {
      try (Connection connection = datasource.getConnection()) {
        Envelope envelope = TileUtil.envelope(connection);
        return Tile.getTiles(envelope, minZoom, maxZoom);
      }
    }
  }

}
