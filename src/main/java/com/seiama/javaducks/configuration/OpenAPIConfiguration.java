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
package com.seiama.javaducks.configuration;

import com.seiama.javaducks.configuration.properties.AppConfiguration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.servers.Server;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class OpenAPIConfiguration {
  @Bean
  OpenAPI openAPI(final AppConfiguration configuration) throws MalformedURLException {
    final OpenAPI api = new OpenAPI();
    api.info(
      new Info()
        .title(configuration.apiTitle())
        .version(configuration.apiVersion())
    );
    final URI apiBaseUrl = configuration.apiBaseUrl();
    if (apiBaseUrl != null) {
      api.servers(List.of(new Server().url(apiBaseUrl.toURL().toExternalForm())));
    }
    return api;
  }

  @Bean
  @SuppressWarnings("rawtypes") // nothing we can do, the API exposes it raw
  OpenApiCustomizer sortSchemasAlphabetically() {
    return openApi -> {
      final Map<String, Schema> schemas = openApi.getComponents().getSchemas();
      openApi.getComponents().setSchemas(new TreeMap<>(schemas));
    };
  }
}