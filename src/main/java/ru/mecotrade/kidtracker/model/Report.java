/*
 * Copyright 2020 Sergey Shadchin (sergei.shadchin@gmail.com)
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
package ru.mecotrade.kidtracker.model;

import lombok.Builder;
import lombok.Data;

import java.util.Collection;
import java.util.Date;
import java.util.Map;

@Data
@Builder
public class Report {

    private Collection<Position> positions;

    private Collection<Snapshot> snapshots;

    private Collection<String> alarms;

    private Map<String, Date> last;
}
