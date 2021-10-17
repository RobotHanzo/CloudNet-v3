/*
 * Copyright 2019-2021 CloudNetService team & contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.dytanic.cloudnet.template.install.execute.defaults;

import de.dytanic.cloudnet.template.install.InstallInformation;
import de.dytanic.cloudnet.template.install.execute.InstallStepExecutor;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.jetbrains.annotations.NotNull;

public class UnzipStepExecutor implements InstallStepExecutor {

  @Override
  public @NotNull Set<Path> execute(
    @NotNull InstallInformation installInformation,
    @NotNull Path workingDirectory,
    @NotNull Set<Path> inputPaths
  ) throws IOException {
    Set<Path> resultPaths = new HashSet<>();

    for (Path path : inputPaths) {
      try (ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(path))) {
        ZipEntry entry;
        while ((entry = zipInputStream.getNextEntry()) != null) {
          Path targetPath = workingDirectory.resolve(entry.getName());

          if (!targetPath.normalize().startsWith(workingDirectory)) {
            throw new IllegalStateException("Zip entry path contains traversal element!");
          }

          resultPaths.add(targetPath);

          if (entry.isDirectory()) {
            Files.createDirectory(targetPath);
          } else {
            Files.copy(zipInputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
          }
        }
      }
    }

    return resultPaths;
  }
}