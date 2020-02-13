package io.gazetteer.tiles.postgis;

import static org.junit.jupiter.api.Assertions.*;

import io.gazetteer.tiles.config.Layer;
import io.gazetteer.tiles.config.Query;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class QueryParserTest {

  @Test
  void parse() {
    Layer layer = new Layer();

    var q1 = QueryParser.parse(layer, "SELECT id, tags, geom FROM table");
    assertEquals(q1.getId(), "id");
    assertEquals(q1.getTags(), "tags");
    assertEquals(q1.getGeom(), "geom");
    assertEquals(q1.getFrom(), "table");
    assertEquals(q1.getWhere(), Optional.empty());

    var q2 = QueryParser.parse(layer, "SELECT id, tags, geom FROM table WHERE condition");
    assertEquals(q2.getId(), "id");
    assertEquals(q2.getTags(), "tags");
    assertEquals(q2.getGeom(), "geom");
    assertEquals(q2.getFrom(), "table");
    assertEquals(q2.getWhere(), Optional.ofNullable("condition"));

    var q3 = QueryParser.parse(layer, "SELECT id, hstore('tag', tag), geom FROM table WHERE condition");
    assertEquals(q3.getId(), "id");
    assertEquals(q3.getTags(), "hstore('tag', tag)");
    assertEquals(q3.getGeom(), "geom");
    assertEquals(q3.getFrom(), "table");
    assertEquals(q3.getWhere(), Optional.ofNullable("condition"));

    var q4 = QueryParser.parse(layer, "SELECT id, hstore(array['tag1', 'tag2'], array[tag1, tag2]), geom FROM table WHERE condition");
    assertEquals(q4.getId(), "id");
    assertEquals(q4.getTags(), "hstore(array['tag1', 'tag2'], array[tag1, tag2])");
    assertEquals(q4.getGeom(), "geom");
    assertEquals(q4.getFrom(), "table");
    assertEquals(q4.getWhere(), Optional.ofNullable("condition"));

  }
}