/*
 * This file is part of javaducks, licensed under the MIT License.
 *
 * Copyright (c) 2023-2024 Seiama
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.seiama.javaducks.controller;

import com.seiama.javaducks.configuration.properties.AppConfiguration;
import com.seiama.javaducks.service.JavadocService;
import com.vdurmont.semver4j.Semver;
import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.HandlerMapping;

import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import static org.springframework.http.ResponseEntity.notFound;
import static org.springframework.http.ResponseEntity.ok;
import static org.springframework.http.ResponseEntity.status;

@Controller
@NullMarked
public class JavadocController {
  // https://regex101.com/r/fyzJ7g/1
  private static final Pattern STATICS_PATTERN = Pattern.compile("^(?!.*search-index).*\\.(js|png|css|html)$");
  private static final CacheControl CACHE_CONTROL = CacheControl.maxAge(Duration.ofMinutes(10));
  private static final CacheControl STATICS_CACHE_CONTROL = CacheControl.maxAge(Duration.ofDays(7));
  private static final ContentDisposition CONTENT_DISPOSITION = ContentDisposition.inline().build();
  private static final Map<String, MediaType> MEDIATYPES = Map.of(
    ".css", MediaType.parseMediaType("text/css"),
    ".js", MediaType.parseMediaType("application/javascript"),
    ".zip", MediaType.parseMediaType("application/zip")
  );
  private final JavadocService service;
  private final AppConfiguration appConfiguration;

  @Autowired
  public JavadocController(final JavadocService service, final AppConfiguration appConfiguration) {
    this.service = service;
    this.appConfiguration = appConfiguration;
  }

  @GetMapping("/{project:[a-z]+}/{version:[0-9.]+-?(?:pre|SNAPSHOT)?(?:[0-9.]+)?}")
  @ResponseBody
  public ResponseEntity<?> redirectToPathWithTrailingSlash(
    final HttpServletRequest request,
    @PathVariable final String project,
    @PathVariable final String version
  ) {
    return status(HttpStatus.FOUND)
      .location(URI.create(request.getRequestURI() + "/"))
      .build();
  }

  @GetMapping("/{project:[a-z]+}/{version:[0-9.]+-?(?:pre|SNAPSHOT)?(?:[0-9.]+)?}/**")
  @ResponseBody
  public ResponseEntity<?> serveJavadocs(
    final HttpServletRequest request,
    @PathVariable final String project,
    @PathVariable final String version
  ) {
    final AppConfiguration.EndpointConfiguration endpointConfiguration = this.project(project);
    if (endpointConfiguration != null) {
      // idk try
      final AppConfiguration.EndpointConfiguration.VersionGroup group = this.findFromVersion(endpointConfiguration, version);
      if (group != null) {
        final AppConfiguration.EndpointConfiguration.VersionGroup.Version latestVersion = this.latestVersionFromGroup(group);
        if (latestVersion != null) {
          return status(HttpStatus.FOUND)
            .location(URI.create(request.getRequestURI().replace(version, latestVersion.name())))
            .build();
        }
      }
    }

    final String root = "/%s/%s".formatted(project, version);
    //noinspection resource - This warning can be ignored, we want to keep this FS open.
    final @Nullable FileSystem fs = this.service.contentsFor(new JavadocService.Key(project, version));
    if (fs != null) {
      String path = ((String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE)).substring(root.length());
      if (path.equals("/")) {
        path = "index.html";
      }
      final Path file = fs.getPath(path);
      if (Files.isRegularFile(file)) {
        return ok()
          .cacheControl(STATICS_PATTERN.matcher(path).find() ? STATICS_CACHE_CONTROL : CACHE_CONTROL)
          .headers(headers -> {
            headers.setContentDisposition(CONTENT_DISPOSITION);
            headers.set("X-JavaDucks", "Quack");
            final String name = file.getFileName().toString();
            for (final Map.Entry<String, MediaType> entry : MEDIATYPES.entrySet()) {
              if (name.endsWith(entry.getKey())) {
                headers.setContentType(entry.getValue());
                break;
              }
            }
          })
          .body(new FileSystemResource(file));
      }
    }
    return notFound()
      .cacheControl(CacheControl.noCache())
      .build();
  }

  // all ugly temporary code
  public AppConfiguration.@Nullable EndpointConfiguration project(final String name) {
    for (final AppConfiguration.EndpointConfiguration project : this.appConfiguration.endpoints()) {
      if (project.name().equalsIgnoreCase(name)) {
        return project;
      }
    }
    return null;
  }

  public AppConfiguration.EndpointConfiguration.VersionGroup.@Nullable Version latestVersionFromGroup(final AppConfiguration.EndpointConfiguration.VersionGroup group) {
    AppConfiguration.EndpointConfiguration.VersionGroup.Version latestVersion = null;
    String latestPatchVersion = null;
    Semver latestSemver = null;
    for (final AppConfiguration.EndpointConfiguration.VersionGroup.Version version : group.versions()) {
      Semver semver = new Semver(version.name());
      // Check if this version is greater than the current latest patch version
      if (latestPatchVersion == null || semver.isGreaterThan(latestSemver) || semver.getPatch() > latestSemver.getPatch()) {
        latestPatchVersion = version.name();
        latestSemver = semver;
        latestVersion = version;
      }
    }
    return latestVersion;
  }

  public AppConfiguration.EndpointConfiguration.@Nullable VersionGroup findFromVersion(final AppConfiguration.EndpointConfiguration project, final String version) {
    for (final Map.Entry<String, AppConfiguration.EndpointConfiguration.VersionGroup> group : project.versionsGroups().entrySet()) {
      final Semver groupSemver = new Semver(group.getValue().version(), Semver.SemverType.LOOSE);
      final Semver versionSemver = new Semver(version, Semver.SemverType.LOOSE);
      if (Objects.equals(groupSemver.getMinor(), versionSemver.getMinor())) {
        return group.getValue();
      }
    }
    return null;
  }
}
