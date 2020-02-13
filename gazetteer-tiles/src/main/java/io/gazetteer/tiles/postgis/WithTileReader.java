package io.gazetteer.tiles.postgis;

import io.gazetteer.tiles.Tile;
import io.gazetteer.tiles.TileException;
import io.gazetteer.tiles.config.Config;
import io.gazetteer.tiles.config.Layer;
import io.gazetteer.tiles.postgis.QueryParser.Query;
import java.io.ByteArrayOutputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectItem;
import org.apache.commons.dbcp2.PoolingDataSource;

public class WithTileReader extends AbstractTileReader {

  private static final String SQL_WITH = "WITH {0} {1}";

  private static final String SQL_SOURCE = "{0} AS (SELECT id, "
      + "(tags || hstore(''geometry'', lower(replace(st_geometrytype(geom), ''ST_'', '''')))) as tags, "
      + "ST_AsMvtGeom(geom, {5}, 4096, 256, true) AS geom "
      + "FROM (SELECT {1}, {2}, {3} FROM {4}) AS {0} WHERE ST_Intersects(geom, {5}))";

  private static final String SQL_LAYER = "SELECT ST_AsMVT(mvt_geom, ''{0}'', 4096) FROM ({1}) as mvt_geom";

  private static final String SQL_QUERY = "SELECT id, tags::jsonb, geom FROM {3}";

  private static final String SQL_CONDITION = " WHERE {0}";

  private static final String SQL_COMMA = ", ";

  private static final String SQL_UNION = " UNION All ";

  private final PoolingDataSource datasource;

  private final Config config;

  private final Map<Layer, List<Query>> queries;

  public WithTileReader(PoolingDataSource datasource, Config config) {
    this.datasource = datasource;
    this.config = config;
    this.queries = config.getLayers().stream()
        .flatMap(layer -> layer.getQueries().stream().map(query -> QueryParser.parse(layer, query.getSql())))
        .collect(Collectors.groupingBy(q -> q.getLayer()));
  }

  @Override
  public byte[] read(Tile tile) throws TileException {
    try (Connection connection = datasource.getConnection();
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        GZIPOutputStream gzip = new GZIPOutputStream(data)) {
      String sql = query(tile);
      System.out.println(sql);
      try (Statement statement = connection.createStatement()) {
        ResultSet result = statement.executeQuery(sql);
        while (result.next()) {
          gzip.write(result.getBytes(1));
        }
      }
      gzip.close();
      return data.toByteArray();
    } catch (Exception e) {
      throw new TileException(e);
    }

  }

  private String query(Tile tile) {
    System.out.println(tile);
    String sources = queries.entrySet().stream()
        .filter(entry -> entry.getKey().getMinZoom() <= tile.getZ() && entry.getKey().getMaxZoom() >= tile.getZ())
        .flatMap(entry -> entry.getValue().stream().map(query -> MessageFormat.format(SQL_SOURCE,
            query.getSource(),
            query.getId(),
            query.getTags(),
            query.getGeom(),
            query.getFrom(),
            envelope(tile))))
        .collect(Collectors.toSet())
        .stream()
        .collect(Collectors.joining(SQL_COMMA));
    String targets = queries.entrySet().stream()
        .filter(entry -> entry.getKey().getMinZoom() <= tile.getZ() && entry.getKey().getMaxZoom() >= tile.getZ())
        .map(entry -> {
          String queries = entry.getValue().stream()
              .map(select -> {
                String l = MessageFormat.format(SQL_QUERY, select.getId(), select.getTags(), select.getGeom(), select.getSource());
                String r = select.getWhere()
                    .map(s -> MessageFormat.format(SQL_CONDITION, s))
                    .orElse("");
                return l + r;
              })
              .collect(Collectors.joining(SQL_UNION));
          return MessageFormat.format(SQL_LAYER, entry.getKey().getName(), queries);
        })
        .collect(Collectors.joining(SQL_UNION));
    return MessageFormat.format(SQL_WITH, sources, targets);
  }

}
