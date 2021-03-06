// Copyright (c) 2017, Baidu.com, Inc. All Rights Reserved

// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package com.baidu.palo.load;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.baidu.palo.catalog.Catalog;
import com.baidu.palo.common.FeMetaVersion;
import com.baidu.palo.common.io.Text;
import com.baidu.palo.common.io.Writable;
import com.baidu.palo.load.AsyncDeleteJob.DeleteState;
import com.baidu.palo.persist.ReplicaPersistInfo;

import com.google.common.collect.Lists;

public class DeleteInfo implements Writable {

    private long dbId;
    private long tableId;
    private String tableName;
    private long partitionId;
    private String partitionName;
    private long partitionVersion;
    private long partitionVersionHash;
    private List<ReplicaPersistInfo> replicaInfos;

    private List<String> deleteConditions;
    private long createTimeMs;

    private AsyncDeleteJob asyncDeleteJob;

    public DeleteInfo() {
        this.replicaInfos = new ArrayList<ReplicaPersistInfo>();
        this.deleteConditions = Lists.newArrayList();
        this.asyncDeleteJob = null;
    }

    public DeleteInfo(long dbId, long tableId, String tableName, long partitionId, String partitionName,
                      long partitionVersion, long partitionVersionHash, List<String> deleteConditions) {
        this.dbId = dbId;
        this.tableId = tableId;
        this.tableName = tableName;
        this.partitionId = partitionId;
        this.partitionName = partitionName;
        this.partitionVersion = partitionVersion;
        this.partitionVersionHash = partitionVersionHash;
        this.replicaInfos = new ArrayList<ReplicaPersistInfo>();
        this.deleteConditions = deleteConditions;

        this.createTimeMs = System.currentTimeMillis();

        this.asyncDeleteJob = null;
    }

    public long getJobId() {
        return this.asyncDeleteJob == null ? -1 : this.asyncDeleteJob.getJobId();
    }

    public long getDbId() {
        return dbId;
    }

    public long getTableId() {
        return tableId;
    }

    public String getTableName() {
        return tableName;
    }

    public long getPartitionId() {
        return partitionId;
    }

    public String getPartitionName() {
        return partitionName;
    }

    public long getPartitionVersion() {
        return partitionVersion;
    }

    public long getPartitionVersionHash() {
        return partitionVersionHash;
    }

    public List<ReplicaPersistInfo> getReplicaPersistInfos() {
        return this.replicaInfos;
    }

    public void addReplicaPersistInfo(ReplicaPersistInfo info) {
        this.replicaInfos.add(info);
    }

    public void setDeleteConditions(List<String> deleteConditions) {
        this.deleteConditions = deleteConditions;
    }

    public List<String> getDeleteConditions() {
        return deleteConditions;
    }

    public long getCreateTimeMs() {
        return createTimeMs;
    }

    public AsyncDeleteJob getAsyncDeleteJob() {
        return asyncDeleteJob;
    }

    public void setAsyncDeleteJob(AsyncDeleteJob asyncDeleteJob) {
        this.asyncDeleteJob = asyncDeleteJob;
    }

    public DeleteState getState() {
        return asyncDeleteJob == null ? DeleteState.FINISHED : asyncDeleteJob.getState();
    }

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeLong(dbId);
        out.writeLong(tableId);
        out.writeLong(partitionId);
        out.writeLong(partitionVersion);
        out.writeLong(partitionVersionHash);
        out.writeInt(replicaInfos.size());
        for (ReplicaPersistInfo info : replicaInfos) {
            info.write(out);
        }

        Text.writeString(out, tableName);
        Text.writeString(out, partitionName);

        out.writeInt(deleteConditions.size());
        for (String deleteCond : deleteConditions) {
            Text.writeString(out, deleteCond);
        }

        out.writeLong(createTimeMs);

        if (asyncDeleteJob == null) {
            out.writeBoolean(false);
        } else {
            out.writeBoolean(true);
            asyncDeleteJob.write(out);
        }
    }

    @Override
    public void readFields(DataInput in) throws IOException {
        dbId = in.readLong();
        tableId = in.readLong();
        partitionId = in.readLong();
        partitionVersion = in.readLong();
        partitionVersionHash = in.readLong();
        int size = in.readInt();
        for (int i = 0; i < size; i++) {
            ReplicaPersistInfo info = ReplicaPersistInfo.read(in);
            replicaInfos.add(info);
        }

        if (Catalog.getCurrentCatalogJournalVersion() >= FeMetaVersion.VERSION_11) {
            tableName = Text.readString(in);
            partitionName = Text.readString(in);

            size = in.readInt();
            for (int i = 0; i < size; i++) {
                String deleteCond = Text.readString(in);
                deleteConditions.add(deleteCond);
            }

            createTimeMs = in.readLong();
        }

        if (Catalog.getCurrentCatalogJournalVersion() >= FeMetaVersion.VERSION_19) {
            if (in.readBoolean()) {
                asyncDeleteJob = AsyncDeleteJob.read(in);
            }
        }
    }
}