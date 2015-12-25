/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.sk89q.worldedit.function.operation;

import com.boydti.fawe.util.SetBlockQueue;
import com.boydti.fawe.util.TaskManager;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.WorldEditException;

/**
 * Operation helper methods.
 */
public final class Operations {
    
    private Operations() {}
    
    /**
     * Complete a given operation synchronously until it completes.
     *
     * @param op operation to execute
     * @throws WorldEditException WorldEdit exception
     */
    public static void complete(Operation operation) throws WorldEditException {
        while (operation != null) {
            operation = operation.resume(new RunContext());
        }
    }
    
    /**
     * Complete a given operation synchronously until it completes. Catch all
     * errors that is not {@link MaxChangedBlocksException} for legacy reasons.
     *
     * @param op operation to execute
     * @throws MaxChangedBlocksException thrown when too many blocks have been changed
     */
    public static void completeLegacy(Operation operation) throws MaxChangedBlocksException {
        try {
            while (operation != null) {
                operation = operation.resume(new RunContext());
            }
        } catch (final WorldEditException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Complete a given operation synchronously until it completes. Re-throw all
     * {@link com.sk89q.worldedit.WorldEditException} exceptions as
     * {@link java.lang.RuntimeException}s.
     *
     * @param op operation to execute
     */
    public static void completeBlindly(Operation operation) {
        try {
            while (operation != null) {
                operation = operation.resume(new RunContext());
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }
    
    public static void completeSmart(final Operation op, final Runnable whenDone, final boolean threadsafe) {
        if (!threadsafe) {
            completeBlindly(op);
            if (whenDone != null) {
                whenDone.run();
            }
            return;
        }
        SetBlockQueue.IMP.addTask(new Runnable() {
            @Override
            public void run() {
                TaskManager.IMP.async(new Runnable() {
                    @Override
                    public void run() {
                        Operation operation = op;
                        while (operation != null) {
                            try {
                                operation = operation.resume(new RunContext());
                            } catch (final Exception e) {
                                e.printStackTrace();
                            }
                        }
                        TaskManager.IMP.task(whenDone);
                    }
                });
            };
        });
    }
    
    public static Class<?> inject() {
        return Operations.class;
    }
}
