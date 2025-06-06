/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.amoro.server.scheduler.inline;

import org.apache.amoro.server.persistence.PersistentBase;
import org.apache.amoro.server.persistence.mapper.TableBlockerMapper;
import org.apache.amoro.server.scheduler.PeriodicTableScheduler;
import org.apache.amoro.server.table.DefaultTableRuntime;
import org.apache.amoro.server.table.TableService;

public class BlockerExpiringExecutor extends PeriodicTableScheduler {

  private final Persistency persistency = new Persistency();

  private static final long INTERVAL = 60 * 60 * 1000L; // 1 hour

  public BlockerExpiringExecutor(TableService tableService) {
    super(tableService, 1);
  }

  @Override
  protected long getNextExecutingTime(DefaultTableRuntime tableRuntime) {
    return INTERVAL;
  }

  @Override
  protected boolean enabled(DefaultTableRuntime tableRuntime) {
    return true;
  }

  @Override
  protected void execute(DefaultTableRuntime tableRuntime) {
    try {
      persistency.doExpiring(tableRuntime);
    } catch (Throwable t) {
      logger.error("table {} expire blocker failed.", tableRuntime.getTableIdentifier(), t);
    }
  }

  private static class Persistency extends PersistentBase {

    public void doExpiring(DefaultTableRuntime tableRuntime) {
      String catalog = tableRuntime.getTableIdentifier().getCatalog();
      String database = tableRuntime.getTableIdentifier().getDatabase();
      String table = tableRuntime.getTableIdentifier().getTableName();
      doAs(
          TableBlockerMapper.class,
          mapper ->
              mapper.deleteExpiredBlockers(catalog, database, table, System.currentTimeMillis()));
    }
  }
}
