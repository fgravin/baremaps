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

package com.baremaps.osm.database;

import com.baremaps.osm.database.NodeTable.Node;
import com.baremaps.osm.geometry.GeometryUtil;
import com.baremaps.util.postgis.CopyWriter;
import com.google.common.base.Objects;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.locationtech.jts.geom.Point;
import org.postgresql.PGConnection;
import org.postgresql.copy.PGCopyOutputStream;

public class NodeTable implements Table<Node> {

  public static class Node {

    private final long id;

    private final int version;

    private final LocalDateTime timestamp;

    private final long changeset;

    private final int userId;

    private final Map<String, String> tags;

    private final Point geometry;

    public Node(
        long id,
        int version,
        LocalDateTime timestamp,
        long changeset,
        int userId,
        Map<String, String> tags,
        Point geometry) {
      this.id = id;
      this.version = version;
      this.timestamp = timestamp;
      this.changeset = changeset;
      this.userId = userId;
      this.tags = tags;
      this.geometry = geometry;
    }

    public long getId() {
      return id;
    }

    public int getVersion() {
      return version;
    }

    public LocalDateTime getTimestamp() {
      return timestamp;
    }

    public long getChangeset() {
      return changeset;
    }

    public int getUserId() {
      return userId;
    }

    public Map<String, String> getTags() {
      return tags;
    }

    public Point getPoint() {
      return geometry;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Node node = (Node) o;
      return id == node.id &&
          version == node.version &&
          changeset == node.changeset &&
          userId == node.userId &&
          Objects.equal(timestamp, node.timestamp) &&
          Objects.equal(tags, node.tags) &&
          Objects.equal(geometry, node.geometry);
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(id, version, timestamp, changeset, userId, tags, geometry);
    }
  }

  private static final String SELECT =
      "SELECT version, uid, timestamp, changeset, tags, st_asbinary(geom) FROM osm_nodes WHERE id = ?";

  private static final String SELECT_IN =
      "SELECT id, version, uid, timestamp, changeset, tags, st_asbinary(geom) FROM osm_nodes WHERE id = ANY (?)";

  private static final String INSERT =
      "INSERT INTO osm_nodes (id, version, uid, timestamp, changeset, tags, geom) VALUES (?, ?, ?, ?, ?, ?, ?)"
          + "ON CONFLICT (id) DO UPDATE SET "
          + "version = excluded.version, "
          + "uid = excluded.uid, "
          + "timestamp = excluded.timestamp, "
          + "changeset = excluded.changeset, "
          + "tags = excluded.tags, "
          + "geom = excluded.geom";

  private static final String DELETE =
      "DELETE FROM osm_nodes WHERE id = ?";

  private static final String COPY =
      "COPY osm_nodes (id, version, uid, timestamp, changeset, tags, geom) FROM STDIN BINARY";

  private final DataSource dataSource;

  public NodeTable(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  public Node select(Long id) {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(SELECT)) {
      statement.setLong(1, id);
      ResultSet result = statement.executeQuery();
      if (result.next()) {
        int version = result.getInt(1);
        int uid = result.getInt(2);
        LocalDateTime timestamp = result.getObject(3, LocalDateTime.class);
        long changeset = result.getLong(4);
        Map<String, String> tags = (Map<String, String>) result.getObject(5);
        Point point = (Point) GeometryUtil.deserialize(result.getBytes(6));
        return new Node(id, version, timestamp, changeset, uid, tags, point);
      } else {
        throw new IllegalArgumentException();
      }
    } catch (SQLException e) {
      throw new DatabaseException(e);
    }
  }

  public List<Node> select(List<Long> ids) {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(SELECT_IN)) {
      statement.setArray(1, connection.createArrayOf("int8", ids.toArray()));
      ResultSet result = statement.executeQuery();
      Map<Long, Node> nodes = new HashMap<>();
      while (result.next()) {
        long id = result.getLong(1);
        int version = result.getInt(2);
        int uid = result.getInt(3);
        LocalDateTime timestamp = result.getObject(4, LocalDateTime.class);
        long changeset = result.getLong(5);
        Map<String, String> tags = (Map<String, String>) result.getObject(6);
        Point point = (Point) GeometryUtil.deserialize(result.getBytes(7));
        nodes.put(id, new Node(id, version, timestamp, changeset, uid, tags, point));
      }
      return ids.stream().map(id -> nodes.get(id)).collect(Collectors.toList());
    } catch (SQLException e) {
      throw new DatabaseException(e);
    }
  }

  public void insert(Node row) {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(INSERT)) {
      statement.setLong(1, row.getId());
      statement.setInt(2, row.getVersion());
      statement.setInt(3, row.getUserId());
      statement.setObject(4, row.getTimestamp());
      statement.setLong(5, row.getChangeset());
      statement.setObject(6, row.getTags());
      statement.setBytes(7, GeometryUtil.serialize(row.getPoint()));
      statement.execute();
    } catch (SQLException e) {
      throw new DatabaseException(e);
    }
  }

  public void insert(List<Node> rows) {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(INSERT)) {
      for (Node row : rows) {
        statement.clearParameters();
        statement.setLong(1, row.getId());
        statement.setInt(2, row.getVersion());
        statement.setInt(3, row.getUserId());
        statement.setObject(4, row.getTimestamp());
        statement.setLong(5, row.getChangeset());
        statement.setObject(6, row.getTags());
        statement.setBytes(7, GeometryUtil.serialize(row.getPoint()));
        statement.addBatch();
      }
      statement.executeBatch();
    } catch (SQLException e) {
      throw new DatabaseException(e);
    }
  }

  public void delete(Long id) {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(DELETE)) {
      statement.setLong(1, id);
      statement.execute();
    } catch (SQLException e) {
      throw new DatabaseException(e);
    }
  }

  public void delete(List<Long> ids) {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(DELETE)) {
      for (Long id : ids) {
        statement.clearParameters();
        statement.setLong(1, id);
        statement.addBatch();
      }
      statement.executeBatch();
    } catch (SQLException e) {
      throw new DatabaseException(e);
    }
  }

  public void copy(List<Node> rows) {
    try (Connection connection = dataSource.getConnection()) {
      PGConnection pgConnection = connection.unwrap(PGConnection.class);
      try (CopyWriter writer = new CopyWriter(new PGCopyOutputStream(pgConnection, COPY))) {
        writer.writeHeader();
        for (Node node : rows) {
          writer.startRow(7);
          writer.writeLong(node.getId());
          writer.writeInteger(node.getVersion());
          writer.writeInteger(node.getUserId());
          writer.writeLocalDateTime(node.getTimestamp());
          writer.writeLong(node.getChangeset());
          writer.writeHstore(node.getTags());
          writer.writeGeometry(node.getPoint());
        }
      }
    } catch (IOException | SQLException e) {
      throw new DatabaseException(e);
    }
  }
}
