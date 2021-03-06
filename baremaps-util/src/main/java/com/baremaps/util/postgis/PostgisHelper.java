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

package com.baremaps.util.postgis;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import org.apache.commons.dbcp2.ConnectionFactory;
import org.apache.commons.dbcp2.DriverManagerConnectionFactory;
import org.apache.commons.dbcp2.PoolableConnection;
import org.apache.commons.dbcp2.PoolableConnectionFactory;
import org.apache.commons.dbcp2.PoolingDataSource;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;

public final class PostgisHelper {

  private PostgisHelper() {

  }

  private static final String DATABASE_URL = "jdbc:postgresql://{0}:{1}/{2}?user={3}&password={4}&allowMultiQueries={5}";

  public static String url(
      String hostname,
      Integer port,
      String database,
      String username,
      String password,
      Boolean allowMultiQueries) {
    checkNotNull(hostname);
    checkNotNull(port);
    checkNotNull(database);
    checkNotNull(username);
    checkNotNull(password);
    checkNotNull(allowMultiQueries);
    return MessageFormat
        .format(DATABASE_URL, hostname, port, database, username, password, allowMultiQueries);
  }

  public static String url(String database, String user, String password) {
    return url("localhost", 5432, database, user, password, true);
  }

  public static PoolingDataSource poolingDataSource(String url) {
    ConnectionFactory connectionFactory =
        new DriverManagerConnectionFactory(url, null);
    PoolableConnectionFactory poolableConnectionFactory =
        new PoolableConnectionFactory(connectionFactory, null);
    ObjectPool<PoolableConnection> connectionPool =
        new GenericObjectPool<>(poolableConnectionFactory);
    poolableConnectionFactory.setPool(connectionPool);
    PoolingDataSource<PoolableConnection> dataSource =
        new PoolingDataSource<>(connectionPool);
    return dataSource;
  }

  public static void executeScript(Connection connection, String script) throws IOException, SQLException {
    URL url = Resources.getResource(script);
    String sql = Resources.toString(url, Charsets.UTF_8);
    try (Statement statement = connection.createStatement()) {
      statement.execute(sql);
    }
  }

}
