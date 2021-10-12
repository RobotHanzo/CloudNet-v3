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

package de.dytanic.cloudnet.service.defaults.factory;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.driver.service.ServiceConfiguration;
import de.dytanic.cloudnet.service.ICloudService;
import de.dytanic.cloudnet.service.ICloudServiceFactory;
import de.dytanic.cloudnet.service.ICloudServiceManager;
import de.dytanic.cloudnet.util.PortValidator;
import java.util.Collection;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractServiceFactory implements ICloudServiceFactory {

  protected void validateConfiguration(
    @NotNull ICloudServiceManager manager,
    @NotNull ServiceConfiguration configuration
  ) {
    // set the node unique id
    configuration.getServiceId().setNodeUniqueId(CloudNet.getInstance().getNodeUniqueId());

    // find a free port for the service
    int port = configuration.getPort();
    while (this.isPortInUse(manager, port)) {
      port++;
    }
    // set the port
    configuration.setPort(port);

    // check if the service id
    int serviceId = configuration.getServiceId().getTaskServiceId();
    // check if the service id is invalid
    if (serviceId <= 0) {
      serviceId = 1;
    }
    // check if it is already taken
    Collection<Integer> takenIds = manager.getCloudServices(configuration.getServiceId().getTaskName())
      .stream()
      .map(service -> service.getServiceId().getTaskServiceId())
      .collect(Collectors.toSet());
    while (takenIds.contains(serviceId)) {
      serviceId++;
    }
    // update the service id
    configuration.getServiceId().setTaskServiceId(serviceId);
  }

  protected boolean isPortInUse(@NotNull ICloudServiceManager manager, int port) {
    // check if any local service has the port
    for (ICloudService cloudService : manager.getLocalCloudServices()) {
      if (cloudService.getServiceConfiguration().getPort() == port) {
        return true;
      }
    }
    // validate that the port is free
    return !PortValidator.checkPort(port);
  }
}