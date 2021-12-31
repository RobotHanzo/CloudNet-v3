/*
 * Copyright 2019-2022 CloudNetService team & contributors
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

package de.dytanic.cloudnet.ext.cloudflare;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.language.I18n;
import de.dytanic.cloudnet.common.log.LogManager;
import de.dytanic.cloudnet.common.log.Logger;
import de.dytanic.cloudnet.driver.module.ModuleLifeCycle;
import de.dytanic.cloudnet.driver.module.ModuleTask;
import de.dytanic.cloudnet.driver.module.driver.DriverModule;
import de.dytanic.cloudnet.ext.cloudflare.CloudflareConfigurationEntry.AuthenticationMethod;
import de.dytanic.cloudnet.ext.cloudflare.cloudflare.CloudFlareAPI;
import de.dytanic.cloudnet.ext.cloudflare.dns.DNSType;
import de.dytanic.cloudnet.ext.cloudflare.dns.DefaultDNSRecord;
import de.dytanic.cloudnet.ext.cloudflare.listener.CloudflareStartAndStopListener;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;
import lombok.NonNull;

public final class CloudNetCloudflareModule extends DriverModule {

  private static final Logger LOGGER = LogManager.logger(CloudNetCloudflareModule.class);
  private static CloudNetCloudflareModule instance;
  private CloudFlareAPI cloudFlareAPI;
  private CloudflareConfiguration cloudflareConfiguration;

  public CloudNetCloudflareModule() {
    instance = this;
  }

  public static CloudNetCloudflareModule instance() {
    return CloudNetCloudflareModule.instance;
  }

  @ModuleTask(order = 127, event = ModuleLifeCycle.STARTED)
  public void loadConfiguration() {
    var configuration = this.readConfig();
    this.cloudflareConfiguration = configuration.get(
      "config",
      CloudflareConfiguration.class,
      new CloudflareConfiguration(
        new ArrayList<>(Collections.singletonList(
          new CloudflareConfigurationEntry(
            false,
            AuthenticationMethod.GLOBAL_KEY,
            this.initialHostAddress(),
            "user@example.com",
            "api_token_string",
            "zoneId",
            "example.com",
            new ArrayList<>(Collections.singletonList(
              new CloudflareGroupConfiguration("Proxy", "@", 1, 1)
            ))
          )
        ))
      ));

    this.updateConfiguration(this.cloudflareConfiguration);
  }

  @ModuleTask(order = 126, event = ModuleLifeCycle.STARTED)
  public void initCloudflareAPI() {
    this.cloudFlareAPI = new CloudFlareAPI();
  }

  @ModuleTask(order = 125, event = ModuleLifeCycle.STARTED)
  public void addedDefaultCloudflareDNSServices() {
    var cloudConfig = CloudNet.instance().config();

    for (var entry : this.cloudFlareConfiguration().entries()) {
      if (entry.enabled()) {
        boolean ipv6Address;
        try {
          ipv6Address = InetAddress.getByName(entry.hostAddress()) instanceof Inet6Address;
        } catch (UnknownHostException exception) {
          LOGGER.severe("Host address of entry " + entry + " is invalid!", exception);
          continue;
        }

        var recordDetail = this.cloudFlareAPI.createRecord(
          UUID.randomUUID(),
          entry,
          new DefaultDNSRecord(
            ipv6Address ? DNSType.AAAA : DNSType.A,
            cloudConfig.identity().uniqueId() + "." + entry.domainName(),
            entry.hostAddress(),
            JsonDocument.emptyDocument()));
        if (recordDetail != null) {
          LOGGER.info(I18n.trans("module-cloudflare-create-dns-record-for-service")
            .replace("%service%", cloudConfig.identity().uniqueId())
            .replace("%domain%", entry.domainName())
            .replace("%recordId%", recordDetail.id()));
        }
      }
    }
  }

  @ModuleTask(event = ModuleLifeCycle.RELOADING)
  public void handleReload() {
    this.loadConfiguration();
  }

  @ModuleTask(order = 124, event = ModuleLifeCycle.STARTED)
  public void registerListeners() {
    this.registerListener(new CloudflareStartAndStopListener(this.cloudFlareAPI));
  }

  @ModuleTask(order = 64, event = ModuleLifeCycle.STOPPED)
  public void removeRecordsOnDelete() {
    this.cloudFlareAPI.close();
  }

  private String initialHostAddress() {
    try {
      return InetAddress.getLocalHost().getHostAddress();
    } catch (Exception ex) {
      return "0.0.0.0";
    }
  }

  public CloudflareConfiguration cloudFlareConfiguration() {
    return this.cloudflareConfiguration;
  }

  public void updateConfiguration(@NonNull CloudflareConfiguration cloudflareConfiguration) {
    this.cloudflareConfiguration = cloudflareConfiguration;
    this.writeConfig(JsonDocument.newDocument("config", cloudflareConfiguration));
  }

  public CloudFlareAPI cloudFlareAPI() {
    return this.cloudFlareAPI;
  }
}
