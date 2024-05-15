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
package com.seiama.javaducks.api.v1.response;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.seiama.javaducks.api.model.Namespace;
import com.seiama.javaducks.api.v1.error.Error;
import com.seiama.javaducks.api.v1.error.ToError;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.jspecify.annotations.Nullable;

@Schema
public record NamespacesResponse(
  @Schema(name = "ok")
  boolean ok,
  @Schema(name = "namespaces")
  @Nullable List<Namespace> namespaces,
  @JsonUnwrapped
  @Nullable Error error
) {
  public static NamespacesResponse success(final List<Namespace> namespaces) {
    return new NamespacesResponse(true, namespaces, null);
  }

  public static NamespacesResponse error(final ToError error) {
    return new NamespacesResponse(false, null, error.toError());
  }
}
