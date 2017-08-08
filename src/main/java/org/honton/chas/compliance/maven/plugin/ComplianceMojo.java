package org.honton.chas.compliance.maven.plugin;

import java.util.Arrays;
import java.util.List;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingResult;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.shared.artifact.resolve.ArtifactResolver;
import org.honton.chas.compliance.maven.plugin.license.LicenseMatcher;
import org.honton.chas.compliance.maven.plugin.license.LicenseRegex;
import org.honton.chas.compliance.maven.plugin.license.LicenseSet;

/**
 * Check dependencies' licenses for compliance.  For each dependency, check if any of the acceptable
 * licenses match the any of the dependency licenses.  A match is successful if either the acceptable
 * license URL regular expression is matched by the dependency license URL, or the acceptable
 * license name regular expression is matched by the dependency license name.
 */
@Mojo(name = "compliance", defaultPhase = LifecyclePhase.VALIDATE)
public class ComplianceMojo extends AbstractMojo {

  @Parameter(defaultValue = "${project}", readonly = true)
  private MavenProject project;

  @Parameter(defaultValue = "${session}", required = true, readonly = true)
  private MavenSession session;

  @Component
  private RepositorySystem repositorySystem;

  @Component
  private ProjectBuilder mavenProjectBuilder;

  @Component
  private ArtifactResolver artifactResolver;

  /**
   * Skip checking licence compliance
   */
  @Parameter(property = "compliance.skip", defaultValue = "false")
  private boolean skipCompliance;

  /**
   * The licenses that are allowed.  Each license has a name and URL regular expression.
   * If not set, then licenses from acceptableLicenseResource are used.
   */
  @Parameter
  private List<LicenseRegex> acceptableLicenses;

  /**
   * The resource containing licenses that are allowed.
   */
  @Parameter(property = "compliance.licenses", defaultValue = "osi")
  private String acceptableLicenseResource;

  /**
   * The dependencies to exclude from checking compliance.  These will be in the form of
   * <em></em>groupId:artifactId[[:type]:classifier]</em>. Wildcard characters '*' and '?' can be
   * used to do glob-like pattern matching.
   */
  @Parameter
  private List<String> excludeDependencies;

  /**
   * The dependency scopes to check.  The default dependency scopes are compile, runtime, provided, test.
   */
  @Parameter
  private List<String> scopes;

  private DependencyMatcher excludeMatcher;
  private LicenseMatcher licenseMatcher;

  public void execute() throws MojoExecutionException, MojoFailureException {
    if (skipCompliance) {
      getLog().info("skipping license compliance");
      return;
    }
    if (scopes == null) {
      scopes = Arrays.asList("compile", "runtime", "provided", "test");
    }
    if( acceptableLicenses == null) {
      getLog().debug("using licenses from " + acceptableLicenseResource);
      acceptableLicenses = LicenseSet.loadLicenses(acceptableLicenseResource);
    }
    excludeMatcher = new DependencyMatcher(getLog(), excludeDependencies);
    licenseMatcher = new LicenseMatcher(getLog(), acceptableLicenses);

    for (Dependency dependency : project.getDependencies()) {
      checkDependency(dependency);
    }
  }

  private void checkDependency(Dependency dependency) throws MojoExecutionException, MojoFailureException {
    if(!scopes.contains(dependency.getScope())) {
      getLog().debug(dependency.getArtifactId() + " is not scoped");
      return;
    }
    if (excludeMatcher.isMatch(dependency)) {
      getLog().debug(dependency.getArtifactId() + " is excluded");
      return;
    }
    MavenProject mavenProject = getMavenProject(dependency);
    if (!licenseMatcher.hasAcceptableLicense(mavenProject.getLicenses())) {
      throw new MojoExecutionException(dependency.getArtifactId() + " does not have acceptable license");
    }
  }

  private MavenProject getMavenProject(Dependency d) throws MojoFailureException {
    try {
      Artifact pomArtifact = repositorySystem.createProjectArtifact(d.getGroupId(), d.getArtifactId(), d.getVersion());
      ProjectBuildingResult build = mavenProjectBuilder.build(pomArtifact, session.getProjectBuildingRequest());
      return build.getProject();
    } catch (ProjectBuildingException e) {
      throw new MojoFailureException("Could not build effective pom for " + d.getArtifactId(), e);
    }
  }
}
