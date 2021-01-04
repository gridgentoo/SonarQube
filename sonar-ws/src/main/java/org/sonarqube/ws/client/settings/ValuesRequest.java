/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
package org.sonarqube.ws.client.settings;

import java.util.List;
import javax.annotation.Generated;

/**
 * This is part of the internal API.
 * This is a POST request.
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/settings/values">Further information about this action online (including a response example)</a>
 * @since 6.3
 */
@Generated("sonar-ws-generator")
public class ValuesRequest {

  private String branch;
  private String component;
  private List<String> keys;
  private String pullRequest;

  /**
   * This is part of the internal API.
   * Example value: "feature/my_branch"
   */
  public ValuesRequest setBranch(String branch) {
    this.branch = branch;
    return this;
  }

  public String getBranch() {
    return branch;
  }

  /**
   * Example value: "my_project"
   */
  public ValuesRequest setComponent(String component) {
    this.component = component;
    return this;
  }

  public String getComponent() {
    return component;
  }

  /**
   * Example value: "sonar.test.inclusions,sonar.cpd.cross_project"
   */
  public ValuesRequest setKeys(List<String> keys) {
    this.keys = keys;
    return this;
  }

  public List<String> getKeys() {
    return keys;
  }

  /**
   * This is part of the internal API.
   * Example value: "5461"
   */
  public ValuesRequest setPullRequest(String pullRequest) {
    this.pullRequest = pullRequest;
    return this;
  }

  public String getPullRequest() {
    return pullRequest;
  }
}
