package org.eclipse.xsmp.tests;

import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.eclipse.emf.common.util.URI;
import org.eclipse.xsmp.xcatalogue.Catalogue;
import org.eclipse.xtext.resource.SaveOptions;
import org.eclipse.xtext.resource.XtextResourceSet;
import org.eclipse.xtext.serializer.ISerializer;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.eclipse.xtext.testing.util.ParseHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.google.common.io.Files;
import com.google.inject.Inject;
import com.google.inject.Provider;

@ExtendWith(InjectionExtension.class)
@InjectWith(XsmpcatInjectorProvider.class)
class FormatterTest
{
  @Inject
  ISerializer serializer;

  @Inject
  ParseHelper<Catalogue> catalogueParseHelper;

  @Inject
  private Provider<XtextResourceSet> resourceSetProvider;

  @Test
  void formatter() throws Exception
  {
    final var in = getClass().getResource("/input/FormatterFile.xsmpcat");
    final var cat = catalogueParseHelper.parse(in.openStream(), URI.createFileURI(in.getFile()),
            null, resourceSetProvider.get());

    final var formatted = serializer.serialize(cat, SaveOptions.newBuilder().format().getOptions());
    final var out = new File("target/tests/FormatterFile.xsmpcat");
    out.getParentFile().mkdirs();
    Files.write(formatted.getBytes(), out);

    final InputStream inputStream = new ByteArrayInputStream(formatted.getBytes());
    final var br_formatted = new BufferedReader(new InputStreamReader(inputStream));

    final var ref = getClass().getResource("/reference/FormatterFile.xsmpcat").openStream();
    final var br_ref = new BufferedReader(new InputStreamReader(ref));

    assertLinesMatch(br_ref.lines(), br_formatted.lines());
  }
}
