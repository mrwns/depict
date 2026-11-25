package org.openscience.cdk.app;

import org.openscience.cdk.depict.Abbreviations;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesGenerator;
import org.openscience.cdk.smiles.SmilesParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Loads an abbreviation file ("SMILES label" per line, '#' comments) like
 * {@link Abbreviations#loadFromFile(String)}, but fails on any entry that
 * CDK would silently drop (invalid SMILES, no label, 2+ attachment points).
 * A molecule listed twice keeps the later label, as in CDK, with a warning.
 */
final class AbbreviationLoader {

  private static final Logger LOGGER = LoggerFactory.getLogger(AbbreviationLoader.class);

  private AbbreviationLoader() {
  }

  static void load(Abbreviations abbr, String resource) throws IOException {
    SmilesParser smipar = new SmilesParser(SilentChemObjectBuilder.getInstance());
    SmilesGenerator usmigen = SmilesGenerator.unique();
    Map<String, Integer> lineOfMolecule = new HashMap<>();
    List<String> lines = readLines(resource);
    for (int i = 0; i < lines.size(); i++) {
      String line = lines.get(i);
      if (line.isEmpty() || line.charAt(0) == '#')
        continue;
      String where = resource + ":" + (i + 1);
      try {
        Integer first = lineOfMolecule.putIfAbsent(usmigen.create(smipar.parseSmiles(line)), i + 1);
        if (first != null)
          LOGGER.warn("{}: same molecule as line {}, keeping this label: {}", where, first, line);
        if (!abbr.add(line))
          throw new IllegalArgumentException(where + ": rejected (no label or 2+ attachment points): " + line);
      } catch (CDKException e) {
        throw new IllegalArgumentException(where + ": " + e.getMessage() + ": " + line, e);
      }
    }
  }

  private static List<String> readLines(String resource) throws IOException {
    InputStream in = Objects.requireNonNull(AbbreviationLoader.class.getResourceAsStream(resource),
                                            "missing resource " + resource);
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
      return reader.lines().toList();
    }
  }
}
