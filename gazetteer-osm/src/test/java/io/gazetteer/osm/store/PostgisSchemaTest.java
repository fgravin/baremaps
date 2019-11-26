package io.gazetteer.osm.store;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.gazetteer.common.postgis.DatabaseUtils;
import io.gazetteer.osm.OSMTestUtil;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

public class PostgisSchemaTest {

  @Test
  @Tag("integration")
  public void resetDatabase() throws SQLException, IOException {
    try (Connection connection = DriverManager.getConnection(OSMTestUtil.DATABASE_URL)) {
      DatabaseUtils.executeScript(connection, "osm_create_extensions.sql");
      DatabaseUtils.executeScript(connection, "osm_create_tables.sql");
      assertTrue(tableExists("osm_headers"));
      assertTrue(tableExists("osm_nodes"));
      assertTrue(tableExists("osm_ways"));
      assertTrue(tableExists("osm_relations"));
    }
  }

  public boolean tableExists(String table) throws SQLException {
    try (Connection connection = DriverManager.getConnection(OSMTestUtil.DATABASE_URL)) {
      DatabaseMetaData metadata = connection.getMetaData();
      ResultSet tables = metadata.getTables(null, null, table, null);
      return tables.next();
    }
  }
}