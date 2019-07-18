package io.gazetteer.osm.osmpbf;

import io.gazetteer.common.stream.HoldingConsumer;
import java.util.Spliterator;
import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;

import static io.gazetteer.osm.OSMTestUtil.osmPbfData;
import static org.junit.jupiter.api.Assertions.*;

public class FileBlockSpliteratorTest {

  @Test
  public void tryAdvance() throws FileNotFoundException {
    Spliterator<FileBlock> reader = PBFUtil.spliterator(osmPbfData());
    reader.forEachRemaining(fileBlock -> assertNotNull(fileBlock));
    assertFalse(reader.tryAdvance(new HoldingConsumer<>()));
  }

}
