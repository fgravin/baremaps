package io.gazetteer.cli.commands;

import com.google.common.base.Stopwatch;
import io.gazetteer.core.postgis.PostgisHelper;
import io.gazetteer.tiles.Tile;
import io.gazetteer.tiles.TileReader;
import io.gazetteer.tiles.TileWriter;
import io.gazetteer.tiles.config.Config;
import io.gazetteer.tiles.file.FileTileStore;
import io.gazetteer.tiles.postgis.BasicTileReader;
import io.gazetteer.tiles.postgis.WithTileReader;
import io.gazetteer.tiles.util.TileUtil;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.apache.commons.dbcp2.PoolingDataSource;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "tiles")
public class Tiles implements Callable<Integer> {

  @Parameters(
      index = "0",
      paramLabel = "CONFIG_FILE",
      description = "The YAML configuration file.")
  private File file;

  @Parameters(
      index = "1",
      paramLabel = "POSTGRES_DATABASE",
      description = "The Postgres database.")
  private String database;

  @Parameters(
      index = "2",
      paramLabel = "TILE_DIRECTORY",
      description = "The tile directory.")
  private Path directory;

  @Option(
      names = {"--minZoom"},
      description = "The minimal zoom level.")
  private int minZoom = 0;

  @Option(
      names = {"--maxZoom"},
      description = "The maximal zoom level.")
  private int maxZoom = 14;

  @Option(
      names = {"-t", "--tile-reader"},
      description = "The tile reader.")
  private String tileReader = "basic";

  public TileReader initTileReader(PoolingDataSource dataSource, Config config) {
    switch (tileReader) {
      case "basic":
        return new BasicTileReader(dataSource, config);
      case "with":
        return new WithTileReader(dataSource, config);
      default:
        throw new UnsupportedOperationException("Unsupported tile reader");
    }
  }

  @Override
  public Integer call() throws SQLException, ParseException, FileNotFoundException {
    Stopwatch stopWatch = Stopwatch.createStarted();

    // Read the configuration toInputStream
    Config config = Config.load(new FileInputStream(file));
    PoolingDataSource datasource = PostgisHelper.poolingDataSource(database);

    // Choose the tile reader
    TileReader tileReader = initTileReader(datasource, config);
    TileWriter tileWriter = new FileTileStore(directory);

    //AmazonS3 client = AmazonS3ClientBuilder.standard().defaultClient();
    //TileWriter tileWriter = new S3TileStore(client, "gazetteer-tiles");

    try (Connection connection = datasource.getConnection()) {
      Geometry geometry = TileUtil.bbox(connection);
      Stream<Tile> coords = TileUtil.getTiles(geometry, minZoom, maxZoom);
      coords.forEach(xyz -> {
        try {
          byte[] tile = tileReader.read(xyz);
          tileWriter.write(xyz, tile);
        } catch (Exception e) {
          e.printStackTrace();
        }
      });
    }
    System.out.println(String.format("-> %dms", stopWatch.elapsed(TimeUnit.MILLISECONDS)));

    return 0;
  }

}
