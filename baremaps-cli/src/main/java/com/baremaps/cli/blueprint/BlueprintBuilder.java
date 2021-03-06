/*
 * Copyright (C) 2020 The Baremaps Authors
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

package com.baremaps.cli.blueprint;

import com.baremaps.tiles.config.Config;
import com.baremaps.tiles.config.Layer;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Lists;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BlueprintBuilder {

  private final Config config;

  public BlueprintBuilder(Config config) {
    this.config = config;
  }

  public Map<String, Object> build() {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("id", "baremaps");
    map.put("version", 8);
    map.put("sprite", "");
    map.put("glyphs", "https://daglvzoa8byzd.cloudfront.net/{fontstack}/{range}.pbf");
    map.put("sources", ImmutableSortedMap.naturalOrder()
        .put("baremaps", ImmutableSortedMap.naturalOrder()
            .put("type", "vector")
            .put("minzoom", config.getMinZoom())
            .put("maxzoom", config.getMaxZoom())
            .put("tiles", Arrays.asList(String.format("http://%s:%s/tiles/{z}/{x}/{y}.pbf",
                config.getHost(),
                config.getPort())))
            .build())
        .build());

    if (config.getStyles() == null || config.getStyles().isEmpty()) {
      map.put("layers", buildStyles());
    } else {
      map.put("layers", config.getStyles());
    }

    return map;
  }

  public List<Map<String, Object>> buildStyles() {
    List<Map<String, Object>> styles = Lists.newArrayList();
    styles.add(buildBackground());
    styles.addAll(config.getLayers().stream()
        .flatMap(layer -> buildStyle(layer))
        .collect(Collectors.toList()));
    return styles;
  }

  private Map<String, Object> buildBackground() {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("id", "background");
    map.put("type", "background");
    map.put("layout", ImmutableSortedMap.naturalOrder()
        .put("visibility", "visible")
        .build());
    map.put("paint", ImmutableSortedMap.naturalOrder()
        .put("background-color", "rgb(64, 92, 176)")
        .build());
    return map;
  }

  private Stream<Map<String, Object>> buildStyle(Layer layer) {
    switch (layer.getType()) {
      case "point":
        return Stream.of(buildPointStyle(layer, String.format("%s_point", layer.getId())));
      case "linestring":
        return Stream.of(buildLinestringStyle(layer, String.format("%s_linestring", layer.getId())));
      case "polygon":
        return Stream.of(buildPolygonStyle(layer, String.format("%s_polygon", layer.getId())));
      case "geometry":
        return Stream.of(
            buildPointStyle(layer, String.format("%s_point", layer.getId())),
            buildLinestringStyle(layer, String.format("%s_linestring", layer.getId())),
            buildPolygonStyle(layer, String.format("%s_polygon", layer.getId())));
      default:
        return Stream.empty();
    }
  }

  private Map<String, Object> buildPointStyle(Layer layer, String id) {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("id", id);
    map.put("source", "baremaps");
    map.put("source-layer", layer.getId());
    map.put("minzoom", layer.getMinZoom());
    map.put("maxzoom", layer.getMaxZoom());
    map.put("type", "circle");
    map.put("layout", ImmutableSortedMap.naturalOrder()
        .put("visibility", "visible")
        .build());
    map.put("filter", Arrays.asList("all", Arrays.asList("==", "geometry", "point")));
    map.put("paint", ImmutableSortedMap.naturalOrder()
        .put("circle-color", "rgb(229, 235, 247)")
        .put("circle-radius", 2)
        .build());
    return map;
  }

  private Map<String, Object> buildLinestringStyle(Layer layer, String id) {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("id", id);
    map.put("source", "baremaps");
    map.put("source-layer", layer.getId());
    map.put("minzoom", layer.getMinZoom());
    map.put("maxzoom", layer.getMaxZoom());
    map.put("type", "line");
    map.put("layout", ImmutableSortedMap.naturalOrder()
        .put("visibility", "visible")
        .build());
    map.put("filter", Arrays.asList("all", Arrays.asList("==", "geometry", "linestring")));
    map.put("paint", ImmutableSortedMap.naturalOrder()
        .put("line-color", "rgb(152, 174, 221)")
        .put("line-width", 1)
        .build());
    return map;
  }

  private Map<String, Object> buildPolygonStyle(Layer layer, String id) {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("id", id);
    map.put("source", "baremaps");
    map.put("source-layer", layer.getId());
    map.put("minzoom", layer.getMinZoom());
    map.put("maxzoom", layer.getMaxZoom());
    map.put("type", "fill");
    map.put("layout", ImmutableSortedMap.naturalOrder()
        .put("visibility", "visible")
        .build());
    map.put("filter", Arrays.asList("all", Arrays.asList("==", "geometry", "polygon")));
    map.put("paint", ImmutableSortedMap.naturalOrder()
        .put("fill-color", "rgba(152, 174, 221, 0.1)")
        .put("fill-outline-color", "rgb(152, 174, 221)")
        .put("fill-opacity", 1)
        .put("fill-antialias", true)
        .build());
    return map;
  }


}
