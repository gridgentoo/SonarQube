/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.pushapi.scheduler.polling;

import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.sonar.api.Startable;
import org.sonar.api.config.Configuration;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.pushevent.PushEventDto;
import org.sonar.server.pushapi.sonarlint.SonarLintClient;
import org.sonar.server.pushapi.sonarlint.SonarLintClientsRegistry;

@ServerSide
public class PushEventPollScheduler implements Startable {

  private static final Logger LOG = Loggers.get(PushEventPollScheduler.class);

  private static final String INITIAL_DELAY_IN_SECONDS = "sonar.pushevents.polling.initial.delay";
  private static final String PERIOD_IN_SECONDS = "sonar.pushevents.polling.period";
  private static final String PAGE_SIZE = "sonar.pushevents.polling.page.size";

  private final PushEventExecutorService executorService;
  private final SonarLintClientsRegistry clientsRegistry;
  private final DbClient dbClient;
  private final System2 system2;
  private final Configuration config;
  private Long lastPullTimestamp = null;
  private String lastSeenUuid = null;

  public PushEventPollScheduler(PushEventExecutorService executorService, SonarLintClientsRegistry clientsRegistry,
    DbClient dbClient, System2 system2, Configuration config) {
    this.executorService = executorService;
    this.clientsRegistry = clientsRegistry;
    this.dbClient = dbClient;
    this.system2 = system2;
    this.config = config;
  }

  @Override
  public void start() {
    this.executorService.scheduleAtFixedRate(this::tryBroadcastEvents, getInitialDelay(), getPeriod(), TimeUnit.SECONDS);
  }

  private void tryBroadcastEvents() {
    try {
      doBroadcastEvents();
    } catch (Exception e) {
      LOG.warn("Failed to poll for push events", e);
    }
  }

  private void doBroadcastEvents() {
    var clients = clientsRegistry.getClients();
    if (clients.isEmpty()) {
      lastPullTimestamp = null;
      lastSeenUuid = null;
      return;
    }

    if (lastPullTimestamp == null) {
      lastPullTimestamp = system2.now();
    }

    var projectKeys = getClientsProjectKeys(clients);

    try (DbSession dbSession = dbClient.openSession(false)) {
      var projectUuids = getProjectUuids(projectKeys, dbSession);
      Deque<PushEventDto> events = getPushEvents(dbSession, projectUuids);

      LOG.debug("Received {} push events, attempting to broadcast to {} registered clients.", events.size(),
        clients.size());

      events.forEach(clientsRegistry::broadcastMessage);

      if (!events.isEmpty()) {
        var last = events.getLast();
        lastPullTimestamp = last.getCreatedAt();
        lastSeenUuid = last.getUuid();
      }
    }
  }

  private static Set<String> getClientsProjectKeys(List<SonarLintClient> clients) {
    return clients.stream()
      .map(SonarLintClient::getClientProjectKeys)
      .flatMap(Collection::stream)
      .collect(Collectors.toSet());
  }

  private Deque<PushEventDto> getPushEvents(DbSession dbSession, Set<String> projectUuids) {
    return dbClient.pushEventDao().selectChunkByProjectUuids(dbSession, projectUuids, lastPullTimestamp, lastSeenUuid, getPageSize());
  }

  @NotNull
  private Set<String> getProjectUuids(Set<String> projectKeys, DbSession dbSession) {
    return dbClient.projectDao().selectProjectsByKeys(dbSession, projectKeys)
      .stream().map(ProjectDto::getUuid)
      .collect(Collectors.toSet());
  }

  public long getInitialDelay() {
    // two minutes default initial delay
    return config.getLong(INITIAL_DELAY_IN_SECONDS).orElse(2 * 60L);
  }

  public long getPeriod() {
    // execute every 40 seconds
    return config.getLong(PERIOD_IN_SECONDS).orElse(40L);
  }

  public long getPageSize() {
    // 20 events per 40 seconds
    return config.getLong(PAGE_SIZE).orElse(20L);
  }

  @Override
  public void stop() {
    // nothing to do
  }

}
