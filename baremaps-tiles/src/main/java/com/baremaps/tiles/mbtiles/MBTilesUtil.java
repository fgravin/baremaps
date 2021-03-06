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

package com.baremaps.tiles.mbtiles;

import com.baremaps.util.tile.Tile;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

public final class MBTilesUtil {

  private static final String CREATE_TABLE_METADATA =
      "CREATE TABLE metadata (name TEXT, value TEXT, PRIMARY KEY (name))";

  private static final String CREATE_TABLE_TILES =
      "CREATE TABLE tiles (zoom_level INTEGER, tile_column INTEGER, tile_row INTEGER, tile_data BLOB, PRIMARY KEY (zoom_level, tile_column, tile_row))";

  private static final String CREATE_INDEX_TILES =
      "CREATE UNIQUE INDEX tile_index on tiles (zoom_level, tile_column, tile_row)";

  private static final String SELECT_METADATA =
      "SELECT name, value FROM metadata";

  private static final String SELECT_TILE =
      "SELECT tile_data FROM tiles WHERE zoom_level = ? AND tile_column = ? AND tile_row = ?";

  private static final String INSERT_METADATA =
      "INSERT INTO metadata (name, value) VALUES (?, ?)";

  private static final String INSERT_TILE =
      "INSERT INTO tiles (zoom_level, tile_column, tile_row, tile_data) VALUES (?, ?, ?, ?)";

  private static final String DELETE_TILE =
      "DELETE FROM tiles WHERE zoom_level = ? AND tile_column = ? AND tile_row = ?";

  public static final String TRUNCATE_METADATA = "TRUNCATE TABLE metadata";

  public static void initializeDatabase(Connection connection) throws SQLException {
    try (Statement statement = connection.createStatement()) {
      statement.execute(CREATE_TABLE_METADATA);
      statement.execute(CREATE_TABLE_TILES);
      statement.execute(CREATE_INDEX_TILES);
    }
  }

  public static Map<String, String> readMetadata(Connection connection) throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement(SELECT_METADATA);
        ResultSet resultSet = statement.executeQuery()) {
      Map<String, String> metadata = new HashMap<>();
      while (resultSet.next()) {
        String name = resultSet.getString("name");
        String value = resultSet.getString("value");
        metadata.put(name, value);
      }
      return metadata;
    }
  }

  public static byte[] readTile(Connection connection, Tile tile) throws SQLException {
    try (PreparedStatement statement = readTileStatement(connection, tile);
        ResultSet resultSet = statement.executeQuery()) {
      if (resultSet.next()) {
        return resultSet.getBytes("tile_data");
      } else {
        throw new SQLException("The tile does not exist");
      }
    }
  }

  private static PreparedStatement readTileStatement(Connection connection, Tile tile)
      throws SQLException {
    PreparedStatement statement = connection.prepareStatement(SELECT_TILE);
    statement.setInt(1, tile.getZ());
    statement.setInt(2, tile.getX());
    statement.setInt(3, reverseY(tile.getY(), tile.getZ()));
    return statement;
  }

  public static void truncateMetadata(Connection connection) throws SQLException {
    try (Statement statement = connection.createStatement()) {
      statement.execute(TRUNCATE_METADATA);
    }
  }

  public static void writeMetadata(Connection connection, Map<String, String> metadata)
      throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement(INSERT_METADATA)) {
      for (Map.Entry<String, String> entry : metadata.entrySet()) {
        statement.setString(1, entry.getKey());
        statement.setString(2, entry.getValue());
        statement.executeUpdate();
      }
    }
  }

  public static void writeTile(Connection connection, Tile tile, byte[] bytes) throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement(INSERT_TILE)) {
      statement.setInt(1, tile.getZ());
      statement.setInt(2, tile.getX());
      statement.setInt(3, reverseY(tile.getY(), tile.getZ()));
      statement.setBytes(4, bytes);
      statement.executeUpdate();
    }
  }

  public static void deleteTile(Connection connection, Tile tile) throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement(DELETE_TILE)) {
      statement.setInt(1, tile.getZ());
      statement.setInt(2, tile.getX());
      statement.setInt(3, reverseY(tile.getY(), tile.getZ()));
      statement.execute();
    }
  }

  private static int reverseY(int y, int z) {
    return (int) (Math.pow(2.0, z) - 1 - y);
  }
}
