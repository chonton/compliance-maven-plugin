package org.honton.chas.license.maven.plugin.compliance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.maven.model.License;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

/** */
public class LicenseSetTest {

  @Test
  public void loadLicenses() throws MojoExecutionException {
    List<LicenseRegex> licenses = new ArrayList<>();
    LicenseSet.loadLicenses(licenses, "osi-widely-used");
    LicenseRegex apache = licenses.get(0);
    Assert.assertEquals(
        "((The\\s+)?Apache\\s+(Software\\s+)?License\\s*(,\\s*Version\\s+)?2\\.0)"
            + "|(Apache-2\\.0)",
        apache.getName());
    Assert.assertEquals(
        "(https?://www\\.apache\\.org/licenses/LICENSE-2\\.0(\\.txt)?)"
            + "|(https?://opensource\\.org/licenses/Apache-2\\.0)",
        apache.getUrl());
  }

  private static License createLicense(String name, String url) {
    License license = new License();
    license.setName(name);
    license.setUrl(url);
    return license;
  }

  private static LicenseRegex loadLicenseRegex(String resources, int index)
      throws MojoExecutionException {
    List<LicenseRegex> licenses = new ArrayList<>();
    LicenseSet.loadLicenses(licenses, resources);
    LicenseRegex apache = licenses.get(index);
    apache.checkRegex();
    return apache;
  }

  @Test
  public void testCompareApache() throws MojoExecutionException {
    License license =
        createLicense("Apache Software Licenses", "http://www.apache.org/licenses/LICENSE-2.0.txt");
    Assert.assertTrue(loadLicenseRegex("osi-permissive", 0).matches(license));
  }

  @Test
  public void testCompareLgpl() throws MojoExecutionException {
    License license = createLicense("GNU Lesser Public License", null);
    Assert.assertTrue(loadLicenseRegex("osi-permissive", 5).matches(license));
  }

  @Test
  public void testMatchEplv2() throws MojoExecutionException {
    Log logger = Mockito.mock(Log.class);
    List<LicenseRegex> licenses = new ArrayList<>();
    LicenseSet.loadLicenses(licenses, "osi-permissive");
    LicenseMatcher licenseMatcher = new LicenseMatcher(logger, licenses);

    License eplV2 =
        createLicense("Eclipse Public License v2.0", "https://www.eclipse.org/legal/epl-v20.html");
    Assert.assertTrue(licenseMatcher.hasAcceptableLicense(Collections.singletonList(eplV2)));
  }
}
