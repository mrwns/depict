package org.openscience.cdk.app;

import org.junit.jupiter.api.Test;
import org.openscience.cdk.depict.Abbreviations;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AbbreviationLoaderTest {

  private static String loadError(String resource) {
    return assertThrows(IllegalArgumentException.class,
                        () -> AbbreviationLoader.load(new Abbreviations(), resource)).getMessage();
  }

  @Test
  void invalidSmilesFails() {
    assertThat(loadError("/org/openscience/cdk/app/abbr_invalid_smiles.smi"),
               containsString("abbr_invalid_smiles.smi:1"));
  }

  @Test
  void missingLabelFails() {
    assertThat(loadError("/org/openscience/cdk/app/abbr_no_label.smi"),
               containsString("abbr_no_label.smi:1: rejected"));
  }
}
