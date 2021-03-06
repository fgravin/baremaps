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

package com.baremaps.tiles;

import com.baremaps.util.tile.Tile;
import java.io.IOException;
import org.locationtech.jts.geom.Envelope;

public interface TileStore {

  byte[] read(Tile tile) throws IOException;

  void write(Tile tile, byte[] bytes) throws IOException;

  void delete(Tile tile) throws IOException;

}
