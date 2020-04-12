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

package com.baremaps.osm.stream;

import com.baremaps.core.tile.Tile;
import com.baremaps.osm.database.NodeTable;
import com.baremaps.osm.database.RelationTable;
import com.baremaps.osm.database.WayTable;
import com.baremaps.osm.geometry.NodeBuilder;
import com.baremaps.osm.geometry.RelationBuilder;
import com.baremaps.osm.geometry.WayBuilder;
import com.baremaps.osm.model.Entity;
import com.baremaps.osm.model.Node;
import com.baremaps.osm.model.Relation;
import com.baremaps.osm.model.Way;
import com.baremaps.osm.osmxml.Change;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class DatabaseDiffer implements Consumer<Change> {

  private final WayBuilder wayBuilder;
  private final RelationBuilder relationBuilder;
  private final NodeBuilder nodeBuilder;

  private final NodeTable nodeStore;
  private final WayTable wayStore;
  private final RelationTable relationStore;

  private final int z = 18;

  private final Set<Tile> tiles = new HashSet<>();

  public DatabaseDiffer(
      NodeBuilder nodeBuilder, WayBuilder wayBuilder,
      RelationBuilder relationBuilder, NodeTable nodeStore,
      WayTable wayStore,
      RelationTable relationStore) {
    this.nodeBuilder = nodeBuilder;
    this.wayBuilder = wayBuilder;
    this.relationBuilder = relationBuilder;
    this.nodeStore = nodeStore;
    this.wayStore = wayStore;
    this.relationStore = relationStore;
  }

  @Override
  public void accept(Change change) {
    Entity entity = change.getEntity();
    switch (change.getType()) {
      case delete:
      case modify:
        if (entity instanceof Node) {
          NodeTable.Node node = nodeStore.select(entity.getInfo().getId());
          tiles.addAll(Tile.getTiles(node.getPoint(), z).collect(Collectors.toList()));
        } else if (entity instanceof Way) {
          WayTable.Way way = wayStore.select(entity.getInfo().getId());
          tiles.addAll(Tile.getTiles(way.getGeometry(), z).collect(Collectors.toList()));
        } else if (entity instanceof Relation) {
          RelationTable.Relation relation = relationStore.select(entity.getInfo().getId());
          tiles.addAll(Tile.getTiles(relation.getGeometry(), z).collect(Collectors.toList()));
        }
      case create:
        if (entity instanceof Node) {
          Node node = (Node) entity;
          tiles.addAll(Tile.getTiles(nodeBuilder.build(node), z).collect(Collectors.toList()));
        } else if (entity instanceof Way) {
          Way way = (Way) entity;
          tiles.addAll(Tile.getTiles(wayBuilder.build(way), z).collect(Collectors.toList()));
        } else if (entity instanceof Relation) {
          Relation relation = (Relation) entity;
          tiles.addAll(Tile.getTiles(relationBuilder.build(relation), z).collect(Collectors.toList()));
        }
    }
  }

  public Set<Tile> getTiles() {
    return tiles;
  }

}